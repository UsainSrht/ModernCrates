package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.PreviewConfig;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class PreviewEditorGui extends EditorGuiBase {

    private final Crate crate;

    public PreviewEditorGui(Player player, ModernCratesPlugin plugin, Crate crate) {
        super(player, plugin);
        this.crate = crate;
    }

    @Override
    public void open() {
        PreviewConfig pc = crate.getPreviewConfig();

        inventory = Bukkit.createInventory(this, 54, TextUtil.parse("<dark_red><bold>Preview: " + crate.getName()));
        fillBlack();

        if (pc == null) {
            inventory.setItem(22, ItemBuilder.create("BARRIER", "<red>No preview config",
                    List.of("<gray>Click to create default")));
        } else {
            inventory.setItem(10, ItemBuilder.create("NAME_TAG",
                    "<yellow><bold>Title: <white>" + pc.getTitle(), List.of("<gray>Click to set")));
            inventory.setItem(12, createNumberItem("LADDER", "Rows", pc.getRows()));

            String fillMat = pc.getFill() != null ? pc.getFill().getMaterial() : "none";
            inventory.setItem(14, ItemBuilder.create(fillMat.equals("none") ? "BARRIER" : fillMat,
                    "<yellow><bold>Fill Material: <white>" + fillMat, List.of("<gray>Click to set")));

            inventory.setItem(19, createSlotItemDisplay("Close Button", pc.getCloseButton()));
            inventory.setItem(21, createSlotItemDisplay("Next Button", pc.getNextButton()));
            inventory.setItem(23, createSlotItemDisplay("Previous Button", pc.getPreviousButton()));

            List<String> soundLore = new ArrayList<>();
            soundLore.add("<gray>Click to add sound");
            soundLore.add("<gray>Right-click to clear");
            if (pc.getSounds() != null) for (String s : pc.getSounds()) soundLore.add("<white>" + s);
            inventory.setItem(25, ItemBuilder.create("NOTE_BLOCK", "<yellow><bold>Sounds", soundLore));
        }

        inventory.setItem(45, ItemBuilder.create("ARROW", "<red><bold>Back", List.of("<gray>Return to crate editor")));
        inventory.setItem(49, ItemBuilder.create("LIME_WOOL", "<green><bold>Save", List.of("<gray>Save crate")));
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean rightClick = event.isRightClick();
        boolean shiftClick = event.isShiftClick();
        PreviewConfig pc = crate.getPreviewConfig();

        if (slot == 22 && pc == null) {
            pc = new PreviewConfig();
            pc.setTitle("<gold>Preview: " + crate.getName());
            pc.setRows(6);
            PreviewConfig.GuiItem fill = new PreviewConfig.GuiItem();
            fill.setMaterial("BLACK_STAINED_GLASS_PANE");
            fill.setName(" ");
            pc.setFill(fill);
            crate.setPreviewConfig(pc);
            open();
            return;
        }
        if (pc == null) return;

        PreviewConfig finalPc = pc;
        switch (slot) {
            case 10 -> requestSignInput("Preview title", input -> {
                finalPc.setTitle(input);
                open();
            });
            case 12 -> {
                adjustInt(finalPc::getRows, finalPc::setRows, rightClick, shiftClick, 1, 6);
                open();
            }
            case 14 -> requestSignInput("Fill material", input -> {
                if (finalPc.getFill() == null) {
                    PreviewConfig.GuiItem fill = new PreviewConfig.GuiItem();
                    fill.setMaterial(input.toUpperCase());
                    fill.setName(" ");
                    finalPc.setFill(fill);
                } else {
                    finalPc.getFill().setMaterial(input.toUpperCase());
                }
                open();
            });
            case 19 -> editSlotItem("Close Button", finalPc.getCloseButton(), item -> {
                finalPc.setCloseButton(item);
                open();
            });
            case 21 -> editSlotItem("Next Button", finalPc.getNextButton(), item -> {
                finalPc.setNextButton(item);
                open();
            });
            case 23 -> editSlotItem("Previous Button", finalPc.getPreviousButton(), item -> {
                finalPc.setPreviousButton(item);
                open();
            });
            case 25 -> {
                if (rightClick) {
                    finalPc.setSounds(null);
                    open();
                } else {
                    requestSignInput("Sound name", input -> {
                        List<String> sounds = finalPc.getSounds() != null ? new ArrayList<>(finalPc.getSounds()) : new ArrayList<>();
                        sounds.add(input);
                        finalPc.setSounds(sounds);
                        open();
                    });
                }
            }
            case 45 -> new CrateEditorGui(player, plugin, crate).open();
            case 49 -> { saveCrate(crate); player.sendMessage(TextUtil.parse("<green>Crate saved!")); }
        }
    }

    private void editSlotItem(String label, PreviewConfig.SlotItem current, java.util.function.Consumer<PreviewConfig.SlotItem> setter) {
        requestSignInput("slot,mat,name", input -> {
            String[] parts = input.split(",", 3);
            if (parts.length >= 3) {
                PreviewConfig.SlotItem item = new PreviewConfig.SlotItem();
                try { item.setSlot(Integer.parseInt(parts[0].trim())); } catch (NumberFormatException e) { item.setSlot(0); }
                item.setMaterial(parts[1].trim().toUpperCase());
                item.setName(parts[2].trim());
                setter.accept(item);
            } else {
                player.sendMessage(TextUtil.parse("<red>Invalid format. Use: slot,material,name"));
                open();
            }
        });
    }
}
