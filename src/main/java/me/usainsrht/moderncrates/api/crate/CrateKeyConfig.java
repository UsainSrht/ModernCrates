package me.usainsrht.moderncrates.api.crate;

import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for crate key requirements.
 */
public class CrateKeyConfig {

    private boolean required;
    private String material;
    private int count;
    private Map<String, Integer> enchantments;
    private List<String> itemFlags;
    private String name;
    private List<String> lore;

    private ItemStack itemStack;

    public ItemStack getItemStack() {
        return itemStack != null ? itemStack.clone() : null;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack != null ? itemStack.clone() : null;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
        this.itemStack = null;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
        if (this.itemStack != null) this.itemStack.setAmount(Math.max(1, count));
    }

    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(Map<String, Integer> enchantments) {
        this.enchantments = enchantments;
        this.itemStack = null;
    }

    public List<String> getItemFlags() {
        return itemFlags;
    }

    public void setItemFlags(List<String> itemFlags) {
        this.itemFlags = itemFlags;
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

    private boolean hideTooltip;

    public boolean isHideTooltip() {
        return hideTooltip;
    }

    public void setHideTooltip(boolean hideTooltip) {
        this.hideTooltip = hideTooltip;
        this.itemStack = null;
    }

    private Map<String, Integer> storedEnchantments;

    public Map<String, Integer> getStoredEnchantments() {
        return storedEnchantments;
    }

    public void setStoredEnchantments(Map<String, Integer> storedEnchantments) {
        this.storedEnchantments = storedEnchantments;
        this.itemStack = null;
    }
}
