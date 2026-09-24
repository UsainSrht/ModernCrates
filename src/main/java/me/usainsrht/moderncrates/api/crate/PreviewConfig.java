package me.usainsrht.moderncrates.api.crate;

import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        private boolean hideTooltip;
        private ItemStack itemStack;

        public ItemStack getItemStack() {
            if (itemStack != null) {
                return itemStack.clone();
            }
            if (material != null) {
                return ItemBuilder.create(material, name, lore, hideTooltip);
            }
            return null;
        }

        public void setItemStack(ItemStack itemStack) {
            this.itemStack = itemStack != null ? itemStack.clone() : null;
        }

        public String getMaterial() {
            if (material != null) {
                return material;
            }
            if (itemStack != null && itemStack.getType() != null) {
                return itemStack.getType().name();
            }
            return null;
        }

        public void setMaterial(String material) {
            this.material = material;
            if (this.itemStack != null) {
                Material mat = Material.matchMaterial(material != null ? material : "");
                if (mat != null) {
                    this.itemStack.setType(mat);
                }
            }
        }

        public String getName() {
            if (name != null) {
                return name;
            }
            if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                return ItemBuilder.componentToMiniMessage(itemStack.getItemMeta().displayName());
            }
            return null;
        }

        public void setName(String name) {
            this.name = name;
            if (this.itemStack != null) {
                ItemMeta meta = this.itemStack.getItemMeta();
                if (meta != null) {
                    meta.displayName(name != null ? TextUtil.parse(name) : null);
                    this.itemStack.setItemMeta(meta);
                }
            }
        }

        public List<String> getLore() {
            if (lore != null) {
                return lore;
            }
            if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().lore() != null) {
                return itemStack.getItemMeta().lore().stream()
                        .map(ItemBuilder::componentToMiniMessage)
                        .collect(Collectors.toList());
            }
            return null;
        }

        public void setLore(List<String> lore) {
            this.lore = lore;
            if (this.itemStack != null) {
                ItemMeta meta = this.itemStack.getItemMeta();
                if (meta != null) {
                    meta.lore(lore != null ? lore.stream().map(TextUtil::parse).collect(Collectors.toList()) : null);
                    this.itemStack.setItemMeta(meta);
                }
            }
        }

        public boolean isHideTooltip() {
            if (itemStack != null && itemStack.hasItemMeta()) {
                return itemStack.getItemMeta().isHideTooltip();
            }
            return hideTooltip;
        }

        public void setHideTooltip(boolean hideTooltip) {
            this.hideTooltip = hideTooltip;
            if (this.itemStack != null) {
                ItemMeta meta = this.itemStack.getItemMeta();
                if (meta != null) {
                    meta.setHideTooltip(hideTooltip);
                    this.itemStack.setItemMeta(meta);
                }
            }
        }
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
