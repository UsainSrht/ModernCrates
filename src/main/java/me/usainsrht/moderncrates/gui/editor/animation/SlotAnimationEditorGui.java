package me.usainsrht.moderncrates.gui.editor.animation;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.GuiItemConfig;
import me.usainsrht.moderncrates.api.animation.PointerConfig;
import me.usainsrht.moderncrates.gui.editor.AnimationListGui;
import me.usainsrht.moderncrates.gui.editor.AnimationSoundsEditorGui;
import me.usainsrht.moderncrates.gui.editor.EditorGuiBase;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor GUI for Slot-type animations (multi-column slot machine).
 * Settings: type, gui title, rows, fill, totalTicks, stayOpen, startTickRate,
 * tickRateModifier, slotColumns, rewardWinnerColumns, rewardWinnerIndex,
 * columnStopDelayTicks, matchChance, pointers, winTitle, loseTitle,
 * endOfAnimationItem/Slots, fillerSlots, fillerItems, sounds.
 */
public class SlotAnimationEditorGui extends EditorGuiBase {

    private final Animation animation;

    public SlotAnimationEditorGui(Player player, ModernCratesPlugin plugin, Animation animation) {
        super(player, plugin);
        this.animation = animation;
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(this, 54,
                TextUtil.parse("<dark_red><bold>Slot Animation: " + animation.getId()));
        fillBlack();

        // Row 0: Type, GUI Title, Rows, Fill
        inventory.setItem(10, createTypeSelector());
        inventory.setItem(12, ItemBuilder.create("NAME_TAG",
                "<yellow><bold>GUI Title: <white>" + animation.getGuiTitle(),
                List.of("<gray>Click to set")));
        inventory.setItem(14, createNumberItem("LADDER", "GUI Rows", animation.getGuiRows()));
        String fillMat = animation.getGuiFill() != null ? animation.getGuiFill().getMaterial() : "none";
        inventory.setItem(16, ItemBuilder.create(fillMat.equals("none") ? "BARRIER" : fillMat,
                "<yellow><bold>GUI Fill: <white>" + fillMat, List.of("<gray>Click to set material")));

        // Row 1: Slot-specific timing
        inventory.setItem(19, createNumberItem("REPEATER", "Total Ticks", animation.getTotalTicks()));
        inventory.setItem(20, createNumberItem("COMPARATOR", "Stay Open After", animation.getStayOpenAfterRewardTicks()));
        inventory.setItem(21, createNumberItem("REDSTONE_TORCH", "Start Tick Rate", animation.getStartTickRate()));
        inventory.setItem(22, createNumberItem("LEVER", "Tick Rate Modifier", animation.getTickRateModifier()));
        inventory.setItem(23, createNumberItem("HOPPER", "Column Stop Delay", animation.getColumnStopDelayTicks()));
        inventory.setItem(24, createNumberItem("GOLDEN_APPLE", "Winner Index", animation.getRewardWinnerIndex()));

        // Match chance
        inventory.setItem(25, ItemBuilder.create("EXPERIENCE_BOTTLE",
                "<yellow><bold>Match Chance: <white>" + String.format("%.1f", animation.getMatchChance()) + "%",
                List.of("<gray>Click to set value")));

        // Row 2: Win/Lose, Slots, Pointers
        inventory.setItem(28, ItemBuilder.create("LIME_WOOL",
                "<yellow><bold>Win Title: <white>" + orNone(animation.getWinTitle()),
                List.of("<gray>Click to set")));
        inventory.setItem(29, ItemBuilder.create("RED_WOOL",
                "<yellow><bold>Lose Title: <white>" + orNone(animation.getLoseTitle()),
                List.of("<gray>Click to set")));

        // Slot columns display
        List<String> colLore = new ArrayList<>();
        colLore.add("<gray>Edited via YAML");
        if (animation.getSlotColumns() != null) {
            for (var entry : animation.getSlotColumns().entrySet()) {
                colLore.add("<white>" + entry.getKey() + ": " + entry.getValue());
            }
        }
        inventory.setItem(31, ItemBuilder.create("IRON_BARS", "<yellow><bold>Slot Columns", colLore));

        List<String> fillerLore = new ArrayList<>();
        fillerLore.add("<gray>Click to set");
        fillerLore.add("<gray>Current: <white>" + (animation.getFillerSlots() != null ? animation.getFillerSlots().toString() : "[]"));
        inventory.setItem(33, ItemBuilder.create("HOPPER_MINECART", "<yellow><bold>Filler Slots", fillerLore));

        // Pointers
        inventory.setItem(34, ItemBuilder.create("ARROW", "<yellow><bold>Down Pointer",
                List.of("<gray>Slot: <white>" + (animation.getDownPointer() != null ? animation.getDownPointer().getSlot() : "none"),
                        "<gray>Click to configure")));
        inventory.setItem(35, ItemBuilder.create("ARROW", "<yellow><bold>Up Pointer",
                List.of("<gray>Slot: <white>" + (animation.getUpPointer() != null ? animation.getUpPointer().getSlot() : "none"),
                        "<gray>Click to configure")));

        // Row 3: End of animation, Sounds
        String endMat = animation.getEndOfAnimationItem() != null ? animation.getEndOfAnimationItem().getMaterial() : "none";
        inventory.setItem(37, ItemBuilder.create(endMat.equals("none") ? "FIREWORK_ROCKET" : endMat,
                "<yellow><bold>End Animation Item: <white>" + endMat,
                List.of("<gray>Click to set material", "<gray>Right-click to set name")));

        List<String> endSlotLore = new ArrayList<>();
        endSlotLore.add("<gray>Click to set");
        endSlotLore.add("<gray>Current: <white>" + (animation.getEndOfAnimationSlots() != null ? animation.getEndOfAnimationSlots().toString() : "[]"));
        inventory.setItem(38, ItemBuilder.create("CHEST_MINECART", "<yellow><bold>End Anim Slots", endSlotLore));

        inventory.setItem(40, ItemBuilder.create("NOTE_BLOCK", "<yellow><bold>Sounds",
                List.of("<gray>Click to edit all sounds")));

        // Bottom
        inventory.setItem(45, ItemBuilder.create("ARROW", "<red><bold>Back",
                List.of("<gray>Return to animation list")));
        inventory.setItem(49, ItemBuilder.create("LIME_WOOL", "<green><bold>Save",
                List.of("<gray>Save animation to file")));
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean rightClick = event.isRightClick();
        boolean shiftClick = event.isShiftClick();

        switch (slot) {
            case 10 -> {
                List<String> types = new ArrayList<>(plugin.getAnimationTypeRegistry().keySet());
                if (!types.isEmpty()) {
                    int idx = types.indexOf(animation.getTypeId());
                    String newType = types.get((idx + 1) % types.size());
                    animation.setTypeId(newType);
                    AnimationListGui.openAnimationEditor(player, plugin, animation);
                }
            }
            case 12 -> requestSignInput("GUI title", input -> {
                animation.setGuiTitle(input);
                open();
            });
            case 14 -> {
                adjustInt(animation::getGuiRows, animation::setGuiRows, rightClick, shiftClick, 1, 6);
                open();
            }
            case 16 -> requestSignInput("Fill material", input -> {
                GuiItemConfig fill = animation.getGuiFill() != null ? animation.getGuiFill() : new GuiItemConfig();
                fill.setMaterial(input.toUpperCase());
                if (fill.getName() == null) fill.setName(" ");
                animation.setGuiFill(fill);
                open();
            });
            case 19 -> { adjustInt(animation::getTotalTicks, animation::setTotalTicks, rightClick, shiftClick, 1, 999); open(); }
            case 20 -> { adjustInt(animation::getStayOpenAfterRewardTicks, animation::setStayOpenAfterRewardTicks, rightClick, shiftClick, 0, 999); open(); }
            case 21 -> { adjustInt(animation::getStartTickRate, animation::setStartTickRate, rightClick, shiftClick, 1, 100); open(); }
            case 22 -> { adjustInt(animation::getTickRateModifier, animation::setTickRateModifier, rightClick, shiftClick, 0, 100); open(); }
            case 23 -> { adjustInt(animation::getColumnStopDelayTicks, animation::setColumnStopDelayTicks, rightClick, shiftClick, 1, 100); open(); }
            case 24 -> { adjustInt(animation::getRewardWinnerIndex, animation::setRewardWinnerIndex, rightClick, shiftClick, 0, 10); open(); }
            case 25 -> requestSignInput("Match chance %", input -> {
                try {
                    animation.setMatchChance(Math.max(0.0, Math.min(100.0, Double.parseDouble(input))));
                } catch (NumberFormatException e) {
                    player.sendMessage(TextUtil.parse("<red>Invalid number"));
                }
                open();
            });
            case 28 -> requestSignInput("Win title", input -> {
                animation.setWinTitle(input);
                open();
            });
            case 29 -> requestSignInput("Lose title", input -> {
                animation.setLoseTitle(input);
                open();
            });
            case 33 -> requestSignInput("Filler slots", input -> {
                animation.setFillerSlots(parseIntList(input));
                open();
            });
            case 34 -> requestSignInput("slot,mat,name", input -> {
                PointerConfig p = parsePointerInput(input);
                animation.setDownPointer(p);
                open();
            });
            case 35 -> requestSignInput("slot,mat,name", input -> {
                PointerConfig p = parsePointerInput(input);
                animation.setUpPointer(p);
                open();
            });
            case 37 -> {
                if (rightClick) {
                    requestSignInput("End item name", input -> {
                        GuiItemConfig ei = animation.getEndOfAnimationItem() != null ? animation.getEndOfAnimationItem() : new GuiItemConfig();
                        ei.setName(input);
                        animation.setEndOfAnimationItem(ei);
                        open();
                    });
                } else {
                    requestSignInput("End item mat", input -> {
                        GuiItemConfig ei = animation.getEndOfAnimationItem() != null ? animation.getEndOfAnimationItem() : new GuiItemConfig();
                        ei.setMaterial(input.toUpperCase());
                        animation.setEndOfAnimationItem(ei);
                        open();
                    });
                }
            }
            case 38 -> requestSignInput("End anim slots", input -> {
                animation.setEndOfAnimationSlots(parseIntList(input));
                open();
            });
            case 40 -> new AnimationSoundsEditorGui(player, plugin, animation).open();
            case 45 -> new AnimationListGui(player, plugin).open();
            case 49 -> { saveAnimation(animation); player.sendMessage(TextUtil.parse("<green>Animation saved!")); }
        }
    }

    private ItemStack createTypeSelector() {
        List<String> typeLore = new ArrayList<>();
        typeLore.add("<gray>Click to cycle type");
        for (String typeId : plugin.getAnimationTypeRegistry().keySet()) {
            typeLore.add(typeId.equals(animation.getTypeId()) ? "<green>> " + typeId : "<gray>  " + typeId);
        }
        return ItemBuilder.create("REDSTONE", "<yellow><bold>Type: <white>" + animation.getTypeId(), typeLore);
    }
}
