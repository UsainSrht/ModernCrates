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

    private static final List<String> BILLBOARD_OPTIONS = List.of("CENTER", "FIXED", "VERTICAL", "HORIZONTAL");

    private final Crate crate;

    public HologramEditorGui(Player player, ModernCratesPlugin plugin, Crate crate) {
        super(player, plugin);
        this.crate = crate;
    }

    @Override
    public void open() {
        HologramConfig hc = crate.getHologramConfig();

        inventory = Bukkit.createInventory(this, 54, TextUtil.parse("<dark_red><bold>Hologram: " + crate.getName()));
        fillBlack();

        if (hc == null) {
            inventory.setItem(22, ItemBuilder.create("BARRIER", "<red>No hologram config",
                    List.of("<gray>Click to create default")));
        } else {
            // Lines editor
            List<String> lineInfo = new ArrayList<>();
            lineInfo.add("<gray>Left-click: add line | Right-click: remove last line");
            lineInfo.add("");
            if (hc.getLines() != null && !hc.getLines().isEmpty()) {
                for (String l : hc.getLines()) lineInfo.add("<white>" + l);
            } else {
                lineInfo.add("<red>no lines");
            }
            inventory.setItem(10, ItemBuilder.create("WRITABLE_BOOK", "<yellow><bold>Lines", lineInfo));

            // Offsets
            inventory.setItem(19, createNumberItemDouble("COMPASS", "Offset X", hc.getOffsetX()));
            inventory.setItem(20, createNumberItemDouble("COMPASS", "Offset Y", hc.getOffsetY()));
            inventory.setItem(21, createNumberItemDouble("COMPASS", "Offset Z", hc.getOffsetZ()));

            // Scale
            inventory.setItem(23, ItemBuilder.create("BLAZE_ROD",
                    "<yellow><bold>Scale: <white>" + String.format("%.2f", hc.getScale()),
                    List.of("<gray>Left +0.1 | Right -0.1 | Shift ±0.5", "<gray>Click to set exact")));

            // Billboard
            inventory.setItem(25, ItemBuilder.create("ITEM_FRAME",
                    "<yellow><bold>Billboard: <white>" + hc.getBillboard(),
                    List.of("<gray>Left-click: cycle forward", "<gray>Right-click: cycle backward",
                            "<gray>Options: " + String.join(", ", BILLBOARD_OPTIONS))));

            // See-through
            inventory.setItem(28, ItemBuilder.create(hc.isSeeThrough() ? "GLASS" : "STONE",
                    "<yellow><bold>See-Through: <white>" + (hc.isSeeThrough() ? "Yes" : "No"),
                    List.of("<gray>Click to toggle")));

            // Background color
            inventory.setItem(30, ItemBuilder.create("CYAN_DYE",
                    "<yellow><bold>Background Color: <white>" + (hc.getBackgroundColor() == -1 ? "default" : "0x" + Integer.toHexString(hc.getBackgroundColor()).toUpperCase()),
                    List.of("<gray>Click to set ARGB int (-1 = vanilla default)")));
        }

        inventory.setItem(45, ItemBuilder.create("ARROW", "<red><bold>Back", List.of("<gray>Return to crate editor")));
        player.openInventory(inventory);
    }

    private void refreshHologram() {
        plugin.getHologramManager().removeHologram(crate.getId());
        plugin.getHologramManager().createHologram(crate);
        saveCrate(crate);
    }

    @Override
    protected void save() {
        saveCrate(crate);
        refreshHologram();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean rightClick = event.isRightClick();
        boolean shiftClick = event.isShiftClick();
        HologramConfig hc = crate.getHologramConfig();

        if (slot == 22 && hc == null) {
            hc = new HologramConfig();
            hc.setLines(new ArrayList<>(List.of("<gold><bold>" + crate.getName())));
            crate.setHologramConfig(hc);
            save();
            open();
            return;
        }
        if (hc == null) return;

        HologramConfig finalHc = hc;
        switch (slot) {
            case 10 -> {
                if (rightClick) {
                    // Remove last line
                    List<String> lines = finalHc.getLines() != null ? new ArrayList<>(finalHc.getLines()) : new ArrayList<>();
                    if (!lines.isEmpty()) lines.remove(lines.size() - 1);
                    finalHc.setLines(lines);
                    save();
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
            case 19 -> requestSignInput("Offset X", input -> {
                try { finalHc.setOffsetX(Double.parseDouble(input)); refreshHologram(); }
                catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("<red>Invalid number")); }
                open();
            });
            case 20 -> requestSignInput("Offset Y", input -> {
                try { finalHc.setOffsetY(Double.parseDouble(input)); refreshHologram(); }
                catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("<red>Invalid number")); }
                open();
            });
            case 21 -> requestSignInput("Offset Z", input -> {
                try { finalHc.setOffsetZ(Double.parseDouble(input)); refreshHologram(); }
                catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("<red>Invalid number")); }
                open();
            });
            case 23 -> {
                if (shiftClick) {
                    float delta = rightClick ? -0.5f : 0.5f;
                    finalHc.setScale(Math.max(0.1f, finalHc.getScale() + delta));
                    save();
                    open();
                } else if (rightClick) {
                    finalHc.setScale(Math.max(0.1f, finalHc.getScale() - 0.1f));
                    save();
                    open();
                } else {
                    requestSignInput("Scale (e.g. 1.5)", input -> {
                        try { finalHc.setScale(Math.max(0.1f, Float.parseFloat(input))); refreshHologram(); }
                        catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("<red>Invalid number")); }
                        open();
                    });
                }
            }
            case 25 -> {
                int idx = BILLBOARD_OPTIONS.indexOf(finalHc.getBillboard() != null ? finalHc.getBillboard().toUpperCase() : "CENTER");
                if (idx < 0) idx = 0;
                int newIdx = rightClick
                        ? (idx - 1 + BILLBOARD_OPTIONS.size()) % BILLBOARD_OPTIONS.size()
                        : (idx + 1) % BILLBOARD_OPTIONS.size();
                finalHc.setBillboard(BILLBOARD_OPTIONS.get(newIdx));
                save();
                open();
            }
            case 28 -> {
                finalHc.setSeeThrough(!finalHc.isSeeThrough());
                save();
                open();
            }
            case 30 -> requestSignInput("Background ARGB int (-1=default)", input -> {
                try { finalHc.setBackgroundColor(Integer.parseInt(input)); refreshHologram(); }
                catch (NumberFormatException e) { player.sendMessage(TextUtil.parse("<red>Invalid integer")); }
                open();
            });
            case 45 -> new CrateEditorGui(player, plugin, crate).open();
        }
    }
}
