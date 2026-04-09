package me.usainsrht.moderncrates.gui.editor.animation;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.GuiItemConfig;
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
 * Editor GUI for Scratchcard-type animations.
 * Settings: type, gui title, shuffling title, rows, fill, rewardSlots, fillerSlots,
 * rewardHideItem, shuffleAmount, shuffleTicks, rewardAmount, showRevealedItemsFor,
 * matchRequired, winTitle, loseTitle, sounds.
 */
public class ScratchcardAnimationEditorGui extends EditorGuiBase {

    private final Animation animation;

    public ScratchcardAnimationEditorGui(Player player, ModernCratesPlugin plugin, Animation animation) {
        super(player, plugin);
        this.animation = animation;
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(this, 54,
                TextUtil.parse("<dark_red><bold>Scratchcard: " + animation.getId()));
        fillBlack();

        // Row 0: Type, GUI Title, Shuffling Title, Rows, Fill
        inventory.setItem(10, createTypeSelector());
        inventory.setItem(11, ItemBuilder.create("NAME_TAG",
                "<yellow><bold>GUI Title: <white>" + animation.getGuiTitle(),
                List.of("<gray>Click to set")));
        inventory.setItem(12, ItemBuilder.create("NAME_TAG",
                "<yellow><bold>Shuffling Title: <white>" + orNone(animation.getGuiTitleShuffling()),
                List.of("<gray>Click to set")));
        inventory.setItem(14, createNumberItem("LADDER", "GUI Rows", animation.getGuiRows()));
        String fillMat = animation.getGuiFill() != null ? animation.getGuiFill().getMaterial() : "none";
        inventory.setItem(16, ItemBuilder.create(fillMat.equals("none") ? "BARRIER" : fillMat,
                "<yellow><bold>GUI Fill: <white>" + fillMat, List.of("<gray>Click to set material")));

        // Row 1: Scratchcard settings
        inventory.setItem(19, createNumberItem("PISTON", "Shuffle Amount", animation.getShuffleAmount()));
        inventory.setItem(20, createNumberItem("STICKY_PISTON", "Shuffle Ticks", animation.getShuffleTicks()));
        inventory.setItem(21, createNumberItem("DIAMOND", "Reward Amount", animation.getRewardAmount()));
        inventory.setItem(22, createNumberItem("CLOCK", "Show Revealed For", animation.getShowRevealedItemsFor()));
        inventory.setItem(23, createNumberItem("TARGET", "Match Required", animation.getMatchRequired()));

        // Hide item
        String hideItemMat = animation.getRewardHideItem() != null ? animation.getRewardHideItem().getMaterial() : "none";
        inventory.setItem(25, ItemBuilder.create(hideItemMat.equals("none") ? "YELLOW_STAINED_GLASS_PANE" : hideItemMat,
                "<yellow><bold>Hide Item: <white>" + hideItemMat,
                List.of("<gray>Click to set material", "<gray>Right-click to set name")));

        // Row 2: Win/Lose titles, Slots
        inventory.setItem(28, ItemBuilder.create("LIME_WOOL",
                "<yellow><bold>Win Title: <white>" + orNone(animation.getWinTitle()),
                List.of("<gray>Click to set")));
        inventory.setItem(29, ItemBuilder.create("RED_WOOL",
                "<yellow><bold>Lose Title: <white>" + orNone(animation.getLoseTitle()),
                List.of("<gray>Click to set")));

        List<String> slotLore = new ArrayList<>();
        slotLore.add("<gray>Click to set");
        slotLore.add("<gray>Current: <white>" + (animation.getRewardSlots() != null ? animation.getRewardSlots().toString() : "[]"));
        inventory.setItem(31, ItemBuilder.create("CHEST_MINECART", "<yellow><bold>Reward Slots", slotLore));

        List<String> fillerLore = new ArrayList<>();
        fillerLore.add("<gray>Click to set");
        fillerLore.add("<gray>Current: <white>" + (animation.getFillerSlots() != null ? animation.getFillerSlots().toString() : "[]"));
        inventory.setItem(33, ItemBuilder.create("HOPPER_MINECART", "<yellow><bold>Filler Slots", fillerLore));

        // Sounds
        inventory.setItem(37, ItemBuilder.create("NOTE_BLOCK", "<yellow><bold>Sounds",
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
            case 11 -> requestSignInput("GUI title", input -> {
                animation.setGuiTitle(input);
                open();
            });
            case 12 -> requestSignInput("Shuffling title", input -> {
                animation.setGuiTitleShuffling(input);
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
            case 19 -> { adjustInt(animation::getShuffleAmount, animation::setShuffleAmount, rightClick, shiftClick, 1, 100); open(); }
            case 20 -> { adjustInt(animation::getShuffleTicks, animation::setShuffleTicks, rightClick, shiftClick, 1, 100); open(); }
            case 21 -> { adjustInt(animation::getRewardAmount, animation::setRewardAmount, rightClick, shiftClick, 1, 100); open(); }
            case 22 -> { adjustInt(animation::getShowRevealedItemsFor, animation::setShowRevealedItemsFor, rightClick, shiftClick, 1, 999); open(); }
            case 23 -> { adjustInt(animation::getMatchRequired, animation::setMatchRequired, rightClick, shiftClick, 1, 50); open(); }
            case 25 -> {
                if (rightClick) {
                    requestSignInput("Hide item name", input -> {
                        GuiItemConfig hi = animation.getRewardHideItem() != null ? animation.getRewardHideItem() : new GuiItemConfig();
                        hi.setName(input);
                        animation.setRewardHideItem(hi);
                        open();
                    });
                } else {
                    requestSignInput("Hide item mat", input -> {
                        GuiItemConfig hi = animation.getRewardHideItem() != null ? animation.getRewardHideItem() : new GuiItemConfig();
                        hi.setMaterial(input.toUpperCase());
                        animation.setRewardHideItem(hi);
                        open();
                    });
                }
            }
            case 28 -> requestSignInput("Win title", input -> {
                animation.setWinTitle(input);
                open();
            });
            case 29 -> requestSignInput("Lose title", input -> {
                animation.setLoseTitle(input);
                open();
            });
            case 31 -> requestSignInput("Reward slots", input -> {
                animation.setRewardSlots(parseIntList(input));
                open();
            });
            case 33 -> requestSignInput("Filler slots", input -> {
                animation.setFillerSlots(parseIntList(input));
                open();
            });
            case 37 -> new AnimationSoundsEditorGui(player, plugin, animation).open();
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
