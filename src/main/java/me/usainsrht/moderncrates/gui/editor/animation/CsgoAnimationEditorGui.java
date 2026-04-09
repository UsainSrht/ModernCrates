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
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor GUI for CSGO-type animations (scrolling reel).
 * Settings: type, gui title, rows, fill, totalTicks, stayOpen, startTickRate,
 * tickRateModifier, rewardIndex, rewardSlots, fillerSlots, pointers, sounds.
 */
public class CsgoAnimationEditorGui extends EditorGuiBase {

    private final Animation animation;

    public CsgoAnimationEditorGui(Player player, ModernCratesPlugin plugin, Animation animation) {
        super(player, plugin);
        this.animation = animation;
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(this, 54,
                TextUtil.parse("<dark_red><bold>CSGO Animation: " + animation.getId()));
        fillBlack();

        // Row 0: Type selector, GUI Title, Rows, Fill
        inventory.setItem(10, createTypeSelector());
        inventory.setItem(12, ItemBuilder.create("NAME_TAG",
                "<yellow><bold>GUI Title: <white>" + animation.getGuiTitle(),
                List.of("<gray>Click to set")));
        inventory.setItem(14, createNumberItem("LADDER", "GUI Rows", animation.getGuiRows()));
        String fillMat = animation.getGuiFill() != null ? animation.getGuiFill().getMaterial() : "none";
        inventory.setItem(16, ItemBuilder.create(fillMat.equals("none") ? "BARRIER" : fillMat,
                "<yellow><bold>GUI Fill: <white>" + fillMat, List.of("<gray>Click to set material")));

        // Row 1: CSGO-specific timing settings
        inventory.setItem(19, createNumberItem("REPEATER", "Total Ticks", animation.getTotalTicks()));
        inventory.setItem(20, createNumberItem("COMPARATOR", "Stay Open After", animation.getStayOpenAfterRewardTicks()));
        inventory.setItem(21, createNumberItem("REDSTONE_TORCH", "Start Tick Rate", animation.getStartTickRate()));
        inventory.setItem(22, createNumberItem("LEVER", "Tick Rate Modifier", animation.getTickRateModifier()));
        inventory.setItem(23, createNumberItem("GOLDEN_APPLE", "Reward Index", animation.getRewardIndex()));

        // Row 2: Reward slots, Filler slots
        List<String> slotLore = new ArrayList<>();
        slotLore.add("<gray>Click to set");
        slotLore.add("<gray>Current: <white>" + (animation.getRewardSlots() != null ? animation.getRewardSlots().toString() : "[]"));
        inventory.setItem(28, ItemBuilder.create("CHEST_MINECART", "<yellow><bold>Reward Slots", slotLore));

        List<String> fillerLore = new ArrayList<>();
        fillerLore.add("<gray>Click to set");
        fillerLore.add("<gray>Current: <white>" + (animation.getFillerSlots() != null ? animation.getFillerSlots().toString() : "[]"));
        inventory.setItem(30, ItemBuilder.create("HOPPER_MINECART", "<yellow><bold>Filler Slots", fillerLore));

        // Row 2: Pointers
        inventory.setItem(32, ItemBuilder.create("ARROW", "<yellow><bold>Down Pointer",
                List.of("<gray>Slot: <white>" + (animation.getDownPointer() != null ? animation.getDownPointer().getSlot() : "none"),
                        "<gray>Click to configure")));
        inventory.setItem(33, ItemBuilder.create("ARROW", "<yellow><bold>Up Pointer",
                List.of("<gray>Slot: <white>" + (animation.getUpPointer() != null ? animation.getUpPointer().getSlot() : "none"),
                        "<gray>Click to configure")));

        // Row 3: Sounds
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
                    // Re-open with the correct editor for the new type
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
            case 23 -> { adjustInt(animation::getRewardIndex, animation::setRewardIndex, rightClick, shiftClick, 1, 100); open(); }
            case 28 -> requestSignInput("Reward slots", input -> {
                animation.setRewardSlots(parseIntList(input));
                open();
            });
            case 30 -> requestSignInput("Filler slots", input -> {
                animation.setFillerSlots(parseIntList(input));
                open();
            });
            case 32 -> requestSignInput("slot,mat,name", input -> {
                PointerConfig p = parsePointerInput(input);
                animation.setDownPointer(p);
                open();
            });
            case 33 -> requestSignInput("slot,mat,name", input -> {
                PointerConfig p = parsePointerInput(input);
                animation.setUpPointer(p);
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
