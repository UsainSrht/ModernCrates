package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.reward.RewardDisplay;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RewardsListGui extends EditorGuiBase {

    private final Crate crate;

    public RewardsListGui(Player player, ModernCratesPlugin plugin, Crate crate) {
        super(player, plugin);
        this.crate = crate;
    }

    @Override
    public void open() {
        var rewardsList = new ArrayList<>(crate.getRewards().entrySet());
        int rows = Math.max(2, Math.min(6, (int) Math.ceil((rewardsList.size() + 2) / 9.0) + 1));

        inventory = Bukkit.createInventory(this, rows * 9, TextUtil.parse("<dark_red><bold>Rewards: " + crate.getName()));
        fillBlack();

        inventory.setItem(inventory.getSize() - 9, ItemBuilder.create("ARROW", "<red><bold>Back",
                List.of("<gray>Return to crate editor")));
        inventory.setItem(inventory.getSize() - 1, ItemBuilder.create("EMERALD", "<green><bold>Add New Reward",
                List.of("<gray>Click to add a new reward")));

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
            lore.add(TextUtil.parse("<gray>ID: <white>" + entry.getKey()));
            lore.add(TextUtil.parse("<gray>Chance: <white>" + reward.getChance()));
            lore.add(TextUtil.parse("<gray>Commands: <white>" + (reward.hasCommands() ? reward.getCommands().size() : 0)));
            lore.add(TextUtil.parse("<gray>Items: <white>" + (reward.hasItems() ? reward.getItems().size() : 0)));
            lore.add(TextUtil.parse(""));
            lore.add(TextUtil.parse("<yellow>Left-click to edit"));
            lore.add(TextUtil.parse("<red>Right-click to delete"));
            meta.lore(lore);

            setEditorTagMeta(meta, "editor_reward", entry.getKey());
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean rightClick = event.isRightClick();

        if (slot == inventory.getSize() - 9) {
            new CrateEditorGui(player, plugin, crate).open();
            return;
        }
        if (slot == inventory.getSize() - 1) {
            String newId = "reward_" + System.currentTimeMillis() % 10000;
            Reward reward = new Reward(newId);
            reward.setChance(1.0);
            RewardDisplay display = new RewardDisplay();
            display.setMaterial("STONE");
            display.setName("<gray>New Reward");
            reward.setDisplay(display);
            crate.getRewards().put(newId, reward);
            open();
            return;
        }

        String rewardId = getEditorTag(slot, "editor_reward");
        if (rewardId == null) return;

        if (rightClick) {
            crate.getRewards().remove(rewardId);
            open();
        } else {
            new RewardEditorGui(player, plugin, crate, rewardId).open();
        }
    }
}
