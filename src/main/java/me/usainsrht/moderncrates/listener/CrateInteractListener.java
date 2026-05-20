package me.usainsrht.moderncrates.listener;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateLocation;
import me.usainsrht.moderncrates.gui.PreviewGui;
import me.usainsrht.moderncrates.util.SoundUtil;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.Iterator;

/**
 * Handles physical crate interaction, crate item usage, crate placement, and crate block breaking.
 */
public class CrateInteractListener implements Listener {

    private final ModernCratesPlugin plugin;

    public CrateInteractListener(ModernCratesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();

        // Check for crate item in hand (right-click in air or on block)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();

            // Block crate-item usage if it is actually a placement item
            if (plugin.getKeyManager().getCrateIdFromPlacerItem(mainHand) != null) {
                return;
            }

            String crateIdFromItem = plugin.getKeyManager().getCrateIdFromItem(mainHand);
            if (crateIdFromItem != null) {
                event.setCancelled(true);
                Crate crate = plugin.getCrateRegistry().get(crateIdFromItem);
                if (crate == null) return;

                boolean opened = plugin.tryOpenCrate(player, crate);
                if (!opened) return;

                if (mainHand.getAmount() > 1) {
                    mainHand.setAmount(mainHand.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                return;
            }
        }

        // Left-click on a physical crate opens the preview GUI
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            Crate crate = getCrateAtBlock(block);
            if (crate == null) return;

            event.setCancelled(true);
            new PreviewGui(player, crate).open();
            return;
        }

        // Check for physical crate block interaction
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            Crate crate = getCrateAtBlock(block);
            if (crate == null) return;

            event.setCancelled(true);

            // Bounce back
            if (crate.isBounceBack() && !canOpen(player, crate)) {
                bounceBack(player, block);
                String msg = plugin.getPluginConfig().getPrefix()
                        + plugin.getPluginConfig().getMessage("no_key")
                                .replace("<crate>", crate.getName());
                player.sendMessage(TextUtil.parse(msg));
                SoundUtil.play(player, plugin.getPluginConfig().getSound("no_key"));
                return;
            }

            // Shift-right-click to preview
            if (player.isSneaking()) {
                new PreviewGui(player, crate).open();
                return;
            }

            // Pass the block location so BlockDismantle animates the correct block
            Location blockLoc = block.getLocation();
            plugin.tryOpenCrate(player, crate, blockLoc);
        }
    }

    /**
     * Handles placement of a crate placement item.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String crateId = plugin.getKeyManager().getCrateIdFromPlacerItem(item);
        if (crateId == null) return;

        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) return;

        Block block = event.getBlock();
        CrateLocation loc = new CrateLocation(
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ()
        );
        crate.addCrateLocation(loc);

        // Auto-save and refresh holograms
        try {
            plugin.getCrateConfigParser().save(crate, new File(plugin.getDataFolder(), "crates"));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to auto-save crate after placement: " + e.getMessage());
        }
        plugin.getHologramManager().removeHologram(crate.getId());
        plugin.getHologramManager().createHologram(crate);

        event.getPlayer().sendMessage(TextUtil.parse(
                "<green>Location added to crate <white>" + crate.getName()
                + "<green>! (" + crate.getCrateLocations().size() + " total)"
        ));
    }

    /**
     * When a player sneaks and breaks a crate block, removes that location from the crate.
     * Requires moderncrates.admin permission.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (!player.hasPermission("moderncrates.admin")) return;

        Block block = event.getBlock();
        Crate crate = getCrateAtBlock(block);
        if (crate == null) return;

        // Remove this specific location from the crate
        event.setCancelled(true); // Prevent the block from actually being broken
        boolean removed = removeCrateLocation(crate, block);
        if (!removed) return;

        try {
            plugin.getCrateConfigParser().save(crate, new File(plugin.getDataFolder(), "crates"));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to auto-save crate after location removal: " + e.getMessage());
        }
        plugin.getHologramManager().removeHologram(crate.getId());
        if (!crate.getCrateLocations().isEmpty()) {
            plugin.getHologramManager().createHologram(crate);
        }

        player.sendMessage(TextUtil.parse(
                "<yellow>Removed crate location for <white>" + crate.getName()
                + "<yellow>. (" + crate.getCrateLocations().size() + " remaining)"
        ));
    }

    private boolean removeCrateLocation(Crate crate, Block block) {
        Iterator<CrateLocation> it = crate.getCrateLocations().iterator();
        while (it.hasNext()) {
            CrateLocation loc = it.next();
            if (loc.getWorldName().equals(block.getWorld().getName())
                    && (int) loc.getX() == block.getX()
                    && (int) loc.getY() == block.getY()
                    && (int) loc.getZ() == block.getZ()) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    Crate getCrateAtBlock(Block block) {
        for (Crate crate : plugin.getCrateRegistry().values()) {
            for (CrateLocation loc : crate.getCrateLocations()) {
                if (loc.getWorldName().equals(block.getWorld().getName())
                        && (int) loc.getX() == block.getX()
                        && (int) loc.getY() == block.getY()
                        && (int) loc.getZ() == block.getZ()) {
                    return crate;
                }
            }
        }
        return null;
    }

    private boolean canOpen(Player player, Crate crate) {
        if (!crate.requiresKey()) return true;
        if (plugin.getKeyManager().hasKey(player, crate)) return true;
        return plugin.getVirtualKeyManager().getKeys(player, crate.getId()) > 0;
    }

    private void bounceBack(Player player, Block block) {
        Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
        Vector direction = player.getLocation().toVector().subtract(blockCenter.toVector()).normalize();
        direction.setY(0.3);
        player.setVelocity(direction.multiply(1.5));
    }
}
