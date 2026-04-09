package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.GuiItemConfig;
import me.usainsrht.moderncrates.gui.editor.animation.*;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AnimationListGui extends EditorGuiBase {

    public AnimationListGui(Player player, ModernCratesPlugin plugin) {
        super(player, plugin);
    }

    @Override
    public void open() {
        var animations = new ArrayList<>(plugin.getAnimationRegistry().entrySet());
        int rows = Math.max(2, Math.min(6, (int) Math.ceil((animations.size() + 2) / 9.0) + 1));

        inventory = Bukkit.createInventory(this, rows * 9, TextUtil.parse("<dark_red><bold>Animations"));
        fillBlack();

        inventory.setItem(inventory.getSize() - 9, ItemBuilder.create("ARROW", "<red><bold>Back",
                List.of("<gray>Return to main menu")));
        inventory.setItem(inventory.getSize() - 1, ItemBuilder.create("EMERALD", "<green><bold>Create New Animation",
                List.of("<gray>Click to create a new animation")));

        for (int i = 0; i < animations.size() && i < inventory.getSize() - 9; i++) {
            var entry = animations.get(i);
            Animation anim = entry.getValue();
            ItemStack item = ItemBuilder.create("CLOCK", "<aqua><bold>" + entry.getKey(), List.of(
                    "<gray>Type: <white>" + anim.getTypeId(),
                    "<gray>Rows: <white>" + anim.getGuiRows(),
                    "<gray>Title: <white>" + anim.getGuiTitle(),
                    "", "<yellow>Left-click to edit", "<red>Right-click to delete"
            ));
            setEditorTag(item, "editor_anim", entry.getKey());
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
            String newId = "new_anim_" + System.currentTimeMillis() % 10000;
            Animation anim = new Animation(newId);
            anim.setTypeId("csgo");
            anim.setTotalTicks(45);
            anim.setStayOpenAfterRewardTicks(50);
            anim.setStartTickRate(1);
            anim.setTickRateModifier(20);
            anim.setGuiTitle("<red><bold><crate>");
            anim.setGuiRows(3);
            anim.setRewardIndex(5);
            anim.setRewardSlots(List.of(9, 10, 11, 12, 13, 14, 15, 16, 17));
            GuiItemConfig fill = new GuiItemConfig();
            fill.setMaterial("BLACK_STAINED_GLASS_PANE");
            fill.setName(" ");
            anim.setGuiFill(fill);
            plugin.getAnimationRegistry().put(newId, anim);
            saveAnimation(anim);
            openAnimationEditor(anim);
            return;
        }

        String animId = getEditorTag(slot, "editor_anim");
        if (animId == null) return;
        Animation anim = plugin.getAnimationRegistry().get(animId);
        if (anim == null) return;

        if (rightClick) {
            plugin.getAnimationRegistry().remove(animId);
            File f = new File(plugin.getDataFolder(), "animations/" + animId + ".yml");
            if (f.exists()) f.delete();
            open();
        } else {
            openAnimationEditor(anim);
        }
    }

    /**
     * Opens the correct animation editor based on the animation's typeId.
     */
    public static void openAnimationEditor(Player player, ModernCratesPlugin plugin, Animation anim) {
        String typeId = anim.getTypeId() != null ? anim.getTypeId().toLowerCase() : "";
        switch (typeId) {
            case "csgo" -> new CsgoAnimationEditorGui(player, plugin, anim).open();
            case "click" -> new ClickAnimationEditorGui(player, plugin, anim).open();
            case "scratchcard" -> new ScratchcardAnimationEditorGui(player, plugin, anim).open();
            case "slot" -> new SlotAnimationEditorGui(player, plugin, anim).open();
            case "item_rise" -> new ItemRiseAnimationEditorGui(player, plugin, anim).open();
            default -> new CsgoAnimationEditorGui(player, plugin, anim).open();
        }
    }

    private void openAnimationEditor(Animation anim) {
        openAnimationEditor(player, plugin, anim);
    }
}
