package me.usainsrht.moderncrates.api.crate;

import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the crate block item representation.
 */
public class CrateItemConfig {

    private String material;
    private String name;
    private List<String> lore;
    private boolean hideTooltip;
    private ItemStack itemStack;

    public ItemStack getItemStack() {
        return itemStack != null ? itemStack.clone() : null;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack != null ? itemStack.clone() : null;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
        this.itemStack = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.itemStack = null;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
        this.itemStack = null;
    }

    public boolean isHideTooltip() {
        return hideTooltip;
    }

    public void setHideTooltip(boolean hideTooltip) {
        this.hideTooltip = hideTooltip;
        this.itemStack = null;
    }
}
