package me.usainsrht.moderncrates.gui.editor.animation;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.animation.Animation;
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
 * Editor GUI for ItemRise-type animations (physical crate item rising).
 * Settings: type, riseHeight, riseTicks, cycleTicks, particleType/count/radius/speed,
 * blockOpenDelayTicks, riseStartDelayTicks, settleDisplayTicks, sounds.
 * No GUI settings (no inventory GUI for this type).
 */
public class ItemRiseAnimationEditorGui extends EditorGuiBase {

    private final Animation animation;

    public ItemRiseAnimationEditorGui(Player player, ModernCratesPlugin plugin, Animation animation) {
        super(player, plugin);
        this.animation = animation;
    }

    @Override
    public void open() {
        inventory = Bukkit.createInventory(this, 54,
                TextUtil.parse("<dark_red><bold>Item Rise: " + animation.getId()));
        fillBlack();

        // Row 0: Type selector
        inventory.setItem(10, createTypeSelector());

        // Row 0-1: Rise settings
        inventory.setItem(12, ItemBuilder.create("FEATHER",
                "<yellow><bold>Rise Height: <white>" + String.format("%.1f", animation.getRiseHeight()),
                List.of("<gray>Click to set")));
        inventory.setItem(14, createNumberItem("REPEATER", "Rise Ticks", animation.getRiseTicks()));
        inventory.setItem(16, createNumberItem("COMPARATOR", "Cycle Ticks", animation.getCycleTicks()));

        // Row 1: Particle settings
        inventory.setItem(19, ItemBuilder.create("BLAZE_POWDER",
                "<yellow><bold>Particle: <white>" + orNone(animation.getParticleType()),
                List.of("<gray>Click to set")));
        inventory.setItem(20, createNumberItem("REDSTONE", "Particle Count", animation.getParticleCount()));
        inventory.setItem(21, ItemBuilder.create("COMPASS",
                "<yellow><bold>Spiral Radius: <white>" + String.format("%.2f", animation.getParticleSpiralRadius()),
                List.of("<gray>Click to set")));
        inventory.setItem(22, ItemBuilder.create("CLOCK",
                "<yellow><bold>Spiral Speed: <white>" + String.format("%.2f", animation.getParticleSpiralSpeed()),
                List.of("<gray>Click to set")));

        // Row 2: Delay settings
        inventory.setItem(28, createNumberItem("PISTON", "Block Open Delay", animation.getBlockOpenDelayTicks()));
        inventory.setItem(30, createNumberItem("STICKY_PISTON", "Rise Start Delay", animation.getRiseStartDelayTicks()));
        inventory.setItem(32, createNumberItem("DAYLIGHT_DETECTOR", "Settle Display", animation.getSettleDisplayTicks()));

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
            case 12 -> requestSignInput("Rise height", input -> {
                try { animation.setRiseHeight(Double.parseDouble(input)); } catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("<red>Invalid number")); }
                open();
            });
            case 14 -> { adjustInt(animation::getRiseTicks, animation::setRiseTicks, rightClick, shiftClick, 1, 999); open(); }
            case 16 -> { adjustInt(animation::getCycleTicks, animation::setCycleTicks, rightClick, shiftClick, 1, 100); open(); }
            case 19 -> requestSignInput("Particle type", input -> {
                animation.setParticleType(input.toUpperCase());
                open();
            });
            case 20 -> { adjustInt(animation::getParticleCount, animation::setParticleCount, rightClick, shiftClick, 1, 100); open(); }
            case 21 -> requestSignInput("Spiral radius", input -> {
                try { animation.setParticleSpiralRadius(Double.parseDouble(input)); } catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("<red>Invalid number")); }
                open();
            });
            case 22 -> requestSignInput("Spiral speed", input -> {
                try { animation.setParticleSpiralSpeed(Double.parseDouble(input)); } catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("<red>Invalid number")); }
                open();
            });
            case 28 -> { adjustInt(animation::getBlockOpenDelayTicks, animation::setBlockOpenDelayTicks, rightClick, shiftClick, 0, 200); open(); }
            case 30 -> { adjustInt(animation::getRiseStartDelayTicks, animation::setRiseStartDelayTicks, rightClick, shiftClick, 0, 200); open(); }
            case 32 -> { adjustInt(animation::getSettleDisplayTicks, animation::setSettleDisplayTicks, rightClick, shiftClick, 1, 999); open(); }
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
