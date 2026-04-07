package me.usainsrht.moderncrates.gui;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Player main menu GUI for viewing and opening crates.
 */
public class PlayerMenuGui implements ModernCratesGui {

    private final Player player;
    private final ModernCratesPlugin plugin;
    private Inventory inventory;

    public PlayerMenuGui(Player player, ModernCratesPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    public void open() {
        var crates = new ArrayList<>(plugin.getCrateRegistry().values());
        int rows = Math.max(1, (int) Math.ceil(crates.size() / 9.0) + 1);
        rows = Math.min(6, rows);

        inventory = Bukkit.createInventory(this, rows * 9, TextUtil.parse("<gold><bold>Crates Menu"));

        // Fill background
        ItemStack fill = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillMeta = fill.getItemMeta();
        fillMeta.displayName(TextUtil.parse(" "));
        fill.setItemMeta(fillMeta);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, fill);
        }

        // Place crate items
        for (int i = 0; i < crates.size() && i < inventory.getSize(); i++) {
            Crate crate = crates.get(i);
            var itemConfig = crate.getItemConfig();
            ItemStack item;
            if (itemConfig != null) {
                item = ItemBuilder.create(itemConfig.getMaterial(), itemConfig.getName(), itemConfig.getLore());
            } else {
                item = ItemBuilder.create("CHEST", "<gold>" + crate.getName(), null);
            }

            // Add virtual key count to lore
            int virtualKeys = plugin.getVirtualKeyManager().getKeys(player, crate.getId());
            ItemMeta meta = item.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(TextUtil.parse(""));
            lore.add(TextUtil.parse("<gray>Virtual Keys: <green>" + virtualKeys));
            lore.add(TextUtil.parse("<yellow>Click to preview"));
            lore.add(TextUtil.parse("<yellow>Shift-click to open"));
            meta.lore(lore);

            // Store crate ID in PDC
            meta.getPersistentDataContainer().set(
                    org.bukkit.NamespacedKey.fromString("moderncrates:menu_crate"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    crate.getId()
            );
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }

        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        String crateId = getCrateId(clicked);
        if (crateId == null) return;

        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) return;

        if (event.isShiftClick()) {
            player.closeInventory();
            plugin.openCrate(player, crate);
        } else {
            PreviewGui preview = new PreviewGui(player, crate);
            preview.open();
        }
    }

    private String getCrateId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        var key = org.bukkit.NamespacedKey.fromString("moderncrates:menu_crate");
        if (!pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) return null;
        return pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
