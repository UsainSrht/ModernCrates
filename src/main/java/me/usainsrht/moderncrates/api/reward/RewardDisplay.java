package me.usainsrht.moderncrates.api.reward;

import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Display configuration for a reward shown in the GUI.
 */
public class RewardDisplay {

    private String material;
    private String name;
    private List<String> lore;
    private Map<String, Integer> enchantments;
    private List<String> itemFlags;
    private int amount = 1;
    private boolean hideTooltip;
    private boolean hideEnchantments;
    private Map<String, Integer> storedEnchantments;
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

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
        if (this.itemStack != null) this.itemStack.setAmount(Math.max(1, amount));
    }

    public boolean isHideTooltip() {
        return hideTooltip;
    }

    public void setHideTooltip(boolean hideTooltip) {
        this.hideTooltip = hideTooltip;
        this.itemStack = null;
    }

    public boolean isHideEnchantments() {
        return hideEnchantments;
    }

    public void setHideEnchantments(boolean hideEnchantments) {
        this.hideEnchantments = hideEnchantments;
        this.itemStack = null;
    }

    public Map<String, Integer> getStoredEnchantments() {
        return storedEnchantments;
    }

    public void setStoredEnchantments(Map<String, Integer> storedEnchantments) {
        this.storedEnchantments = storedEnchantments;
        this.itemStack = null;
    }
}
