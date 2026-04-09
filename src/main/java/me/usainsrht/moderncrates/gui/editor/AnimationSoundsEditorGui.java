package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AnimationSoundsEditorGui extends EditorGuiBase {

    private final Animation animation;

    public AnimationSoundsEditorGui(Player player, ModernCratesPlugin plugin, Animation animation) {
        super(player, plugin);
        this.animation = animation;
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(this, 36, TextUtil.parse("<dark_red><bold>Sounds: " + animation.getId()));
        fillBlack();

        inventory.setItem(10, createSoundItem("Tick Sound", animation.getTickSounds()));
        inventory.setItem(12, createSoundItem("Reward Sound", animation.getRewardSounds()));
        inventory.setItem(14, createSoundItem("Shuffle Sound", animation.getShuffleSounds()));
        inventory.setItem(16, createSoundItem("Hide Sound", animation.getHideSounds()));
        inventory.setItem(19, createSoundItem("Reveal Sound", animation.getRevealSounds()));
        inventory.setItem(21, createSoundItem("Rise Sound", animation.getRiseSounds()));
        inventory.setItem(23, createSoundItem("Settle Sound", animation.getSettleSounds()));
        inventory.setItem(25, createSoundItem("Win Sound", animation.getWinSounds()));
        inventory.setItem(28, createSoundItem("Lose Sound", animation.getLoseSounds()));

        inventory.setItem(27, ItemBuilder.create("ARROW", "<red><bold>Back",
                List.of("<gray>Return to animation editor")));
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean rightClick = event.isRightClick();

        BiConsumer<String, Consumer<List<String>>> handle = (prompt, setter) -> {
            if (rightClick) {
                setter.accept(null);
                open();
            } else {
                requestSignInput(prompt, input -> {
                    setter.accept(List.of(input.split(",\\s*")));
                    open();
                });
            }
        };

        switch (slot) {
            case 10 -> handle.accept("Tick sounds", animation::setTickSounds);
            case 12 -> handle.accept("Reward sounds", animation::setRewardSounds);
            case 14 -> handle.accept("Shuffle sounds", animation::setShuffleSounds);
            case 16 -> handle.accept("Hide sounds", animation::setHideSounds);
            case 19 -> handle.accept("Reveal sounds", animation::setRevealSounds);
            case 21 -> handle.accept("Rise sounds", animation::setRiseSounds);
            case 23 -> handle.accept("Settle sounds", animation::setSettleSounds);
            case 25 -> handle.accept("Win sounds", animation::setWinSounds);
            case 28 -> handle.accept("Lose sounds", animation::setLoseSounds);
            case 27 -> AnimationListGui.openAnimationEditor(player, plugin, animation);
        }
    }
}
