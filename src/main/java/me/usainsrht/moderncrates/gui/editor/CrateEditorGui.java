package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateKeyConfig;
import me.usainsrht.moderncrates.api.crate.CrateLocation;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class CrateEditorGui extends EditorGuiBase {

    private final Crate crate;

    public CrateEditorGui(Player player, ModernCratesPlugin plugin, Crate crate) {
        super(player, plugin);
        this.crate = crate;
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(this, 54, TextUtil.parse("<dark_red><bold>Editing: " + crate.getName()));
        fillBlack();

        // Row 1: Name, Animation
        inventory.setItem(10, ItemBuilder.create("NAME_TAG", "<yellow><bold>Name: <white>" + crate.getName(),
                List.of("<gray>Click to rename")));
        inventory.setItem(12, createAnimationSelector());

        // Row 1: Key toggle, Bounce back
        inventory.setItem(14, ItemBuilder.create("TRIPWIRE_HOOK",
                "<yellow><bold>Key Required: <white>" + (crate.requiresKey() ? "Yes" : "No"),
                List.of("<gray>Click to toggle")));
        inventory.setItem(16, ItemBuilder.create(crate.isBounceBack() ? "SLIME_BALL" : "SNOWBALL",
                "<yellow><bold>Bounce Back: <white>" + (crate.isBounceBack() ? "On" : "Off"),
                List.of("<gray>Click to toggle")));

        // Row 2: Sub-editors
        inventory.setItem(19, ItemBuilder.create("TRIPWIRE_HOOK", "<light_purple><bold>Key Settings",
                List.of("<gray>Click to edit key config")));
        inventory.setItem(21, ItemBuilder.create("CHEST", "<light_purple><bold>Item Display",
                List.of("<gray>Click to edit crate item appearance")));
        inventory.setItem(23, ItemBuilder.create("ARMOR_STAND", "<light_purple><bold>Hologram",
                List.of("<gray>Click to edit hologram config")));
        inventory.setItem(25, ItemBuilder.create("ENDER_EYE", "<light_purple><bold>Preview",
                List.of("<gray>Click to edit preview settings")));

        // Row 3: More sub-editors
        inventory.setItem(28, ItemBuilder.create("GOAT_HORN", "<light_purple><bold>Announce",
                List.of("<gray>Click to edit announce config")));
        inventory.setItem(30, ItemBuilder.create("DIAMOND", "<gold><bold>Rewards (" + crate.getRewards().size() + ")",
                List.of("<gray>Click to manage rewards")));

        // Location
        List<String> locLore = new ArrayList<>();
        locLore.add("<gray>Left-click: <white>get placement item (place to add location)");
        locLore.add("<gray>Right-click: <white>add your target block as location");
        locLore.add("<gray>Shift-click: <white>clear all locations");
        locLore.add("");
        List<CrateLocation> locs = crate.getCrateLocations();
        if (locs.isEmpty()) {
            locLore.add("<red>No locations (virtual)");
        } else {
            locLore.add("<yellow>" + locs.size() + " location(s):");
            for (CrateLocation l : locs) {
                locLore.add("<white>  " + l.getWorldName() + " " + (int) l.getX() + " " + (int) l.getY() + " " + (int) l.getZ());
            }
        }
        inventory.setItem(32, ItemBuilder.create("COMPASS",
                "<yellow><bold>Locations <white>(" + locs.size() + ")", locLore));

        // Bottom: Save, Back
        inventory.setItem(45, ItemBuilder.create("ARROW", "<red><bold>Back", List.of("<gray>Return to crate list")));
        inventory.setItem(49, ItemBuilder.create("LIME_WOOL", "<green><bold>Save to File",
                List.of("<gray>Saves this crate to YAML")));

        player.openInventory(inventory);
    }

    private ItemStack createAnimationSelector() {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Click to cycle animations");
        lore.add("");
        for (String animId : plugin.getAnimationRegistry().keySet()) {
            if (animId.equals(crate.getAnimationId())) {
                lore.add("<green>> " + animId);
            } else {
                lore.add("<gray>  " + animId);
            }
        }
        return ItemBuilder.create("CLOCK", "<yellow><bold>Animation: <white>" + crate.getAnimationId(), lore);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean shiftClick = event.isShiftClick();

        switch (slot) {
            case 10 -> requestSignInput("Enter name", input -> {
                crate.setName(input);
                open();
            });
            case 12 -> {
                List<String> animIds = new ArrayList<>(plugin.getAnimationRegistry().keySet());
                if (!animIds.isEmpty()) {
                    int idx = animIds.indexOf(crate.getAnimationId());
                    crate.setAnimationId(animIds.get((idx + 1) % animIds.size()));
                    open();
                }
            }
            case 14 -> {
                CrateKeyConfig kc = crate.getKeyConfig();
                if (kc == null) {
                    kc = new CrateKeyConfig();
                    kc.setRequired(true);
                    kc.setMaterial("TRIPWIRE_HOOK");
                    kc.setCount(1);
                    crate.setKeyConfig(kc);
                } else {
                    kc.setRequired(!kc.isRequired());
                }
                open();
            }
            case 16 -> {
                crate.setBounceBack(!crate.isBounceBack());
                open();
            }
            case 19 -> new KeyEditorGui(player, plugin, crate).open();
            case 21 -> new ItemEditorGui(player, plugin, crate).open();
            case 23 -> new HologramEditorGui(player, plugin, crate).open();
            case 25 -> new PreviewEditorGui(player, plugin, crate).open();
            case 28 -> new AnnounceEditorGui(player, plugin, crate).open();
            case 30 -> new RewardsListGui(player, plugin, crate).open();
            case 32 -> {
                if (shiftClick) {
                    crate.clearCrateLocations();
                    plugin.getHologramManager().removeHologram(crate.getId());
                    open();
                } else if (event.isRightClick()) {
                    // Add player's target block as a new crate location
                    org.bukkit.block.Block target = player.getTargetBlockExact(10);
                    if (target != null) {
                    crate.addCrateLocation(new CrateLocation(
                                target.getWorld().getName(),
                                target.getX(), target.getY(), target.getZ()
                        ));
                        plugin.getHologramManager().removeHologram(crate.getId());
                        plugin.getHologramManager().createHologram(crate);
                        player.sendMessage(TextUtil.parse("<green>Target block added as crate location."));
                    } else {
                        player.sendMessage(TextUtil.parse("<red>No block in sight (max 10 blocks)."));
                    }
                    open();
                } else {
                    // Left-click: give the crate placement item and close
                    player.closeInventory();
                    ItemStack placerItem = plugin.getKeyManager().createCratePlacerItem(crate);
                    if (placerItem != null) {
                        player.getInventory().addItem(placerItem);
                        player.sendMessage(TextUtil.parse(
                                "<green>Place the item to register that block as a crate location."
                        ));
                    }
                }
            }
            case 45 -> new CrateListGui(player, plugin).open();
            case 49 -> {
                saveCrate(crate);
                player.sendMessage(TextUtil.parse("<green>Crate saved to file!"));
            }
        }
    }

    public Crate getCrate() { return crate; }
}
