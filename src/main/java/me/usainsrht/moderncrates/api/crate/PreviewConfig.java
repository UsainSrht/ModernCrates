package me.usainsrht.moderncrates.api.crate;

import java.util.List;
import java.util.Map;

/**
 * Configuration for the crate preview GUI.
 */
public class PreviewConfig {

    private String title;
    private int rows;
    private GuiItem fill;
    private SlotItem closeButton;
    private SlotItem nextButton;
    private SlotItem previousButton;
    private List<String> sounds;
    private Map<Integer, GuiItem> customSlots;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public GuiItem getFill() {
        return fill;
    }

    public void setFill(GuiItem fill) {
        this.fill = fill;
    }

    public SlotItem getCloseButton() {
        return closeButton;
    }

    public void setCloseButton(SlotItem closeButton) {
        this.closeButton = closeButton;
    }

    public SlotItem getNextButton() {
        return nextButton;
    }

    public void setNextButton(SlotItem nextButton) {
        this.nextButton = nextButton;
    }

    public SlotItem getPreviousButton() {
        return previousButton;
    }

    public void setPreviousButton(SlotItem previousButton) {
        this.previousButton = previousButton;
    }

    public List<String> getSounds() {
        return sounds;
    }

    public void setSounds(List<String> sounds) {
        this.sounds = sounds;
    }

    public Map<Integer, GuiItem> getCustomSlots() {
        return customSlots;
    }

    public void setCustomSlots(Map<Integer, GuiItem> customSlots) {
        this.customSlots = customSlots;
    }

    /**
     * A simple GUI item with material and name.
     */
    public static class GuiItem {
        private String material;
        private String name;
        private List<String> lore;

        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getLore() { return lore; }
        public void setLore(List<String> lore) { this.lore = lore; }
    }

    /**
     * A GUI item bound to a specific slot.
     */
    public static class SlotItem extends GuiItem {
        private int slot;

        public int getSlot() { return slot; }
        public void setSlot(int slot) { this.slot = slot; }
    }
}
