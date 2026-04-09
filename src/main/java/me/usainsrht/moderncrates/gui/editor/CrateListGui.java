package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateItemConfig;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class CrateListGui extends EditorGuiBase {

    public CrateListGui(Player player, ModernCratesPlugin plugin) {
        super(player, plugin);
    }

    @Override
    public void open() {
        var crates = new ArrayList<>(plugin.getCrateRegistry().values());
        int rows = Math.max(2, Math.min(6, (int) Math.ceil((crates.size() + 2) / 9.0) + 1));

        inventory = Bukkit.createInventory(this, rows * 9, TextUtil.parse("<dark_red><bold>Crates"));
        fillBlack();

        inventory.setItem(inventory.getSize() - 9, ItemBuilder.create("ARROW", "<red><bold>Back",
                List.of("<gray>Return to main menu")));
        inventory.setItem(inventory.getSize() - 1, ItemBuilder.create("EMERALD", "<green><bold>Create New Crate",
                List.of("<gray>Click to create a new crate")));

        for (int i = 0; i < crates.size() && i < inventory.getSize() - 9; i++) {
            Crate crate = crates.get(i);
            String mat = crate.getItemConfig() != null ? crate.getItemConfig().getMaterial() : "CHEST";
            ItemStack item = ItemBuilder.create(mat, "<gold><bold>" + crate.getName(), List.of(
                    "<gray>ID: <white>" + crate.getId(),
                    "<gray>Animation: <white>" + crate.getAnimationId(),
                    "<gray>Rewards: <white>" + crate.getRewards().size(),
                    "", "<yellow>Left-click to edit", "<red>Right-click to delete"
            ));
            setEditorTag(item, "editor_crate", crate.getId());
            inventory.setItem(i, item);
        }
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean rightClick = event.isRightClick();

        if (slot == inventory.getSize() - 9) {
            new MainMenuGui(player, plugin).open();
            return;
        }
        if (slot == inventory.getSize() - 1) {
            String newId = "new_crate_" + System.currentTimeMillis() % 10000;
            Crate crate = new Crate(newId);
            crate.setName("New Crate");
            crate.setAnimationId("csgo");
            crate.setRewards(new LinkedHashMap<>());
            CrateItemConfig itemCfg = new CrateItemConfig();
            itemCfg.setMaterial("CHEST");
            itemCfg.setName("<gold><bold>" + crate.getName());
            crate.setItemConfig(itemCfg);
            plugin.getCrateRegistry().put(newId, crate);
            saveCrate(crate);
            new CrateEditorGui(player, plugin, crate).open();
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
            open();
        } else {
            new CrateEditorGui(player, plugin, crate).open();
        }
    }
}
