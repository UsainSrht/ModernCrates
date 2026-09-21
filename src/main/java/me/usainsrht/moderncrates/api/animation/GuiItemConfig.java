package me.usainsrht.moderncrates.api.animation;

import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a GUI item used in animations.
 */
public class GuiItemConfig {

    private String material;
    private String name;
    private List<String> lore;
    private Map<String, Object> nbt;
    private boolean hideTooltip;
    private ItemStack itemStack;

    public ItemStack getItemStack() {
        return itemStack != null ? itemStack.clone() : null;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack != null ? itemStack.clone() : null;
    }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; this.itemStack = null; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.itemStack = null; }

    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; this.itemStack = null; }

    public Map<String, Object> getNbt() { return nbt; }
    public void setNbt(Map<String, Object> nbt) { this.nbt = nbt; this.itemStack = null; }

    public boolean isHideTooltip() { return hideTooltip; }
    public void setHideTooltip(boolean hideTooltip) { this.hideTooltip = hideTooltip; this.itemStack = null; }
}
