package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateItemConfig;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class ItemEditorGui extends EditorGuiBase {

    private final Crate crate;

    public ItemEditorGui(Player player, ModernCratesPlugin plugin, Crate crate) {
        super(player, plugin);
        this.crate = crate;
    }

    @Override
    public void open() {
        CrateItemConfig ic = crate.getItemConfig();

        inventory = Bukkit.createInventory(this, 36, TextUtil.parse("<dark_red><bold>Item Display: " + crate.getName()));
        fillBlack();

        if (ic == null) {
            inventory.setItem(13, ItemBuilder.create("BARRIER", "<red>No item config",
                    List.of("<gray>Click to create default")));
        } else {
            inventory.setItem(11, ItemBuilder.create(ic.getMaterial() != null ? ic.getMaterial() : "CHEST",
                    "<yellow><bold>Material: <white>" + (ic.getMaterial() != null ? ic.getMaterial() : "CHEST"),
                    List.of("<gray>Click to change")));
            inventory.setItem(13, ItemBuilder.create("NAME_TAG",
                    "<yellow><bold>Name: <white>" + (ic.getName() != null ? ic.getName() : "none"),
                    List.of("<gray>Click to set")));

            List<String> loreLore = new ArrayList<>();
            loreLore.add("<gray>Click to add line");
            loreLore.add("<gray>Right-click to clear");
            loreLore.add("");
            if (ic.getLore() != null) for (String l : ic.getLore()) loreLore.add("<white>" + l);
            inventory.setItem(15, ItemBuilder.create("WRITABLE_BOOK", "<yellow><bold>Lore", loreLore));
        }

        inventory.setItem(27, ItemBuilder.create("ARROW", "<red><bold>Back", List.of("<gray>Return to crate editor")));
        inventory.setItem(31, ItemBuilder.create("LIME_WOOL", "<green><bold>Save", List.of("<gray>Save crate")));
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean rightClick = event.isRightClick();
        CrateItemConfig ic = crate.getItemConfig();

        if (slot == 13 && ic == null) {
            ic = new CrateItemConfig();
            ic.setMaterial("CHEST");
            ic.setName("<gold><bold>" + crate.getName());
            crate.setItemConfig(ic);
            open();
            return;
        }
        if (ic == null) return;

        CrateItemConfig finalIc = ic;
        switch (slot) {
            case 11 -> requestSignInput("Item material", input -> {
                Material m = Material.matchMaterial(input.toUpperCase());
                if (m != null) finalIc.setMaterial(input.toUpperCase());
                else player.sendMessage(TextUtil.parse("<red>Invalid material: " + input));
                open();
            });
            case 13 -> requestSignInput("Display name", input -> {
                finalIc.setName(input);
                open();
            });
            case 15 -> {
                if (rightClick) {
                    finalIc.setLore(null);
                    open();
                } else {
                    requestSignInput("Lore line", input -> {
                        List<String> lore = finalIc.getLore() != null ? new ArrayList<>(finalIc.getLore()) : new ArrayList<>();
                        lore.add(input);
                        finalIc.setLore(lore);
                        open();
                    });
                }
            }
            case 27 -> new CrateEditorGui(player, plugin, crate).open();
            case 31 -> { saveCrate(crate); player.sendMessage(TextUtil.parse("<green>Crate saved!")); }
        }
    }
}
