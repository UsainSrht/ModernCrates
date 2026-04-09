package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.HologramConfig;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class HologramEditorGui extends EditorGuiBase {

    private final Crate crate;

    public HologramEditorGui(Player player, ModernCratesPlugin plugin, Crate crate) {
        super(player, plugin);
        this.crate = crate;
    }

    @Override
    public void open() {
        HologramConfig hc = crate.getHologramConfig();

        inventory = Bukkit.createInventory(this, 36, TextUtil.parse("<dark_red><bold>Hologram: " + crate.getName()));
        fillBlack();

        if (hc == null) {
            inventory.setItem(13, ItemBuilder.create("BARRIER", "<red>No hologram config",
                    List.of("<gray>Click to create default")));
        } else {
            List<String> lineInfo = new ArrayList<>();
            lineInfo.add("<gray>Click to add line");
            lineInfo.add("<gray>Right-click to clear all");
            lineInfo.add("");
            if (hc.getLines() != null) {
                for (String l : hc.getLines()) lineInfo.add("<white>" + l);
            }
            inventory.setItem(11, ItemBuilder.create("WRITABLE_BOOK", "<yellow><bold>Lines", lineInfo));

            inventory.setItem(13, createNumberItemDouble("COMPASS", "Offset X", hc.getOffsetX()));
            inventory.setItem(14, createNumberItemDouble("COMPASS", "Offset Y", hc.getOffsetY()));
            inventory.setItem(15, createNumberItemDouble("COMPASS", "Offset Z", hc.getOffsetZ()));
        }

        inventory.setItem(27, ItemBuilder.create("ARROW", "<red><bold>Back", List.of("<gray>Return to crate editor")));
        inventory.setItem(31, ItemBuilder.create("LIME_WOOL", "<green><bold>Save", List.of("<gray>Save crate")));
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean rightClick = event.isRightClick();
        HologramConfig hc = crate.getHologramConfig();

        if (slot == 13 && hc == null) {
            hc = new HologramConfig();
            hc.setLines(new ArrayList<>(List.of("<gold><bold>" + crate.getName())));
            hc.setOffsetY(1.5);
            crate.setHologramConfig(hc);
            open();
            return;
        }
        if (hc == null) return;

        HologramConfig finalHc = hc;
        switch (slot) {
            case 11 -> {
                if (rightClick) {
                    finalHc.setLines(new ArrayList<>());
                    open();
                } else {
                    requestSignInput("Hologram line", input -> {
                        List<String> lines = finalHc.getLines() != null ? new ArrayList<>(finalHc.getLines()) : new ArrayList<>();
                        lines.add(input);
                        finalHc.setLines(lines);
                        open();
                    });
                }
            }
            case 13 -> requestSignInput("Offset X", input -> {
                try { finalHc.setOffsetX(Double.parseDouble(input)); } catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("<red>Invalid number")); }
                open();
            });
            case 14 -> requestSignInput("Offset Y", input -> {
                try { finalHc.setOffsetY(Double.parseDouble(input)); } catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("<red>Invalid number")); }
                open();
            });
            case 15 -> requestSignInput("Offset Z", input -> {
                try { finalHc.setOffsetZ(Double.parseDouble(input)); } catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("<red>Invalid number")); }
                open();
            });
            case 27 -> new CrateEditorGui(player, plugin, crate).open();
            case 31 -> { saveCrate(crate); player.sendMessage(TextUtil.parse("<green>Crate saved!")); }
        }
    }
}
