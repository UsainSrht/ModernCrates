package me.usainsrht.moderncrates.gui;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.GuiItemConfig;
import me.usainsrht.moderncrates.api.animation.PointerConfig;
import me.usainsrht.moderncrates.api.crate.*;
import me.usainsrht.moderncrates.api.crate.*;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.reward.RewardDisplay;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Admin editor GUI for CRUD operations on crates, keys, and animations.
 * Provides comprehensive in-game editing for every config property.
 */
public class AdminEditorGui implements ModernCratesGui {

    private final Player player;
    private final ModernCratesPlugin plugin;

    private EditorMode mode = EditorMode.MAIN_MENU;
    private Crate editingCrate;
    private String editingRewardId;
    private Animation editingAnimation;
    private Inventory inventory;

    public enum EditorMode {
        MAIN_MENU,
        CRATE_LIST,
        CRATE_EDIT,
        KEY_EDIT,
        ITEM_EDIT,
        HOLOGRAM_EDIT,
        PREVIEW_EDIT,
        ANNOUNCE_EDIT,
        REWARDS_LIST,
        REWARD_EDIT,
        ANIMATION_LIST,
        ANIMATION_EDIT,
        ANIMATION_SOUNDS_EDIT
    }

    public AdminEditorGui(Player player, ModernCratesPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    public void open() {
        mode = EditorMode.MAIN_MENU;
        renderMainMenu();
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;
        boolean rightClick = event.isRightClick();
        boolean shiftClick = event.isShiftClick();

        switch (mode) {
            case MAIN_MENU -> handleMainMenu(slot);
            case CRATE_LIST -> handleCrateList(slot, rightClick);
            case CRATE_EDIT -> handleCrateEdit(slot, shiftClick);
            case KEY_EDIT -> handleKeyEdit(slot, rightClick, shiftClick);
            case ITEM_EDIT -> handleItemEdit(slot);
            case HOLOGRAM_EDIT -> handleHologramEdit(slot);
            case PREVIEW_EDIT -> handlePreviewEdit(slot);
            case ANNOUNCE_EDIT -> handleAnnounceEdit(slot);
            case REWARDS_LIST -> handleRewardsList(slot, rightClick);
            case REWARD_EDIT -> handleRewardEdit(slot, rightClick, shiftClick);
            case ANIMATION_LIST -> handleAnimationList(slot, rightClick);
            case ANIMATION_EDIT -> handleAnimationEdit(slot, rightClick, shiftClick);
            case ANIMATION_SOUNDS_EDIT -> handleAnimationSoundsEdit(slot, rightClick);
        }
    }

    // ========================
    // Main Menu
    // ========================

    private void renderMainMenu() {
        inventory = Bukkit.createInventory(this, 27, TextUtil.parse("&4&lModernCrates Editor"));
        fillBlack();

        inventory.setItem(11, ItemBuilder.create("CHEST", "&6&lCrates",
                List.of("&7Manage all crates", "&7" + plugin.getCrateRegistry().size() + " loaded")));
        inventory.setItem(15, ItemBuilder.create("CLOCK", "&b&lAnimations",
                List.of("&7Manage all animations", "&7" + plugin.getAnimationRegistry().size() + " loaded")));
    }

    private void handleMainMenu(int slot) {
        if (slot == 11) {
            openCrateList();
        } else if (slot == 15) {
            openAnimationList();
        }
    }

    // ========================
    // Crate List
    // ========================

    private void openCrateList() {
        mode = EditorMode.CRATE_LIST;
        var crates = new ArrayList<>(plugin.getCrateRegistry().values());
        int rows = Math.max(2, Math.min(6, (int) Math.ceil((crates.size() + 2) / 9.0) + 1));

        inventory = Bukkit.createInventory(this, rows * 9, TextUtil.parse("&4&lCrates"));
        fillBlack();

        // Back button
        inventory.setItem(inventory.getSize() - 9, ItemBuilder.create("ARROW", "&c&lBack", List.of("&7Return to main menu")));

        // Create new crate
        inventory.setItem(inventory.getSize() - 1, ItemBuilder.create("EMERALD", "&a&lCreate New Crate",
                List.of("&7Click to create a new crate")));

        for (int i = 0; i < crates.size() && i < inventory.getSize() - 9; i++) {
            Crate crate = crates.get(i);
            String mat = crate.getItemConfig() != null ? crate.getItemConfig().getMaterial() : "CHEST";
            ItemStack item = ItemBuilder.create(mat, "&6&l" + crate.getName(), List.of(
                    "&7ID: &f" + crate.getId(),
                    "&7Animation: &f" + crate.getAnimationId(),
                    "&7Rewards: &f" + crate.getRewards().size(),
                    "", "&eLeft-click to edit", "&cRight-click to delete"
            ));
            setEditorTag(item, "editor_crate", crate.getId());
            inventory.setItem(i, item);
        }
        player.openInventory(inventory);
    }

    private void handleCrateList(int slot, boolean rightClick) {
        if (slot == inventory.getSize() - 9) { open(); return; }
        if (slot == inventory.getSize() - 1) {
            String newId = "new_crate_" + System.currentTimeMillis() % 10000;
            Crate crate = new Crate(newId);
            crate.setName("New Crate");
            crate.setAnimationId("csgo");
            crate.setRewards(new LinkedHashMap<>());
            CrateItemConfig itemCfg = new CrateItemConfig();
            itemCfg.setMaterial("CHEST");
            itemCfg.setName("&6&l" + crate.getName());
            crate.setItemConfig(itemCfg);
            plugin.getCrateRegistry().put(newId, crate);
            saveCrate(crate);
            openCrateEditor(crate);
            return;
        }

        String crateId = getEditorTag(slot, "editor_crate");
        if (crateId == null) return;
        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) return;

        if (rightClick) {
            plugin.getCrateRegistry().remove(crateId);
            File f = new File(plugin.getDataFolder(), "crates/" + crateId + ".yml");
            if (f.exists()) f.delete();
            openCrateList();
        } else {
            openCrateEditor(crate);
        }
    }

    // ========================
    // Crate Editor
    // ========================

    private void openCrateEditor(Crate crate) {
        mode = EditorMode.CRATE_EDIT;
        editingCrate = crate;

        inventory = Bukkit.createInventory(this, 54, TextUtil.parse("&4&lEditing: " + crate.getName()));
        fillBlack();

        // Row 1: Name, Animation
        inventory.setItem(10, ItemBuilder.create("NAME_TAG", "&e&lName: &f" + crate.getName(),
                List.of("&7Click to rename")));
        inventory.setItem(12, createAnimationSelector(crate));

        // Row 1: Key toggle, Bounce back
        inventory.setItem(14, ItemBuilder.create("TRIPWIRE_HOOK",
                "&e&lKey Required: &f" + (crate.requiresKey() ? "Yes" : "No"),
                List.of("&7Click to toggle")));
        inventory.setItem(16, ItemBuilder.create(crate.isBounceBack() ? "SLIME_BALL" : "SNOWBALL",
                "&e&lBounce Back: &f" + (crate.isBounceBack() ? "On" : "Off"),
                List.of("&7Click to toggle")));

        // Row 2: Sub-editors
        inventory.setItem(19, ItemBuilder.create("TRIPWIRE_HOOK", "&d&lKey Settings",
                List.of("&7Click to edit key config")));
        inventory.setItem(21, ItemBuilder.create("CHEST", "&d&lItem Display",
                List.of("&7Click to edit crate item appearance")));
        inventory.setItem(23, ItemBuilder.create("ARMOR_STAND", "&d&lHologram",
                List.of("&7Click to edit hologram config")));
        inventory.setItem(25, ItemBuilder.create("ENDER_EYE", "&d&lPreview",
                List.of("&7Click to edit preview settings")));

        // Row 3: More sub-editors
        inventory.setItem(28, ItemBuilder.create("GOAT_HORN", "&d&lAnnounce",
                List.of("&7Click to edit announce config")));
        inventory.setItem(30, ItemBuilder.create("DIAMOND", "&6&lRewards (" + crate.getRewards().size() + ")",
                List.of("&7Click to manage rewards")));

        // Location
        String locStr = crate.isPhysical()
                ? crate.getCrateLocation().getWorldName() + " "
                + (int) crate.getCrateLocation().getX() + " "
                + (int) crate.getCrateLocation().getY() + " "
                + (int) crate.getCrateLocation().getZ()
                : "None (Virtual)";
        inventory.setItem(32, ItemBuilder.create("COMPASS", "&e&lLocation: &f" + locStr,
                List.of("&7Click to set to your position", "&7Shift-click to clear (virtual)")));

        // Bottom: Save, Back
        inventory.setItem(45, ItemBuilder.create("ARROW", "&c&lBack", List.of("&7Return to crate list")));
        inventory.setItem(49, ItemBuilder.create("LIME_WOOL", "&a&lSave to File",
                List.of("&7Saves this crate to YAML")));

        player.openInventory(inventory);
    }

