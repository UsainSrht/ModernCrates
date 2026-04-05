package me.usainsrht.moderncrates.api.crate;

import org.bukkit.inventory.ItemStack;

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
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(Map<String, Integer> enchantments) {
        this.enchantments = enchantments;
    }

    public List<String> getItemFlags() {
        return itemFlags;
    }

    public void setItemFlags(List<String> itemFlags) {
        this.itemFlags = itemFlags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }
}
