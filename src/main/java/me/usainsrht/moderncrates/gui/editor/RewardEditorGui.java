package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.reward.RewardDisplay;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class RewardEditorGui extends EditorGuiBase {

    private final Crate crate;
    private final String rewardId;

    public RewardEditorGui(Player player, ModernCratesPlugin plugin, Crate crate, String rewardId) {
        super(player, plugin);
        this.crate = crate;
        this.rewardId = rewardId;
    }

    @Override
    public void open() {
        Reward reward = crate.getRewards().get(rewardId);
        if (reward == null) return;

        inventory = Bukkit.createInventory(this, 54, TextUtil.parse("<dark_red><bold>Reward: " + rewardId));
        fillBlack();

        // Display preview
        if (reward.getDisplay() != null) {
            inventory.setItem(4, ItemBuilder.fromDisplay(reward.getDisplay()));
        } else {
            inventory.setItem(4, ItemBuilder.create("STONE", "<gray>No display item", null));
        }

        // Display material
        inventory.setItem(19, ItemBuilder.create("ITEM_FRAME",
                "<yellow><bold>Display Material: <white>" + (reward.getDisplay() != null ? reward.getDisplay().getMaterial() : "none"),
                List.of("<gray>Click to change")));

        // Display name
        inventory.setItem(20, ItemBuilder.create("NAME_TAG",
                "<yellow><bold>Display Name: <white>" + (reward.getDisplay() != null && reward.getDisplay().getName() != null ? reward.getDisplay().getName() : "none"),
                List.of("<gray>Click to change")));

        // Display lore
        List<String> dispLore = new ArrayList<>();
        dispLore.add("<gray>Click to add line");
        dispLore.add("<gray>Right-click to clear");
        dispLore.add("");
        if (reward.getDisplay() != null && reward.getDisplay().getLore() != null) {
            for (String l : reward.getDisplay().getLore()) dispLore.add("<white>" + l);
        }
        inventory.setItem(21, ItemBuilder.create("WRITABLE_BOOK", "<yellow><bold>Display Lore", dispLore));

        // Chance
        inventory.setItem(23, ItemBuilder.create("EXPERIENCE_BOTTLE",
                "<yellow><bold>Chance: <white>" + reward.getChance(),
                List.of("<gray>Left-click +1 | Right-click -1", "<gray>Shift for +/- 5")));

        // Commands
        List<String> cmdLore = new ArrayList<>();
        cmdLore.add("<gray>Click to add command");
        cmdLore.add("<gray>Right-click to clear");
        cmdLore.add("");
        if (reward.hasCommands()) for (String c : reward.getCommands()) cmdLore.add("<white>" + c);
        inventory.setItem(25, ItemBuilder.create("COMMAND_BLOCK", "<yellow><bold>Commands", cmdLore));

        // Announce
        inventory.setItem(37, ItemBuilder.create("GOAT_HORN",
                "<yellow><bold>Announce: <white>" + orNone(reward.getAnnounce()),
                List.of("<gray>Click to set", "<gray>Right-click to clear")));

        // Items
        inventory.setItem(39, ItemBuilder.create("CHEST",
                "<yellow><bold>Items: <white>" + (reward.hasItems() ? reward.getItems().size() : 0),
                List.of("<gray>Reward items are edited via YAML")));

        inventory.setItem(45, ItemBuilder.create("ARROW", "<red><bold>Back", List.of("<gray>Return to rewards list")));
        inventory.setItem(49, ItemBuilder.create("LIME_WOOL", "<green><bold>Save", List.of("<gray>Save crate")));
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean rightClick = event.isRightClick();
        boolean shiftClick = event.isShiftClick();

        Reward reward = crate.getRewards().get(rewardId);
        if (reward == null) return;

        switch (slot) {
            case 19 -> requestSignInput("Display material", input -> {
                ensureDisplay(reward).setMaterial(input.toUpperCase());
                open();
            });
            case 20 -> requestSignInput("Display name", input -> {
                ensureDisplay(reward).setName(input);
                open();
            });
            case 21 -> {
                if (rightClick) {
                    if (reward.getDisplay() != null) reward.getDisplay().setLore(null);
                    open();
                } else {
                    requestSignInput("Lore line", input -> {
                        RewardDisplay d = ensureDisplay(reward);
                        List<String> lore = d.getLore() != null ? new ArrayList<>(d.getLore()) : new ArrayList<>();
                        lore.add(input);
                        d.setLore(lore);
                        open();
                    });
                }
            }
            case 23 -> {
                double delta = rightClick ? -1 : 1;
                if (shiftClick) delta *= 5;
                reward.setChance(Math.max(0.01, reward.getChance() + delta));
                open();
            }
            case 25 -> {
                if (rightClick) {
                    reward.setCommands(null);
                    open();
                } else {
                    requestSignInput("Command", input -> {
                        List<String> cmds = reward.getCommands() != null ? new ArrayList<>(reward.getCommands()) : new ArrayList<>();
                        cmds.add(input);
                        reward.setCommands(cmds);
                        open();
                    });
                }
            }
            case 37 -> {
                if (rightClick) {
                    reward.setAnnounce(null);
                    open();
                } else {
                    requestSignInput("Announce msg", input -> {
                        reward.setAnnounce(input);
                        open();
                    });
                }
            }
            case 45 -> new RewardsListGui(player, plugin, crate).open();
            case 49 -> { saveCrate(crate); player.sendMessage(TextUtil.parse("<green>Crate saved!")); }
        }
    }

    private RewardDisplay ensureDisplay(Reward reward) {
        if (reward.getDisplay() == null) {
            RewardDisplay d = new RewardDisplay();
            d.setMaterial("STONE");
            reward.setDisplay(d);
        }
        return reward.getDisplay();
    }
}