    private ItemStack createAnimationSelector(Crate crate) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Click to cycle animations");
        lore.add("");
        for (String animId : plugin.getAnimationRegistry().keySet()) {
            if (animId.equals(crate.getAnimationId())) {
                lore.add("&a> " + animId);
            } else {
                lore.add("&7  " + animId);
            }
        }
        return ItemBuilder.create("CLOCK", "&e&lAnimation: &f" + crate.getAnimationId(), lore);
    }

    private void handleCrateEdit(int slot, boolean shiftClick) {
        if (editingCrate == null) return;
        switch (slot) {
            case 10 -> requestInput("Enter new crate name:", input -> {
                editingCrate.setName(input);
                openCrateEditor(editingCrate);
            });
            case 12 -> {
                // Cycle animation
                List<String> animIds = new ArrayList<>(plugin.getAnimationRegistry().keySet());
                if (!animIds.isEmpty()) {
                    int idx = animIds.indexOf(editingCrate.getAnimationId());
                    editingCrate.setAnimationId(animIds.get((idx + 1) % animIds.size()));
                    openCrateEditor(editingCrate);
                }
            }
            case 14 -> {
                CrateKeyConfig kc = editingCrate.getKeyConfig();
                if (kc == null) {
                    kc = new CrateKeyConfig();
                    kc.setRequired(true);
                    kc.setMaterial("TRIPWIRE_HOOK");
                    kc.setCount(1);
                    editingCrate.setKeyConfig(kc);
                } else {
                    kc.setRequired(!kc.isRequired());
                }
                openCrateEditor(editingCrate);
            }
            case 16 -> {
                editingCrate.setBounceBack(!editingCrate.isBounceBack());
                openCrateEditor(editingCrate);
            }
            case 19 -> openKeyEditor(editingCrate);
            case 21 -> openItemEditor(editingCrate);
            case 23 -> openHologramEditor(editingCrate);
            case 25 -> openPreviewEditor(editingCrate);
            case 28 -> openAnnounceEditor(editingCrate);
            case 30 -> openRewardsList(editingCrate);
            case 32 -> {
                if (shiftClick) {
                    editingCrate.setCrateLocation(null);
                } else {
                    CrateLocation loc = new CrateLocation(
                            player.getWorld().getName(),
                            player.getLocation().getBlockX(),
                            player.getLocation().getBlockY(),
                            player.getLocation().getBlockZ()
                    );
                    editingCrate.setCrateLocation(loc);
                }
                openCrateEditor(editingCrate);
            }
            case 45 -> openCrateList();
            case 49 -> {
                saveCrate(editingCrate);
                player.sendMessage(TextUtil.parse("&aCrate saved to file!"));
            }
        }
    }

    // ========================
    // Key Editor
    // ========================

    private void openKeyEditor(Crate crate) {
        mode = EditorMode.KEY_EDIT;
        editingCrate = crate;
        CrateKeyConfig kc = crate.getKeyConfig();

        inventory = Bukkit.createInventory(this, 54, TextUtil.parse("&4&lKey Settings: " + crate.getName()));
        fillBlack();

        if (kc == null) {
            inventory.setItem(22, ItemBuilder.create("BARRIER", "&cNo key config",
                    List.of("&7Click to create default key config")));
        } else {
            inventory.setItem(10, ItemBuilder.create(kc.isRequired() ? "LIME_DYE" : "GRAY_DYE",
                    "&e&lRequired: &f" + kc.isRequired(), List.of("&7Click to toggle")));
            inventory.setItem(12, ItemBuilder.create(kc.getMaterial() != null ? kc.getMaterial() : "TRIPWIRE_HOOK",
                    "&e&lMaterial: &f" + (kc.getMaterial() != null ? kc.getMaterial() : "TRIPWIRE_HOOK"),
                    List.of("&7Click to change material")));
            inventory.setItem(14, createNumberItem("PAPER", "Count", kc.getCount()));
            inventory.setItem(16, ItemBuilder.create("NAME_TAG",
                    "&e&lName: &f" + (kc.getName() != null ? kc.getName() : "none"),
                    List.of("&7Click to set name")));

            // Lore
            List<String> loreLines = new ArrayList<>();
            loreLines.add("&7Click to add a line");
            loreLines.add("&7Right-click to clear lore");
            loreLines.add("");
            if (kc.getLore() != null) {
                for (String l : kc.getLore()) loreLines.add("&f" + l);
            }
            inventory.setItem(28, ItemBuilder.create("WRITABLE_BOOK", "&e&lLore", loreLines));

            // Enchantments
            List<String> enchLore = new ArrayList<>();
            enchLore.add("&7Click to add enchantment");
            enchLore.add("&7Right-click to clear");
            enchLore.add("");
            if (kc.getEnchantments() != null) {
                for (var e : kc.getEnchantments().entrySet()) {
                    enchLore.add("&f" + e.getKey() + " &7= &f" + e.getValue());
                }
            }
            inventory.setItem(30, ItemBuilder.create("ENCHANTING_TABLE", "&e&lEnchantments", enchLore));

            // Item Flags
            List<String> flagLore = new ArrayList<>();
            flagLore.add("&7Click to add flag");
            flagLore.add("&7Right-click to clear");
            flagLore.add("");
            if (kc.getItemFlags() != null) {
                for (String f : kc.getItemFlags()) flagLore.add("&f" + f);
            }
            inventory.setItem(32, ItemBuilder.create("SHIELD", "&e&lItem Flags", flagLore));
        }

        inventory.setItem(45, ItemBuilder.create("ARROW", "&c&lBack", List.of("&7Return to crate editor")));
        inventory.setItem(49, ItemBuilder.create("LIME_WOOL", "&a&lSave", List.of("&7Save crate to file")));
        player.openInventory(inventory);
    }

    private void handleKeyEdit(int slot, boolean rightClick, boolean shiftClick) {
        if (editingCrate == null) return;
        CrateKeyConfig kcRaw = editingCrate.getKeyConfig();

        if (slot == 22 && kcRaw == null) {
            kcRaw = new CrateKeyConfig();
            kcRaw.setRequired(true);
            kcRaw.setMaterial("TRIPWIRE_HOOK");
            kcRaw.setCount(1);
            kcRaw.setName("&6&l" + editingCrate.getName() + " Key");
            editingCrate.setKeyConfig(kcRaw);
            openKeyEditor(editingCrate);
            return;
        }
        if (kcRaw == null) return;
        final CrateKeyConfig kc = kcRaw;

        switch (slot) {
            case 10 -> { kc.setRequired(!kc.isRequired()); openKeyEditor(editingCrate); }
            case 12 -> requestInput("Enter key material (e.g. TRIPWIRE_HOOK):", input -> {
                Material m = Material.matchMaterial(input.toUpperCase());
                if (m != null) kc.setMaterial(input.toUpperCase());
                else player.sendMessage(TextUtil.parse("&cInvalid material: " + input));
                openKeyEditor(editingCrate);
            });
            case 14 -> {
                int delta = rightClick ? -1 : 1;
                if (shiftClick) delta *= 5;
                kc.setCount(Math.max(1, kc.getCount() + delta));
                openKeyEditor(editingCrate);
            }
            case 16 -> requestInput("Enter key display name:", input -> {
                kc.setName(input);
                openKeyEditor(editingCrate);
            });
            case 28 -> {
                if (rightClick) {
                    kc.setLore(null);
                    openKeyEditor(editingCrate);
                } else {
                    requestInput("Enter lore line to add:", input -> {
                        List<String> lore = kc.getLore() != null ? new ArrayList<>(kc.getLore()) : new ArrayList<>();
                        lore.add(input);
                        kc.setLore(lore);
                        openKeyEditor(editingCrate);
                    });
                }
            }
            case 30 -> {
                if (rightClick) {
                    kc.setEnchantments(null);
                    openKeyEditor(editingCrate);
                } else {
                    requestInput("Enter enchantment (e.g. UNBREAKING 3):", input -> {
                        String[] parts = input.split("\\s+");
                        if (parts.length >= 2) {
                            Map<String, Integer> enc = kc.getEnchantments() != null ? new LinkedHashMap<>(kc.getEnchantments()) : new LinkedHashMap<>();
                            try {
                                enc.put(parts[0].toUpperCase(), Integer.parseInt(parts[1]));
                                kc.setEnchantments(enc);
                            } catch (NumberFormatException ignored) {
                                player.sendMessage(TextUtil.parse("&cInvalid format. Use: ENCHANTMENT LEVEL"));
                            }
                        } else {
                            player.sendMessage(TextUtil.parse("&cUse format: ENCHANTMENT LEVEL"));
                        }
                        openKeyEditor(editingCrate);
                    });
                }
            }
            case 32 -> {
                if (rightClick) {
                    kc.setItemFlags(null);
                    openKeyEditor(editingCrate);
                } else {
                    requestInput("Enter item flag (e.g. HIDE_ENCHANTS):", input -> {
                        List<String> flags = kc.getItemFlags() != null ? new ArrayList<>(kc.getItemFlags()) : new ArrayList<>();
                        flags.add(input.toUpperCase());
                        kc.setItemFlags(flags);
                        openKeyEditor(editingCrate);
                    });
                }
            }
            case 45 -> openCrateEditor(editingCrate);
            case 49 -> { saveCrate(editingCrate); player.sendMessage(TextUtil.parse("&aCrate saved!")); }
        }
    }

    // ========================
    // Item Editor
    // ========================

    private void openItemEditor(Crate crate) {
        mode = EditorMode.ITEM_EDIT;
        editingCrate = crate;
        CrateItemConfig ic = crate.getItemConfig();

        inventory = Bukkit.createInventory(this, 36, TextUtil.parse("&4&lItem Display: " + crate.getName()));
        fillBlack();

        if (ic == null) {
            inventory.setItem(13, ItemBuilder.create("BARRIER", "&cNo item config",
                    List.of("&7Click to create default")));
        } else {
            inventory.setItem(11, ItemBuilder.create(ic.getMaterial() != null ? ic.getMaterial() : "CHEST",
                    "&e&lMaterial: &f" + (ic.getMaterial() != null ? ic.getMaterial() : "CHEST"),
                    List.of("&7Click to change")));
            inventory.setItem(13, ItemBuilder.create("NAME_TAG",
                    "&e&lName: &f" + (ic.getName() != null ? ic.getName() : "none"),
                    List.of("&7Click to set")));

            List<String> loreLore = new ArrayList<>();
            loreLore.add("&7Click to add line");
            loreLore.add("&7Right-click to clear");
            loreLore.add("");
            if (ic.getLore() != null) for (String l : ic.getLore()) loreLore.add("&f" + l);
            inventory.setItem(15, ItemBuilder.create("WRITABLE_BOOK", "&e&lLore", loreLore));
        }

        inventory.setItem(27, ItemBuilder.create("ARROW", "&c&lBack", List.of("&7Return to crate editor")));
        inventory.setItem(31, ItemBuilder.create("LIME_WOOL", "&a&lSave", List.of("&7Save crate")));
        player.openInventory(inventory);
    }

    private void handleItemEdit(int slot) {
        if (editingCrate == null) return;
        CrateItemConfig ic = editingCrate.getItemConfig();

        if (slot == 13 && ic == null) {
            ic = new CrateItemConfig();
            ic.setMaterial("CHEST");
            ic.setName("&6&l" + editingCrate.getName());
            editingCrate.setItemConfig(ic);
            openItemEditor(editingCrate);
            return;
        }
        if (ic == null) return;

        CrateItemConfig finalIc = ic;
        switch (slot) {
            case 11 -> requestInput("Enter item material:", input -> {
                Material m = Material.matchMaterial(input.toUpperCase());
                if (m != null) finalIc.setMaterial(input.toUpperCase());
                else player.sendMessage(TextUtil.parse("&cInvalid material: " + input));
                openItemEditor(editingCrate);
            });
            case 13 -> requestInput("Enter item display name:", input -> {
                finalIc.setName(input);
                openItemEditor(editingCrate);
            });
            case 15 -> requestInput("Enter lore line (or 'clear' to clear):", input -> {
                if (input.equalsIgnoreCase("clear")) {
                    finalIc.setLore(null);
                } else {
                    List<String> lore = finalIc.getLore() != null ? new ArrayList<>(finalIc.getLore()) : new ArrayList<>();
                    lore.add(input);
                    finalIc.setLore(lore);
                }
                openItemEditor(editingCrate);
            });
            case 27 -> openCrateEditor(editingCrate);
            case 31 -> { saveCrate(editingCrate); player.sendMessage(TextUtil.parse("&aCrate saved!")); }
        }
    }

    // ========================
    // Hologram Editor
    // ========================

    private void openHologramEditor(Crate crate) {
        mode = EditorMode.HOLOGRAM_EDIT;
        editingCrate = crate;
        HologramConfig hc = crate.getHologramConfig();

        inventory = Bukkit.createInventory(this, 36, TextUtil.parse("&4&lHologram: " + crate.getName()));
        fillBlack();

        if (hc == null) {
            inventory.setItem(13, ItemBuilder.create("BARRIER", "&cNo hologram config",
                    List.of("&7Click to create default")));
        } else {
            List<String> lineInfo = new ArrayList<>();
            lineInfo.add("&7Click to add line");
            lineInfo.add("&7Right-click to clear all");
            lineInfo.add("");
            if (hc.getLines() != null) {
                for (String l : hc.getLines()) lineInfo.add("&f" + l);
            }
            inventory.setItem(11, ItemBuilder.create("WRITABLE_BOOK", "&e&lLines", lineInfo));

            inventory.setItem(13, createNumberItemDouble("COMPASS", "Offset X", hc.getOffsetX()));
            inventory.setItem(14, createNumberItemDouble("COMPASS", "Offset Y", hc.getOffsetY()));
            inventory.setItem(15, createNumberItemDouble("COMPASS", "Offset Z", hc.getOffsetZ()));
        }

        inventory.setItem(27, ItemBuilder.create("ARROW", "&c&lBack", List.of("&7Return to crate editor")));
        inventory.setItem(31, ItemBuilder.create("LIME_WOOL", "&a&lSave", List.of("&7Save crate")));
        player.openInventory(inventory);
    }

    private void handleHologramEdit(int slot) {
        if (editingCrate == null) return;
        HologramConfig hc = editingCrate.getHologramConfig();

        if (slot == 13 && hc == null) {
            hc = new HologramConfig();
            hc.setLines(new ArrayList<>(List.of("&6&l" + editingCrate.getName())));
            hc.setOffsetY(1.5);
            editingCrate.setHologramConfig(hc);
            openHologramEditor(editingCrate);
            return;
        }
        if (hc == null) return;

        HologramConfig finalHc = hc;
        switch (slot) {
            case 11 -> requestInput("Enter hologram line (or 'clear'):", input -> {
                if (input.equalsIgnoreCase("clear")) {
                    finalHc.setLines(new ArrayList<>());
                } else {
                    List<String> lines = finalHc.getLines() != null ? new ArrayList<>(finalHc.getLines()) : new ArrayList<>();
                    lines.add(input);
                    finalHc.setLines(lines);
                }
                openHologramEditor(editingCrate);
            });
            case 13 -> requestInput("Enter offset X:", input -> {
                try { finalHc.setOffsetX(Double.parseDouble(input)); } catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("&cInvalid number")); }
                openHologramEditor(editingCrate);
            });
            case 14 -> requestInput("Enter offset Y:", input -> {
                try { finalHc.setOffsetY(Double.parseDouble(input)); } catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("&cInvalid number")); }
                openHologramEditor(editingCrate);
            });
            case 15 -> requestInput("Enter offset Z:", input -> {
                try { finalHc.setOffsetZ(Double.parseDouble(input)); } catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("&cInvalid number")); }
                openHologramEditor(editingCrate);
            });
            case 27 -> openCrateEditor(editingCrate);
            case 31 -> { saveCrate(editingCrate); player.sendMessage(TextUtil.parse("&aCrate saved!")); }
        }
    }

    // ========================
    // Preview Editor
    // ========================

    private void openPreviewEditor(Crate crate) {
        mode = EditorMode.PREVIEW_EDIT;
        editingCrate = crate;
        PreviewConfig pc = crate.getPreviewConfig();

        inventory = Bukkit.createInventory(this, 54, TextUtil.parse("&4&lPreview: " + crate.getName()));
        fillBlack();

        if (pc == null) {
            inventory.setItem(22, ItemBuilder.create("BARRIER", "&cNo preview config",
                    List.of("&7Click to create default")));
        } else {
            inventory.setItem(10, ItemBuilder.create("NAME_TAG",
                    "&e&lTitle: &f" + pc.getTitle(), List.of("&7Click to set")));
            inventory.setItem(12, createNumberItem("LADDER", "Rows", pc.getRows()));

            // Fill
            String fillMat = pc.getFill() != null ? pc.getFill().getMaterial() : "none";
            inventory.setItem(14, ItemBuilder.create(fillMat.equals("none") ? "BARRIER" : fillMat,
                    "&e&lFill Material: &f" + fillMat, List.of("&7Click to set")));

            // Nav buttons
            inventory.setItem(19, createSlotItemDisplay("Close Button", pc.getCloseButton()));
            inventory.setItem(21, createSlotItemDisplay("Next Button", pc.getNextButton()));
            inventory.setItem(23, createSlotItemDisplay("Previous Button", pc.getPreviousButton()));

            // Sounds
            List<String> soundLore = new ArrayList<>();
            soundLore.add("&7Click to add sound");
            soundLore.add("&7Right-click to clear");
            if (pc.getSounds() != null) for (String s : pc.getSounds()) soundLore.add("&f" + s);
            inventory.setItem(25, ItemBuilder.create("NOTE_BLOCK", "&e&lSounds", soundLore));
        }

        inventory.setItem(45, ItemBuilder.create("ARROW", "&c&lBack", List.of("&7Return to crate editor")));
        inventory.setItem(49, ItemBuilder.create("LIME_WOOL", "&a&lSave", List.of("&7Save crate")));
        player.openInventory(inventory);
    }

    private void handlePreviewEdit(int slot) {
        if (editingCrate == null) return;
        PreviewConfig pc = editingCrate.getPreviewConfig();

        if (slot == 22 && pc == null) {
            pc = new PreviewConfig();
            pc.setTitle("&6Preview: " + editingCrate.getName());
            pc.setRows(6);
            PreviewConfig.GuiItem fill = new PreviewConfig.GuiItem();
            fill.setMaterial("BLACK_STAINED_GLASS_PANE");
            fill.setName(" ");
            pc.setFill(fill);
            editingCrate.setPreviewConfig(pc);
            openPreviewEditor(editingCrate);
            return;
        }
        if (pc == null) return;

        PreviewConfig finalPc = pc;
        switch (slot) {
            case 10 -> requestInput("Enter preview title:", input -> {
                finalPc.setTitle(input);
                openPreviewEditor(editingCrate);
            });
            case 12 -> requestInput("Enter number of rows (1-6):", input -> {
                try {
                    int r = Integer.parseInt(input);
                    finalPc.setRows(Math.max(1, Math.min(6, r)));
                } catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("&cInvalid number")); }
                openPreviewEditor(editingCrate);
            });
            case 14 -> requestInput("Enter fill material:", input -> {
                if (finalPc.getFill() == null) {
                    PreviewConfig.GuiItem fill = new PreviewConfig.GuiItem();
                    fill.setMaterial(input.toUpperCase());
                    fill.setName(" ");
                    finalPc.setFill(fill);
                } else {
                    finalPc.getFill().setMaterial(input.toUpperCase());
                }
                openPreviewEditor(editingCrate);
            });
            case 19 -> editSlotItem("Close Button", finalPc.getCloseButton(), item -> {
                finalPc.setCloseButton(item);
                openPreviewEditor(editingCrate);
            });
            case 21 -> editSlotItem("Next Button", finalPc.getNextButton(), item -> {
                finalPc.setNextButton(item);
                openPreviewEditor(editingCrate);
            });
            case 23 -> editSlotItem("Previous Button", finalPc.getPreviousButton(), item -> {
                finalPc.setPreviousButton(item);
                openPreviewEditor(editingCrate);
            });
            case 25 -> requestInput("Enter sound name (or 'clear'):", input -> {
                if (input.equalsIgnoreCase("clear")) {
                    finalPc.setSounds(null);
                } else {
                    List<String> sounds = finalPc.getSounds() != null ? new ArrayList<>(finalPc.getSounds()) : new ArrayList<>();
                    sounds.add(input);
                    finalPc.setSounds(sounds);
                }
                openPreviewEditor(editingCrate);
            });
            case 45 -> openCrateEditor(editingCrate);
            case 49 -> { saveCrate(editingCrate); player.sendMessage(TextUtil.parse("&aCrate saved!")); }
        }
    }

    // ========================
    // Announce Editor
    // ========================

    private void openAnnounceEditor(Crate crate) {
        mode = EditorMode.ANNOUNCE_EDIT;
        editingCrate = crate;
        AnnounceConfig ac = crate.getAnnounceConfig();

        inventory = Bukkit.createInventory(this, 36, TextUtil.parse("&4&lAnnounce: " + crate.getName()));
        fillBlack();

        if (ac == null) {
            inventory.setItem(13, ItemBuilder.create("BARRIER", "&cNo announce config",
                    List.of("&7Click to create default")));
        } else {
            inventory.setItem(10, ItemBuilder.create(ac.isToEveryone() ? "LIME_DYE" : "GRAY_DYE",
                    "&e&lTo Everyone: &f" + ac.isToEveryone(), List.of("&7Click to toggle")));
            inventory.setItem(12, ItemBuilder.create("PAPER", "&e&lSingle: &f" + orNone(ac.getSingle()),
                    List.of("&7Click to set")));
            inventory.setItem(14, ItemBuilder.create("PAPER", "&e&lMultiple: &f" + orNone(ac.getMultiple()),
                    List.of("&7Click to set")));
            inventory.setItem(16, ItemBuilder.create("PAPER", "&e&lMultiple Item: &f" + orNone(ac.getMultipleItem()),
                    List.of("&7Click to set")));
        }

        inventory.setItem(27, ItemBuilder.create("ARROW", "&c&lBack", List.of("&7Return to crate editor")));
        inventory.setItem(31, ItemBuilder.create("LIME_WOOL", "&a&lSave", List.of("&7Save crate")));
        player.openInventory(inventory);
    }

    private void handleAnnounceEdit(int slot) {
        if (editingCrate == null) return;
        AnnounceConfig ac = editingCrate.getAnnounceConfig();

        if (slot == 13 && ac == null) {
            ac = new AnnounceConfig();
            ac.setToEveryone(true);
            ac.setSingle("&6<player> &7won &6<reward_name> &7from &6" + editingCrate.getName());
            editingCrate.setAnnounceConfig(ac);
            openAnnounceEditor(editingCrate);
            return;
        }
        if (ac == null) return;

        AnnounceConfig finalAc = ac;
        switch (slot) {
            case 10 -> { finalAc.setToEveryone(!finalAc.isToEveryone()); openAnnounceEditor(editingCrate); }
            case 12 -> requestInput("Enter single announce message:", input -> {
                finalAc.setSingle(input);
                openAnnounceEditor(editingCrate);
            });
            case 14 -> requestInput("Enter multiple announce message:", input -> {
                finalAc.setMultiple(input);
                openAnnounceEditor(editingCrate);
            });
            case 16 -> requestInput("Enter multiple item message:", input -> {
                finalAc.setMultipleItem(input);
                openAnnounceEditor(editingCrate);
            });
            case 27 -> openCrateEditor(editingCrate);
            case 31 -> { saveCrate(editingCrate); player.sendMessage(TextUtil.parse("&aCrate saved!")); }
        }
    }

    // ========================
    // Rewards List
    // ========================

    private void openRewardsList(Crate crate) {
        mode = EditorMode.REWARDS_LIST;
        editingCrate = crate;

        var rewardsList = new ArrayList<>(crate.getRewards().entrySet());
        int rows = Math.max(2, Math.min(6, (int) Math.ceil((rewardsList.size() + 2) / 9.0) + 1));

        inventory = Bukkit.createInventory(this, rows * 9, TextUtil.parse("&4&lRewards: " + crate.getName()));
        fillBlack();

        inventory.setItem(inventory.getSize() - 9, ItemBuilder.create("ARROW", "&c&lBack", List.of("&7Return to crate editor")));
        inventory.setItem(inventory.getSize() - 1, ItemBuilder.create("EMERALD", "&a&lAdd New Reward",
                List.of("&7Click to add a new reward")));

        for (int i = 0; i < rewardsList.size() && i < inventory.getSize() - 9; i++) {
            var entry = rewardsList.get(i);
            Reward reward = entry.getValue();

            ItemStack item;
            if (reward.getDisplay() != null) {
                item = ItemBuilder.fromDisplay(reward.getDisplay());
            } else {
                item = new ItemStack(Material.STONE);
            }

            ItemMeta meta = item.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("&7ID: &f" + entry.getKey()));
            lore.add(TextUtil.parse("&7Chance: &f" + reward.getChance()));
            lore.add(TextUtil.parse("&7Commands: &f" + (reward.hasCommands() ? reward.getCommands().size() : 0)));
            lore.add(TextUtil.parse("&7Items: &f" + (reward.hasItems() ? reward.getItems().size() : 0)));
            lore.add(TextUtil.parse(""));
            lore.add(TextUtil.parse("&eLeft-click to edit"));
            lore.add(TextUtil.parse("&cRight-click to delete"));
            meta.lore(lore);

            setEditorTagMeta(meta, "editor_reward", entry.getKey());
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }
        player.openInventory(inventory);
    }

    private void handleRewardsList(int slot, boolean rightClick) {
        if (editingCrate == null) return;

        if (slot == inventory.getSize() - 9) { openCrateEditor(editingCrate); return; }
        if (slot == inventory.getSize() - 1) {
            String newId = "reward_" + System.currentTimeMillis() % 10000;
            Reward reward = new Reward(newId);
            reward.setChance(1.0);
            RewardDisplay display = new RewardDisplay();
            display.setMaterial("STONE");
            display.setName("&7New Reward");
            reward.setDisplay(display);
            editingCrate.getRewards().put(newId, reward);
            openRewardsList(editingCrate);
            return;
        }

        String rewardId = getEditorTag(slot, "editor_reward");
        if (rewardId == null) return;

        if (rightClick) {
            editingCrate.getRewards().remove(rewardId);
            openRewardsList(editingCrate);
        } else {
            openRewardEditor(editingCrate, rewardId);
        }
    }

    // ========================
    // Reward Editor
    // ========================

    private void openRewardEditor(Crate crate, String rewardId) {
        mode = EditorMode.REWARD_EDIT;
        editingCrate = crate;
        editingRewardId = rewardId;

        Reward reward = crate.getRewards().get(rewardId);
        if (reward == null) return;

        inventory = Bukkit.createInventory(this, 54, TextUtil.parse("&4&lReward: " + rewardId));
        fillBlack();

        // Display preview
        if (reward.getDisplay() != null) {
            inventory.setItem(4, ItemBuilder.fromDisplay(reward.getDisplay()));
        } else {
            inventory.setItem(4, ItemBuilder.create("STONE", "&7No display item", null));
        }

        // Display material
        inventory.setItem(19, ItemBuilder.create("ITEM_FRAME",
                "&e&lDisplay Material: &f" + (reward.getDisplay() != null ? reward.getDisplay().getMaterial() : "none"),
                List.of("&7Click to change")));

        // Display name
        inventory.setItem(20, ItemBuilder.create("NAME_TAG",
                "&e&lDisplay Name: &f" + (reward.getDisplay() != null && reward.getDisplay().getName() != null ? reward.getDisplay().getName() : "none"),
                List.of("&7Click to change")));

        // Display lore
        List<String> dispLore = new ArrayList<>();
        dispLore.add("&7Click to add line");
        dispLore.add("&7Right-click to clear");
        dispLore.add("");
        if (reward.getDisplay() != null && reward.getDisplay().getLore() != null) {
            for (String l : reward.getDisplay().getLore()) dispLore.add("&f" + l);
        }
        inventory.setItem(21, ItemBuilder.create("WRITABLE_BOOK", "&e&lDisplay Lore", dispLore));

        // Chance
        inventory.setItem(23, ItemBuilder.create("EXPERIENCE_BOTTLE",
                "&e&lChance: &f" + reward.getChance(),
                List.of("&7Left-click +1 | Right-click -1", "&7Shift for +/- 5")));

        // Commands
        List<String> cmdLore = new ArrayList<>();
        cmdLore.add("&7Click to add command");
        cmdLore.add("&7Right-click to clear");
        cmdLore.add("");
        if (reward.hasCommands()) for (String c : reward.getCommands()) cmdLore.add("&f" + c);
        inventory.setItem(25, ItemBuilder.create("COMMAND_BLOCK", "&e&lCommands", cmdLore));

        // Announce
        inventory.setItem(37, ItemBuilder.create("GOAT_HORN",
                "&e&lAnnounce: &f" + orNone(reward.getAnnounce()),
                List.of("&7Click to set", "&7Right-click to clear")));

        // Items
        inventory.setItem(39, ItemBuilder.create("CHEST",
                "&e&lItems: &f" + (reward.hasItems() ? reward.getItems().size() : 0),
                List.of("&7Reward items are edited via YAML")));

        inventory.setItem(45, ItemBuilder.create("ARROW", "&c&lBack", List.of("&7Return to rewards list")));
        inventory.setItem(49, ItemBuilder.create("LIME_WOOL", "&a&lSave", List.of("&7Save crate")));
        player.openInventory(inventory);
    }

    private void handleRewardEdit(int slot, boolean rightClick, boolean shiftClick) {
        if (editingCrate == null || editingRewardId == null) return;
        Reward reward = editingCrate.getRewards().get(editingRewardId);
        if (reward == null) return;

        switch (slot) {
            case 19 -> requestInput("Enter display material:", input -> {
                ensureDisplay(reward).setMaterial(input.toUpperCase());
                openRewardEditor(editingCrate, editingRewardId);
            });
            case 20 -> requestInput("Enter display name:", input -> {
                ensureDisplay(reward).setName(input);
                openRewardEditor(editingCrate, editingRewardId);
            });
            case 21 -> {
                if (rightClick) {
                    if (reward.getDisplay() != null) reward.getDisplay().setLore(null);
                    openRewardEditor(editingCrate, editingRewardId);
                } else {
                    requestInput("Enter display lore line:", input -> {
                        RewardDisplay d = ensureDisplay(reward);
                        List<String> lore = d.getLore() != null ? new ArrayList<>(d.getLore()) : new ArrayList<>();
                        lore.add(input);
                        d.setLore(lore);
                        openRewardEditor(editingCrate, editingRewardId);
                    });
                }
            }
            case 23 -> {
                double delta = rightClick ? -1 : 1;
                if (shiftClick) delta *= 5;
                reward.setChance(Math.max(0.01, reward.getChance() + delta));
                openRewardEditor(editingCrate, editingRewardId);
            }
            case 25 -> {
                if (rightClick) {
                    reward.setCommands(null);
                    openRewardEditor(editingCrate, editingRewardId);
                } else {
                    requestInput("Enter command (use <player> for player name):", input -> {
                        List<String> cmds = reward.getCommands() != null ? new ArrayList<>(reward.getCommands()) : new ArrayList<>();
                        cmds.add(input);
                        reward.setCommands(cmds);
                        openRewardEditor(editingCrate, editingRewardId);
                    });
                }
            }
            case 37 -> {
                if (rightClick) {
                    reward.setAnnounce(null);
                    openRewardEditor(editingCrate, editingRewardId);
                } else {
                    requestInput("Enter announce message:", input -> {
                        reward.setAnnounce(input);
                        openRewardEditor(editingCrate, editingRewardId);
                    });
                }
            }
            case 45 -> openRewardsList(editingCrate);
            case 49 -> { saveCrate(editingCrate); player.sendMessage(TextUtil.parse("&aCrate saved!")); }
        }
    }

    // ========================
    // Animation List
    // ========================

    private void openAnimationList() {
        mode = EditorMode.ANIMATION_LIST;
        var animations = new ArrayList<>(plugin.getAnimationRegistry().entrySet());
        int rows = Math.max(2, Math.min(6, (int) Math.ceil((animations.size() + 2) / 9.0) + 1));

        inventory = Bukkit.createInventory(this, rows * 9, TextUtil.parse("&4&lAnimations"));
        fillBlack();

        inventory.setItem(inventory.getSize() - 9, ItemBuilder.create("ARROW", "&c&lBack", List.of("&7Return to main menu")));
        inventory.setItem(inventory.getSize() - 1, ItemBuilder.create("EMERALD", "&a&lCreate New Animation",
                List.of("&7Click to create a new animation")));

        for (int i = 0; i < animations.size() && i < inventory.getSize() - 9; i++) {
            var entry = animations.get(i);
            Animation anim = entry.getValue();
            ItemStack item = ItemBuilder.create("CLOCK", "&b&l" + entry.getKey(), List.of(
                    "&7Type: &f" + anim.getTypeId(),
                    "&7Rows: &f" + anim.getGuiRows(),
                    "&7Title: &f" + anim.getGuiTitle(),
                    "", "&eLeft-click to edit", "&cRight-click to delete"
            ));
            setEditorTag(item, "editor_anim", entry.getKey());
            inventory.setItem(i, item);
        }
        player.openInventory(inventory);
    }

    private void handleAnimationList(int slot, boolean rightClick) {
        if (slot == inventory.getSize() - 9) { open(); return; }
        if (slot == inventory.getSize() - 1) {
            String newId = "new_anim_" + System.currentTimeMillis() % 10000;
            Animation anim = new Animation(newId);
            anim.setTypeId("csgo");
            anim.setTotalTicks(45);
            anim.setStayOpenAfterRewardTicks(50);
            anim.setStartTickRate(1);
            anim.setTickRateModifier(20);
            anim.setGuiTitle("&c&l<crate>");
            anim.setGuiRows(3);
            anim.setRewardIndex(5);
            anim.setRewardSlots(List.of(9, 10, 11, 12, 13, 14, 15, 16, 17));
            GuiItemConfig fill = new GuiItemConfig();
            fill.setMaterial("BLACK_STAINED_GLASS_PANE");
            fill.setName(" ");
            anim.setGuiFill(fill);
            plugin.getAnimationRegistry().put(newId, anim);
            saveAnimation(anim);
            openAnimationEditor(anim);
            return;
        }

        String animId = getEditorTag(slot, "editor_anim");
        if (animId == null) return;
        Animation anim = plugin.getAnimationRegistry().get(animId);
        if (anim == null) return;

        if (rightClick) {
            plugin.getAnimationRegistry().remove(animId);
            File f = new File(plugin.getDataFolder(), "animations/" + animId + ".yml");
            if (f.exists()) f.delete();
            openAnimationList();
        } else {
            openAnimationEditor(anim);
        }
    }

    // ========================
    // Animation Editor
    // ========================

    private void openAnimationEditor(Animation anim) {
        mode = EditorMode.ANIMATION_EDIT;
        editingAnimation = anim;

        inventory = Bukkit.createInventory(this, 54, TextUtil.parse("&4&lAnimation: " + anim.getId()));
        fillBlack();

        // Type
        List<String> typeLore = new ArrayList<>();
        typeLore.add("&7Click to cycle type");
        for (String typeId : plugin.getAnimationTypeRegistry().keySet()) {
            typeLore.add(typeId.equals(anim.getTypeId()) ? "&a> " + typeId : "&7  " + typeId);
        }
        inventory.setItem(10, ItemBuilder.create("REDSTONE", "&e&lType: &f" + anim.getTypeId(), typeLore));

        // GUI Title
        inventory.setItem(12, ItemBuilder.create("NAME_TAG", "&e&lGUI Title: &f" + anim.getGuiTitle(),
                List.of("&7Click to set")));

        // GUI Rows
        inventory.setItem(14, createNumberItem("LADDER", "GUI Rows", anim.getGuiRows()));

        // GUI Fill
        String fillMat = anim.getGuiFill() != null ? anim.getGuiFill().getMaterial() : "none";
        inventory.setItem(16, ItemBuilder.create(fillMat.equals("none") ? "BARRIER" : fillMat,
                "&e&lGUI Fill: &f" + fillMat, List.of("&7Click to set material")));

        // CSGO-type fields
        inventory.setItem(19, createNumberItem("REPEATER", "Total Ticks", anim.getTotalTicks()));
        inventory.setItem(20, createNumberItem("COMPARATOR", "Stay Open After", anim.getStayOpenAfterRewardTicks()));
        inventory.setItem(21, createNumberItem("REDSTONE_TORCH", "Start Tick Rate", anim.getStartTickRate()));
        inventory.setItem(22, createNumberItem("LEVER", "Tick Rate Modifier", anim.getTickRateModifier()));
        inventory.setItem(23, createNumberItem("GOLDEN_APPLE", "Reward Index", anim.getRewardIndex()));

        // Click-type fields
        inventory.setItem(24, createNumberItem("PISTON", "Shuffle Amount", anim.getShuffleAmount()));
        inventory.setItem(25, createNumberItem("STICKY_PISTON", "Shuffle Ticks", anim.getShuffleTicks()));

        // Reward amount
        inventory.setItem(28, createNumberItem("DIAMOND", "Reward Amount", anim.getRewardAmount()));
        inventory.setItem(29, createNumberItem("CLOCK", "Show Revealed For", anim.getShowRevealedItemsFor()));

        // Reward slots
        List<String> slotLore = new ArrayList<>();
        slotLore.add("&7Click to set via chat");
        slotLore.add("&7Current: &f" + (anim.getRewardSlots() != null ? anim.getRewardSlots().toString() : "[]"));
        inventory.setItem(31, ItemBuilder.create("CHEST_MINECART", "&e&lReward Slots", slotLore));

        // Filler slots
        List<String> fillerLore = new ArrayList<>();
        fillerLore.add("&7Click to set via chat");
        fillerLore.add("&7Current: &f" + (anim.getFillerSlots() != null ? anim.getFillerSlots().toString() : "[]"));
        inventory.setItem(33, ItemBuilder.create("HOPPER_MINECART", "&e&lFiller Slots", fillerLore));

        // Pointers
        inventory.setItem(37, ItemBuilder.create("ARROW", "&e&lDown Pointer",
                List.of("&7Slot: &f" + (anim.getDownPointer() != null ? anim.getDownPointer().getSlot() : "none"),
                        "&7Click to configure")));
        inventory.setItem(38, ItemBuilder.create("ARROW", "&e&lUp Pointer",
                List.of("&7Slot: &f" + (anim.getUpPointer() != null ? anim.getUpPointer().getSlot() : "none"),
                        "&7Click to configure")));

        // Sounds sub-menu
        inventory.setItem(40, ItemBuilder.create("NOTE_BLOCK", "&e&lSounds",
                List.of("&7Click to edit all sounds")));

        // Reward hide item
        String hideItemMat = anim.getRewardHideItem() != null ? anim.getRewardHideItem().getMaterial() : "none";
        inventory.setItem(42, ItemBuilder.create(hideItemMat.equals("none") ? "YELLOW_STAINED_GLASS_PANE" : hideItemMat,
                "&e&lReward Hide Item: &f" + hideItemMat,
                List.of("&7Click to set material", "&7Right-click to set name")));

        // Shuffling title
        inventory.setItem(43, ItemBuilder.create("NAME_TAG", "&e&lShuffling Title: &f" + orNone(anim.getGuiTitleShuffling()),
                List.of("&7Click to set")));

        // Bottom
        inventory.setItem(45, ItemBuilder.create("ARROW", "&c&lBack", List.of("&7Return to animation list")));
        inventory.setItem(49, ItemBuilder.create("LIME_WOOL", "&a&lSave", List.of("&7Save animation to file")));
        player.openInventory(inventory);
    }

    private void handleAnimationEdit(int slot, boolean rightClick, boolean shiftClick) {
        if (editingAnimation == null) return;
        Animation a = editingAnimation;

        switch (slot) {
            case 10 -> {
                List<String> types = new ArrayList<>(plugin.getAnimationTypeRegistry().keySet());
                if (!types.isEmpty()) {
                    int idx = types.indexOf(a.getTypeId());
                    a.setTypeId(types.get((idx + 1) % types.size()));
                    openAnimationEditor(a);
                }
            }
            case 12 -> requestInput("Enter GUI title:", input -> { a.setGuiTitle(input); openAnimationEditor(a); });
            case 14 -> { adjustInt(a::getGuiRows, a::setGuiRows, rightClick, shiftClick, 1, 6); openAnimationEditor(a); }
            case 16 -> requestInput("Enter fill material:", input -> {
                GuiItemConfig fill = a.getGuiFill() != null ? a.getGuiFill() : new GuiItemConfig();
                fill.setMaterial(input.toUpperCase());
                if (fill.getName() == null) fill.setName(" ");
                a.setGuiFill(fill);
                openAnimationEditor(a);
            });
            case 19 -> { adjustInt(a::getTotalTicks, a::setTotalTicks, rightClick, shiftClick, 1, 999); openAnimationEditor(a); }
            case 20 -> { adjustInt(a::getStayOpenAfterRewardTicks, a::setStayOpenAfterRewardTicks, rightClick, shiftClick, 0, 999); openAnimationEditor(a); }
            case 21 -> { adjustInt(a::getStartTickRate, a::setStartTickRate, rightClick, shiftClick, 1, 100); openAnimationEditor(a); }
            case 22 -> { adjustInt(a::getTickRateModifier, a::setTickRateModifier, rightClick, shiftClick, 0, 100); openAnimationEditor(a); }
            case 23 -> { adjustInt(a::getRewardIndex, a::setRewardIndex, rightClick, shiftClick, 1, 100); openAnimationEditor(a); }
            case 24 -> { adjustInt(a::getShuffleAmount, a::setShuffleAmount, rightClick, shiftClick, 1, 100); openAnimationEditor(a); }
            case 25 -> { adjustInt(a::getShuffleTicks, a::setShuffleTicks, rightClick, shiftClick, 1, 100); openAnimationEditor(a); }
            case 28 -> { adjustInt(a::getRewardAmount, a::setRewardAmount, rightClick, shiftClick, 1, 100); openAnimationEditor(a); }
            case 29 -> { adjustInt(a::getShowRevealedItemsFor, a::setShowRevealedItemsFor, rightClick, shiftClick, 1, 999); openAnimationEditor(a); }
            case 31 -> requestInput("Enter reward slots (comma-separated, e.g. 9,10,11,12):", input -> {
                a.setRewardSlots(parseIntList(input));
                openAnimationEditor(a);
            });
            case 33 -> requestInput("Enter filler slots (comma-separated, e.g. 0,1,2):", input -> {
                a.setFillerSlots(parseIntList(input));
                openAnimationEditor(a);
            });
            case 37 -> requestInput("Enter down pointer slot,material,name (e.g. 4,ARROW,&c▼):", input -> {
                PointerConfig p = parsePointerInput(input);
                a.setDownPointer(p);
                openAnimationEditor(a);
            });
            case 38 -> requestInput("Enter up pointer slot,material,name (e.g. 22,ARROW,&c▲):", input -> {
                PointerConfig p = parsePointerInput(input);
                a.setUpPointer(p);
                openAnimationEditor(a);
            });
            case 40 -> openAnimationSoundsEditor(a);
            case 42 -> {
                if (rightClick) {
                    requestInput("Enter hide item name:", input -> {
                        GuiItemConfig hi = a.getRewardHideItem() != null ? a.getRewardHideItem() : new GuiItemConfig();
                        hi.setName(input);
                        a.setRewardHideItem(hi);
                        openAnimationEditor(a);
                    });
                } else {
                    requestInput("Enter hide item material:", input -> {
                        GuiItemConfig hi = a.getRewardHideItem() != null ? a.getRewardHideItem() : new GuiItemConfig();
                        hi.setMaterial(input.toUpperCase());
                        a.setRewardHideItem(hi);
                        openAnimationEditor(a);
                    });
                }
            }
            case 43 -> requestInput("Enter shuffling title:", input -> { a.setGuiTitleShuffling(input); openAnimationEditor(a); });
            case 45 -> openAnimationList();
            case 49 -> { saveAnimation(a); player.sendMessage(TextUtil.parse("&aAnimation saved!")); }
        }
    }

    // ========================
    // Animation Sounds Editor
    // ========================

    private void openAnimationSoundsEditor(Animation anim) {
        mode = EditorMode.ANIMATION_SOUNDS_EDIT;
        editingAnimation = anim;

        inventory = Bukkit.createInventory(this, 36, TextUtil.parse("&4&lSounds: " + anim.getId()));
        fillBlack();

        inventory.setItem(10, createSoundItem("Tick Sound", anim.getTickSounds()));
        inventory.setItem(12, createSoundItem("Reward Sound", anim.getRewardSounds()));
        inventory.setItem(14, createSoundItem("Shuffle Sound", anim.getShuffleSounds()));
        inventory.setItem(16, createSoundItem("Hide Sound", anim.getHideSounds()));
        inventory.setItem(19, createSoundItem("Reveal Sound", anim.getRevealSounds()));

        inventory.setItem(27, ItemBuilder.create("ARROW", "&c&lBack", List.of("&7Return to animation editor")));
        player.openInventory(inventory);
    }

    private void handleAnimationSoundsEdit(int slot, boolean rightClick) {
        if (editingAnimation == null) return;
        Animation a = editingAnimation;

        java.util.function.BiConsumer<String, java.util.function.Consumer<List<String>>> handle = (prompt, setter) -> {
            if (rightClick) {
                setter.accept(null);
                openAnimationSoundsEditor(a);
            } else {
                requestInput(prompt, input -> {
                    setter.accept(List.of(input.split(",\\s*")));
                    openAnimationSoundsEditor(a);
                });
            }
        };

        switch (slot) {
            case 10 -> handle.accept("Enter tick sounds (comma-separated):", a::setTickSounds);
            case 12 -> handle.accept("Enter reward sounds (comma-separated):", a::setRewardSounds);
            case 14 -> handle.accept("Enter shuffle sounds (comma-separated):", a::setShuffleSounds);
            case 16 -> handle.accept("Enter hide sounds (comma-separated):", a::setHideSounds);
            case 19 -> handle.accept("Enter reveal sounds (comma-separated):", a::setRevealSounds);
            case 27 -> openAnimationEditor(a);
        }
    }

    // ========================
    // Helpers
    // ========================

    private void requestInput(String prompt, java.util.function.Consumer<String> callback) {
        player.closeInventory();
        player.sendMessage(TextUtil.parse("&e" + prompt));
        player.sendMessage(TextUtil.parse("&7Type 'cancel' to cancel."));
        plugin.getChatInputManager().awaitInput(player, input -> {
            if (!input.equalsIgnoreCase("cancel")) {
                callback.accept(input);
            } else {
                // Re-open the current screen
                reopenCurrentScreen();
            }
        });
    }

    private void reopenCurrentScreen() {
        switch (mode) {
            case MAIN_MENU -> open();
            case CRATE_LIST -> openCrateList();
            case CRATE_EDIT -> { if (editingCrate != null) openCrateEditor(editingCrate); }
            case KEY_EDIT -> { if (editingCrate != null) openKeyEditor(editingCrate); }
            case ITEM_EDIT -> { if (editingCrate != null) openItemEditor(editingCrate); }
            case HOLOGRAM_EDIT -> { if (editingCrate != null) openHologramEditor(editingCrate); }
            case PREVIEW_EDIT -> { if (editingCrate != null) openPreviewEditor(editingCrate); }
            case ANNOUNCE_EDIT -> { if (editingCrate != null) openAnnounceEditor(editingCrate); }
            case REWARDS_LIST -> { if (editingCrate != null) openRewardsList(editingCrate); }
            case REWARD_EDIT -> { if (editingCrate != null && editingRewardId != null) openRewardEditor(editingCrate, editingRewardId); }
            case ANIMATION_LIST -> openAnimationList();
            case ANIMATION_EDIT -> { if (editingAnimation != null) openAnimationEditor(editingAnimation); }
            case ANIMATION_SOUNDS_EDIT -> { if (editingAnimation != null) openAnimationSoundsEditor(editingAnimation); }
        }
    }

    private void editSlotItem(String label, PreviewConfig.SlotItem current, java.util.function.Consumer<PreviewConfig.SlotItem> setter) {
        requestInput("Enter " + label + " as slot,material,name (e.g. 49,BARRIER,&cClose):", input -> {
            String[] parts = input.split(",", 3);
            if (parts.length >= 3) {
                PreviewConfig.SlotItem item = new PreviewConfig.SlotItem();
                try { item.setSlot(Integer.parseInt(parts[0].trim())); } catch (NumberFormatException e) { item.setSlot(0); }
                item.setMaterial(parts[1].trim().toUpperCase());
                item.setName(parts[2].trim());
                setter.accept(item);
            } else {
                player.sendMessage(TextUtil.parse("&cInvalid format. Use: slot,material,name"));
                reopenCurrentScreen();
            }
        });
    }

    private void saveCrate(Crate crate) {
        try {
            plugin.getCrateConfigParser().save(crate, new File(plugin.getDataFolder(), "crates"));
        } catch (IOException e) {
            player.sendMessage(TextUtil.parse("&cFailed to save: " + e.getMessage()));
        }
    }

    private void saveAnimation(Animation anim) {
        try {
            plugin.getAnimationConfigParser().save(anim, new File(plugin.getDataFolder(), "animations"));
        } catch (IOException e) {
            player.sendMessage(TextUtil.parse("&cFailed to save: " + e.getMessage()));
        }
    }

    private void fillBlack() {
        ItemStack fill = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = fill.getItemMeta();
        meta.displayName(TextUtil.parse(" "));
        fill.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, fill);
        }
    }

    private ItemStack createNumberItem(String material, String label, int value) {
        return ItemBuilder.create(material, "&e&l" + label + ": &f" + value,
                List.of("&7Left-click +1 | Right-click -1", "&7Shift for +/- 5"));
    }

    private ItemStack createNumberItemDouble(String material, String label, double value) {
        return ItemBuilder.create(material, "&e&l" + label + ": &f" + String.format("%.2f", value),
                List.of("&7Click to set value"));
    }

    private ItemStack createSlotItemDisplay(String label, PreviewConfig.SlotItem item) {
        if (item == null) {
            return ItemBuilder.create("BARRIER", "&e&l" + label + ": &cnone",
                    List.of("&7Click to configure"));
        }
        return ItemBuilder.create(item.getMaterial() != null ? item.getMaterial() : "BARRIER",
                "&e&l" + label + ": &fslot " + item.getSlot(),
                List.of("&7Material: &f" + item.getMaterial(),
                        "&7Name: &f" + item.getName(),
                        "&7Click to reconfigure"));
    }

    private ItemStack createSoundItem(String label, List<String> sounds) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Click to set sounds");
        lore.add("&7Right-click to clear");
        lore.add("");
        if (sounds != null) for (String s : sounds) lore.add("&f" + s);
        else lore.add("&cnone");
        return ItemBuilder.create("NOTE_BLOCK", "&e&l" + label, lore);
    }

    private void setEditorTag(ItemStack item, String key, String value) {
        ItemMeta meta = item.getItemMeta();
        setEditorTagMeta(meta, key, value);
        item.setItemMeta(meta);
    }

    private void setEditorTagMeta(ItemMeta meta, String key, String value) {
        meta.getPersistentDataContainer().set(
                org.bukkit.NamespacedKey.fromString("moderncrates:" + key),
                org.bukkit.persistence.PersistentDataType.STRING,
                value
        );
    }

    private String getEditorTag(int slot, String key) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(
                org.bukkit.NamespacedKey.fromString("moderncrates:" + key),
                org.bukkit.persistence.PersistentDataType.STRING
        );
    }

    private RewardDisplay ensureDisplay(Reward reward) {
        if (reward.getDisplay() == null) {
            RewardDisplay d = new RewardDisplay();
            d.setMaterial("STONE");
            reward.setDisplay(d);
        }
        return reward.getDisplay();
    }

    private String orNone(String s) {
        return s != null && !s.isEmpty() ? s : "none";
    }

    private List<Integer> parseIntList(String input) {
        List<Integer> list = new ArrayList<>();
        for (String s : input.split("[,\\s]+")) {
            try { list.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    private PointerConfig parsePointerInput(String input) {
        String[] parts = input.split(",", 3);
        if (parts.length >= 3) {
            PointerConfig p = new PointerConfig();
            try { p.setSlot(Integer.parseInt(parts[0].trim())); } catch (NumberFormatException e) { p.setSlot(0); }
            p.setMaterial(parts[1].trim().toUpperCase());
            p.setName(parts[2].trim());
            return p;
        }
        return null;
    }

    private void adjustInt(java.util.function.IntSupplier getter, java.util.function.IntConsumer setter,
                           boolean rightClick, boolean shiftClick, int min, int max) {
        int delta = rightClick ? -1 : 1;
        if (shiftClick) delta *= 5;
        setter.accept(Math.max(min, Math.min(max, getter.getAsInt() + delta)));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public EditorMode getMode() { return mode; }
    public Crate getEditingCrate() { return editingCrate; }
}
