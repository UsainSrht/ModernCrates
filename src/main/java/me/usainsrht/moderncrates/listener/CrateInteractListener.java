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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Handles physical crate interaction and crate item usage.
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
            String crateIdFromItem = plugin.getKeyManager().getCrateIdFromItem(mainHand);
            if (crateIdFromItem != null) {
                event.setCancelled(true);
                Crate crate = plugin.getCrateRegistry().get(crateIdFromItem);
                if (crate == null) return;

                boolean opened = plugin.tryOpenCrate(player, crate);
                if (!opened) return;

                // Consume one crate item only when the crate actually opens.
                if (mainHand.getAmount() > 1) {
                    mainHand.setAmount(mainHand.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                return;
            }
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
                var previewGui = new PreviewGui(player, crate);
                previewGui.open();
                return;
            }

            plugin.openCrate(player, crate);
        }
    }

    private Crate getCrateAtBlock(Block block) {
        for (Crate crate : plugin.getCrateRegistry().values()) {
            CrateLocation loc = crate.getCrateLocation();
            if (loc == null) continue;
            if (loc.getWorldName().equals(block.getWorld().getName())
                    && (int) loc.getX() == block.getX()
                    && (int) loc.getY() == block.getY()
                    && (int) loc.getZ() == block.getZ()) {
                return crate;
            }
        }
        return null;
    }

    private boolean canOpen(Player player, Crate crate) {
        if (!crate.requiresKey()) return true;
        // Check physical keys
        if (plugin.getKeyManager().hasKey(player, crate)) return true;
        // Check virtual keys
        return plugin.getVirtualKeyManager().getKeys(player, crate.getId()) > 0;
    }

    private void bounceBack(Player player, Block block) {
        Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
        Vector direction = player.getLocation().toVector().subtract(blockCenter.toVector()).normalize();
        direction.setY(0.3);
        player.setVelocity(direction.multiply(0.5));
    }
}
