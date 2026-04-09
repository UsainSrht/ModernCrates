package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class MainMenuGui extends EditorGuiBase {

    public MainMenuGui(Player player, ModernCratesPlugin plugin) {
        super(player, plugin);
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(this, 27, TextUtil.parse("<dark_red><bold>ModernCrates Editor"));
        fillBlack();

        inventory.setItem(11, ItemBuilder.create("CHEST", "<gold><bold>Crates",
                List.of("<gray>Manage all crates", "<gray>" + plugin.getCrateRegistry().size() + " loaded")));
        inventory.setItem(15, ItemBuilder.create("CLOCK", "<aqua><bold>Animations",
                List.of("<gray>Manage all animations", "<gray>" + plugin.getAnimationRegistry().size() + " loaded")));

        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 11) {
            new CrateListGui(player, plugin).open();
        } else if (slot == 15) {
            new AnimationListGui(player, plugin).open();
        }
    }
}
