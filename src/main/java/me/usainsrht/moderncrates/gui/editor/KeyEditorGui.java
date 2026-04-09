package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateKeyConfig;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.*;

public class KeyEditorGui extends EditorGuiBase {

    private final Crate crate;

    public KeyEditorGui(Player player, ModernCratesPlugin plugin, Crate crate) {
        super(player, plugin);
        this.crate = crate;
    }

    @Override
    public void open() {
        CrateKeyConfig kc = crate.getKeyConfig();

        inventory = Bukkit.createInventory(this, 54, TextUtil.parse("<dark_red><bold>Key Settings: " + crate.getName()));
        fillBlack();

        if (kc == null) {
            inventory.setItem(22, ItemBuilder.create("BARRIER", "<red>No key config",
                    List.of("<gray>Click to create default key config")));
        } else {
            inventory.setItem(10, ItemBuilder.create(kc.isRequired() ? "LIME_DYE" : "GRAY_DYE",
                    "<yellow><bold>Required: <white>" + kc.isRequired(), List.of("<gray>Click to toggle")));
            inventory.setItem(12, ItemBuilder.create(kc.getMaterial() != null ? kc.getMaterial() : "TRIPWIRE_HOOK",
                    "<yellow><bold>Material: <white>" + (kc.getMaterial() != null ? kc.getMaterial() : "TRIPWIRE_HOOK"),
                    List.of("<gray>Click to change material")));
            inventory.setItem(14, createNumberItem("PAPER", "Count", kc.getCount()));
            inventory.setItem(16, ItemBuilder.create("NAME_TAG",
                    "<yellow><bold>Name: <white>" + (kc.getName() != null ? kc.getName() : "none"),
                    List.of("<gray>Click to set name")));

            // Lore
            List<String> loreLines = new ArrayList<>();
            loreLines.add("<gray>Click to add a line");
            loreLines.add("<gray>Right-click to clear lore");
            loreLines.add("");
            if (kc.getLore() != null) {
                for (String l : kc.getLore()) loreLines.add("<white>" + l);
            }
            inventory.setItem(28, ItemBuilder.create("WRITABLE_BOOK", "<yellow><bold>Lore", loreLines));

            // Enchantments
            List<String> enchLore = new ArrayList<>();
            enchLore.add("<gray>Click to add enchantment");
            enchLore.add("<gray>Right-click to clear");
            enchLore.add("");
            if (kc.getEnchantments() != null) {
                for (var e : kc.getEnchantments().entrySet()) {
                    enchLore.add("<white>" + e.getKey() + " <gray>= <white>" + e.getValue());
                }
            }
            inventory.setItem(30, ItemBuilder.create("ENCHANTING_TABLE", "<yellow><bold>Enchantments", enchLore));

            // Item Flags
            List<String> flagLore = new ArrayList<>();
            flagLore.add("<gray>Click to add flag");
            flagLore.add("<gray>Right-click to clear");
            flagLore.add("");
            if (kc.getItemFlags() != null) {
                for (String f : kc.getItemFlags()) flagLore.add("<white>" + f);
            }
            inventory.setItem(32, ItemBuilder.create("SHIELD", "<yellow><bold>Item Flags", flagLore));
        }

        inventory.setItem(45, ItemBuilder.create("ARROW", "<red><bold>Back", List.of("<gray>Return to crate editor")));
        inventory.setItem(49, ItemBuilder.create("LIME_WOOL", "<green><bold>Save", List.of("<gray>Save crate to file")));
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean rightClick = event.isRightClick();
        boolean shiftClick = event.isShiftClick();
        CrateKeyConfig kcRaw = crate.getKeyConfig();

        if (slot == 22 && kcRaw == null) {
            kcRaw = new CrateKeyConfig();
            kcRaw.setRequired(true);
            kcRaw.setMaterial("TRIPWIRE_HOOK");
            kcRaw.setCount(1);
            kcRaw.setName("<gold><bold>" + crate.getName() + " Key");
            crate.setKeyConfig(kcRaw);
            open();
            return;
        }
        if (kcRaw == null) return;
        final CrateKeyConfig kc = kcRaw;

        switch (slot) {
            case 10 -> { kc.setRequired(!kc.isRequired()); open(); }
            case 12 -> requestSignInput("Key material", input -> {
                Material m = Material.matchMaterial(input.toUpperCase());
                if (m != null) kc.setMaterial(input.toUpperCase());
                else player.sendMessage(TextUtil.parse("<red>Invalid material: " + input));
                open();
            });
            case 14 -> {
                int delta = rightClick ? -1 : 1;
                if (shiftClick) delta *= 5;
                kc.setCount(Math.max(1, kc.getCount() + delta));
                open();
            }
            case 16 -> requestSignInput("Key name", input -> {
                kc.setName(input);
                open();
            });
            case 28 -> {
                if (rightClick) {
                    kc.setLore(null);
                    open();
                } else {
                    requestSignInput("Lore line", input -> {
                        List<String> lore = kc.getLore() != null ? new ArrayList<>(kc.getLore()) : new ArrayList<>();
                        lore.add(input);
                        kc.setLore(lore);
                        open();
                    });
                }
            }
            case 30 -> {
                if (rightClick) {
                    kc.setEnchantments(null);
                    open();
                } else {
                    requestSignInput("ENCH LEVEL", input -> {
                        String[] parts = input.split("\\s+");
                        if (parts.length >= 2) {
                            Map<String, Integer> enc = kc.getEnchantments() != null ? new LinkedHashMap<>(kc.getEnchantments()) : new LinkedHashMap<>();
                            try {
                                enc.put(parts[0].toUpperCase(), Integer.parseInt(parts[1]));
                                kc.setEnchantments(enc);
                            } catch (NumberFormatException ignored) {
                                player.sendMessage(TextUtil.parse("<red>Invalid format. Use: ENCHANTMENT LEVEL"));
                            }
                        } else {
                            player.sendMessage(TextUtil.parse("<red>Use format: ENCHANTMENT LEVEL"));
                        }
                        open();
                    });
                }
            }
            case 32 -> {
                if (rightClick) {
                    kc.setItemFlags(null);
                    open();
                } else {
                    requestSignInput("Item flag", input -> {
                        List<String> flags = kc.getItemFlags() != null ? new ArrayList<>(kc.getItemFlags()) : new ArrayList<>();
                        flags.add(input.toUpperCase());
                        kc.setItemFlags(flags);
                        open();
                    });
                }
            }
            case 45 -> new CrateEditorGui(player, plugin, crate).open();
            case 49 -> { saveCrate(crate); player.sendMessage(TextUtil.parse("<green>Crate saved!")); }
        }
    }
}
