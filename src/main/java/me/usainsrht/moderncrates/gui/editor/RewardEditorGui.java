package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.reward.RewardDisplay;
import me.usainsrht.moderncrates.api.reward.RewardItem;
import me.usainsrht.moderncrates.api.reward.requirement.RewardRequirements;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import me.usainsrht.yamlmessage.YamlMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RewardEditorGui extends EditorGuiBase {

    private static final int REWARD_ITEM_SLOT_START = 40;
    private static final int REWARD_ITEM_SLOT_COUNT = 5;

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

        inventory.setItem(4, ItemBuilder.fromDisplay(reward, crate));

        inventory.setItem(19, ItemBuilder.create("ITEM_FRAME",
                "<yellow><bold>Display Item",
                List.of("<gray>Place item on cursor and click",
                        "<gray>to copy name, lore, enchants, etc.",
                        "<gray>Current: <white>" + (reward.getDisplay() != null ? reward.getDisplay().getMaterial() : "none (uses reward item)"))));

        inventory.setItem(20, ItemBuilder.create("NAME_TAG",
                "<yellow><bold>Display Name: <white>" + (reward.getDisplay() != null && reward.getDisplay().getName() != null ? reward.getDisplay().getName() : "none"),
                List.of("<gray>Click to change")));

        List<String> dispLore = new ArrayList<>();
        dispLore.add("<gray>Click to add line");
        dispLore.add("<gray>Right-click to clear");
        dispLore.add("");
        if (reward.getDisplay() != null && reward.getDisplay().getLore() != null) {
            for (String l : reward.getDisplay().getLore()) dispLore.add("<white>" + l);
        }
        inventory.setItem(21, ItemBuilder.create("WRITABLE_BOOK", "<yellow><bold>Display Lore", dispLore));

        inventory.setItem(23, ItemBuilder.create("EXPERIENCE_BOTTLE",
                "<yellow><bold>Chance: <white>" + reward.getChance(),
                List.of("<gray>Left-click +1 | Right-click -1", "<gray>Shift for +/- 5")));

        List<String> reqLore = new ArrayList<>();
        reqLore.add("<gray>Left-click to add condition");
        reqLore.add("<gray>Right-click to clear");
        reqLore.add("<gray>Shift-click to toggle mode (AND/OR)");
        reqLore.add("");
        RewardRequirements reqs = reward.getRequirements();
        if (reqs != null && !reqs.isEmpty()) {
            reqLore.add("<yellow>Mode: <white>" + reqs.getMode().name());
            if (reqs.hasPermission()) {
                reqLore.add("<yellow>Permission: <white>" + reqs.getPermission());
            }
            if (!reqs.getRawConditions().isEmpty()) {
                reqLore.add("<yellow>Conditions (" + reqs.getRawConditions().size() + "):");
                for (String c : reqs.getRawConditions()) {
                    reqLore.add("<white>- " + c);
                }
            }
        } else if (reward.hasRequiredPermission()) {
            reqLore.add("<yellow>Permission: <white>" + reward.getRequiredPermission());
        } else {
            reqLore.add("<dark_gray>No requirements configured");
        }
        inventory.setItem(24, ItemBuilder.create("COMPARATOR", "<yellow><bold>Requirements", reqLore));

        List<String> cmdLore = new ArrayList<>();
        cmdLore.add("<gray>Click to add command");
        cmdLore.add("<gray>Right-click to clear");
        cmdLore.add("");
        if (reward.hasCommands()) for (String c : reward.getCommands()) cmdLore.add("<white>" + c);
        inventory.setItem(25, ItemBuilder.create("COMMAND_BLOCK", "<yellow><bold>Commands", cmdLore));

        String announceDesc = reward.getAnnounce() == null ? "<gray>default (crate setting)"
                : (reward.getAnnounce() ? "<green>true (always announce)" : "<red>false (exempt)");
        String announceMat = reward.getAnnounce() == null ? "GRAY_DYE" : (reward.getAnnounce() ? "LIME_DYE" : "RED_DYE");
        inventory.setItem(37, ItemBuilder.create(announceMat,
                "<yellow><bold>Announce: <white>" + announceDesc,
                List.of("<gray>Click to toggle (default -> true -> false)", "<gray>Right-click to reset to default")));

        String msgDesc = reward.getAnnouncementMessageRaw() != null ? reward.getAnnouncementMessageRaw() : "none (uses crate default)";
        inventory.setItem(38, ItemBuilder.create("WRITABLE_BOOK",
                "<yellow><bold>Announcement Message",
                List.of("<gray>Current: <white>" + msgDesc, "", "<gray>Click to set unique message", "<gray>Right-click to clear")));

        inventory.setItem(39, ItemBuilder.create("CHEST",
                "<yellow><bold>Reward Items",
                List.of("<gray>Place item on cursor and click to add",
                        "<gray>Right-click to clear all items",
                        "<gray>Count: <white>" + (reward.hasItems() ? reward.getItems().size() : 0))));

        populateRewardItemSlots(reward);

        inventory.setItem(45, ItemBuilder.create("ARROW", "<red><bold>Back", List.of("<gray>Return to rewards list")));
        player.openInventory(inventory);
    }

    private void populateRewardItemSlots(Reward reward) {
        if (!reward.hasItems()) return;

        List<Map.Entry<String, RewardItem>> items = new ArrayList<>(reward.getItems().entrySet());
        for (int i = 0; i < items.size() && i < REWARD_ITEM_SLOT_COUNT; i++) {
            Map.Entry<String, RewardItem> entry = items.get(i);
            ItemStack stack = ItemBuilder.fromRewardItem(entry.getValue());
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) continue;

            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(TextUtil.parse(""));
            lore.add(TextUtil.parse("<red>Right-click to remove"));
            meta.lore(lore);
            setEditorTagMeta(meta, "editor_reward_item", entry.getKey());
            stack.setItemMeta(meta);
            inventory.setItem(REWARD_ITEM_SLOT_START + i, stack);
        }
    }

    @Override
    protected void save() {
        saveCrate(crate);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean rightClick = event.isRightClick();
        boolean shiftClick = event.isShiftClick();

        Reward reward = crate.getRewards().get(rewardId);
        if (reward == null) return;

        switch (slot) {
            case 19 -> {
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    reward.setDisplay(ItemBuilder.toRewardDisplay(cursor));
                    player.setItemOnCursor(null);
                    save();
                    open();
                } else {
                    requestSignInput("Display material", input -> {
                        ensureDisplay(reward).setMaterial(input.toUpperCase());
                        open();
                    });
                }
            }
            case 20 -> requestSignInput("Display name", input -> {
                ensureDisplay(reward).setName(input);
                open();
            });
            case 21 -> {
                if (rightClick) {
                    if (reward.getDisplay() != null) reward.getDisplay().setLore(null);
                    save();
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
                if (crate.getAutoSortOnChance().isEnabled()) {
                    crate.sortRewards();
                }
                save();
                open();
            }
            case 24 -> {
                if (rightClick) {
                    reward.setRequirements(null);
                    save();
                    open();
                } else if (shiftClick) {
                    RewardRequirements req = reward.getRequirements();
                    if (req == null) {
                        req = new RewardRequirements();
                        reward.setRequirements(req);
                    }
                    req.setMode(req.getMode() == RewardRequirements.LogicalMode.AND
                            ? RewardRequirements.LogicalMode.OR
                            : RewardRequirements.LogicalMode.AND);
                    save();
                    open();
                } else {
                    requestSignInput("Condition e.g. %claims% < 50", input -> {
                        RewardRequirements req = reward.getRequirements();
                        if (req == null) {
                            req = new RewardRequirements();
                            reward.setRequirements(req);
                        }
                        try {
                            req.addCondition(input);
                            save();
                        } catch (Exception e) {
                            player.sendMessage(TextUtil.parse("<red>Invalid condition: " + e.getMessage()));
                        }
                        open();
                    });
                }
            }
            case 25 -> {
                if (rightClick) {
                    reward.setCommands(null);
                    save();
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
                } else {
                    if (reward.getAnnounce() == null) {
                        reward.setAnnounce(true);
                    } else if (Boolean.TRUE.equals(reward.getAnnounce())) {
                        reward.setAnnounce(false);
                    } else {
                        reward.setAnnounce(null);
                    }
                }
                save();
                open();
            }
            case 38 -> {
                if (rightClick) {
                    reward.setAnnouncementMessage((YamlMessage) null);
                    save();
                    open();
                } else {
                    requestSignInput("Announce msg", input -> {
                        reward.setAnnouncementMessage(input);
                        open();
                    });
                }
            }
            case 39 -> {
                ItemStack cursor = event.getCursor();
                if (rightClick) {
                    reward.setItems(null);
                    save();
                    open();
                } else if (cursor != null && cursor.getType() != Material.AIR) {
                    Map<String, RewardItem> items = reward.getItems() != null
                            ? new LinkedHashMap<>(reward.getItems())
                            : new LinkedHashMap<>();
                    String itemKey = "item_" + (System.currentTimeMillis() % 100000);
                    items.put(itemKey, ItemBuilder.toRewardItem(cursor));
                    reward.setItems(items);
                    player.setItemOnCursor(null);
                    save();
                    open();
                }
            }
            case 45 -> new RewardsListGui(player, plugin, crate).open();
            default -> {
                if (slot >= REWARD_ITEM_SLOT_START && slot < REWARD_ITEM_SLOT_START + REWARD_ITEM_SLOT_COUNT && rightClick) {
                    String itemKey = getEditorTag(slot, "editor_reward_item");
                    if (itemKey != null && reward.getItems() != null) {
                        reward.getItems().remove(itemKey);
                        if (reward.getItems().isEmpty()) {
                            reward.setItems(null);
                        }
                        save();
                        open();
                    }
                }
            }
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
