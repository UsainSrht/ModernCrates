package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.GuiItemConfig;
import me.usainsrht.moderncrates.api.animation.PointerConfig;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.PreviewConfig;
import me.usainsrht.moderncrates.gui.ModernCratesGui;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Abstract base for all editor GUI screens.
 * Provides common utilities like fill, number items, sign-based text input, and save helpers.
 */
public abstract class EditorGuiBase implements ModernCratesGui {

    protected final Player player;
    protected final ModernCratesPlugin plugin;
    protected Inventory inventory;

    protected EditorGuiBase(Player player, ModernCratesPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    /**
     * Render and open this GUI screen.
     */
    public abstract void open();

    @Override
    public abstract void handleClick(InventoryClickEvent event);

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // ========================
    // Sign-based Text Input
    // ========================

    /**
     * Opens a sign editor for the player to input text.
     * After submission, the callback is invoked on the main thread with the text from line 0.
     */
    protected void requestSignInput(String promptLine, Consumer<String> callback) {
        player.closeInventory();

        Location signLoc = player.getLocation().clone();
        signLoc.setY(Math.max(signLoc.getBlockY() - 5, signLoc.getWorld().getMinHeight()));

        Block block = signLoc.getBlock();
        Material previousType = block.getType();

        block.setType(Material.OAK_SIGN);
        Sign sign = (Sign) block.getState();
        sign.getSide(Side.FRONT).line(1, TextUtil.parse("<dark_gray>^^^^^^^^"));
        sign.getSide(Side.FRONT).line(2, TextUtil.parse("<dark_gray>" + promptLine));
        sign.update(true, false);

        player.openSign(sign, Side.FRONT);

        plugin.getSignInputManager().awaitInput(player, lines -> {
            block.setType(previousType);
            String text = lines[0] != null ? lines[0].trim() : "";
            if (text.isEmpty() || text.equalsIgnoreCase("cancel")) {
                open();
            } else {
                callback.accept(text);
            }
        });
    }

    /**
     * Opens a sign editor for multi-line text input.
     */
    protected void requestSignInputMultiLine(String promptLine, Consumer<String[]> callback) {
        player.closeInventory();

        Location signLoc = player.getLocation().clone();
        signLoc.setY(Math.max(signLoc.getBlockY() - 5, signLoc.getWorld().getMinHeight()));

        Block block = signLoc.getBlock();
        Material previousType = block.getType();

        block.setType(Material.OAK_SIGN);
        Sign sign = (Sign) block.getState();
        sign.getSide(Side.FRONT).line(3, TextUtil.parse("<dark_gray>" + promptLine));
        sign.update(true, false);

        player.openSign(sign, Side.FRONT);

        plugin.getSignInputManager().awaitInput(player, lines -> {
            block.setType(previousType);
            callback.accept(lines);
        });
    }

    // ========================
    // Common GUI elements
    // ========================

    protected void fillBlack() {
        ItemStack fill = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = fill.getItemMeta();
        meta.displayName(TextUtil.parse(" "));
        fill.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, fill);
        }
    }

    protected ItemStack createNumberItem(String material, String label, int value) {
        return ItemBuilder.create(material, "<yellow><bold>" + label + ": <white>" + value,
                List.of("<gray>Left-click +1 | Right-click -1", "<gray>Shift for +/- 5"));
    }

    protected ItemStack createNumberItemDouble(String material, String label, double value) {
        return ItemBuilder.create(material, "<yellow><bold>" + label + ": <white>" + String.format("%.2f", value),
                List.of("<gray>Click to set value"));
    }

    protected ItemStack createSlotItemDisplay(String label, PreviewConfig.SlotItem item) {
        if (item == null) {
            return ItemBuilder.create("BARRIER", "<yellow><bold>" + label + ": <red>none",
                    List.of("<gray>Click to configure"));
        }
        return ItemBuilder.create(item.getMaterial() != null ? item.getMaterial() : "BARRIER",
                "<yellow><bold>" + label + ": <white>slot " + item.getSlot(),
                List.of("<gray>Material: <white>" + item.getMaterial(),
                        "<gray>Name: <white>" + item.getName(),
                        "<gray>Click to reconfigure"));
    }

    protected ItemStack createSoundItem(String label, List<String> sounds) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Click to set sounds");
        lore.add("<gray>Right-click to clear");
        lore.add("");
        if (sounds != null) for (String s : sounds) lore.add("<white>" + s);
        else lore.add("<red>none");
        return ItemBuilder.create("NOTE_BLOCK", "<yellow><bold>" + label, lore);
    }

    // ========================
    // Tag helpers for PDC
    // ========================

    protected void setEditorTag(ItemStack item, String key, String value) {
        ItemMeta meta = item.getItemMeta();
        setEditorTagMeta(meta, key, value);
        item.setItemMeta(meta);
    }

    protected void setEditorTagMeta(ItemMeta meta, String key, String value) {
        meta.getPersistentDataContainer().set(
                org.bukkit.NamespacedKey.fromString("moderncrates:" + key),
                org.bukkit.persistence.PersistentDataType.STRING,
                value
        );
    }

    protected String getEditorTag(int slot, String key) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(
                org.bukkit.NamespacedKey.fromString("moderncrates:" + key),
                org.bukkit.persistence.PersistentDataType.STRING
        );
    }

    // ========================
    // Utility methods
    // ========================

    protected String orNone(String s) {
        return s != null && !s.isEmpty() ? s : "none";
    }

    protected List<Integer> parseIntList(String input) {
        List<Integer> list = new ArrayList<>();
        for (String s : input.split("[,\\s]+")) {
            try { list.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    protected PointerConfig parsePointerInput(String input) {
        String[] parts = input.split(",", 3);
        if (parts.length >= 3) {
            PointerConfig p = new PointerConfig();
            try { p.setSlot(Integer.parseInt(parts[0].trim())); } catch (NumberFormatException e) { p.setSlot(0); }
            p.setMaterial(parts[1].trim().toUpperCase());
            p.setName(parts[2].trim());
            return p;
        }
        return null;
    }

    protected void adjustInt(IntSupplier getter, IntConsumer setter,
                             boolean rightClick, boolean shiftClick, int min, int max) {
        int delta = rightClick ? -1 : 1;
        if (shiftClick) delta *= 5;
        setter.accept(Math.max(min, Math.min(max, getter.getAsInt() + delta)));
    }

    protected void saveCrate(Crate crate) {
        try {
            plugin.getCrateConfigParser().save(crate, new File(plugin.getDataFolder(), "crates"));
        } catch (IOException e) {
            player.sendMessage(TextUtil.parse("<red>Failed to save: " + e.getMessage()));
        }
    }

    protected void saveAnimation(Animation anim) {
        try {
            plugin.getAnimationConfigParser().save(anim, new File(plugin.getDataFolder(), "animations"));
        } catch (IOException e) {
            player.sendMessage(TextUtil.parse("<red>Failed to save: " + e.getMessage()));
        }
    }
}
