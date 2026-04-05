package me.usainsrht.moderncrates.api.reward;

import java.util.List;
import java.util.Map;

/**
 * Represents an actual item to give to the player as part of a reward.
 */
public class RewardItem {

    private String material;
    private int amount = 1;
    private String name;
    private List<String> lore;
    private Map<String, Integer> enchantments;
    private List<String> itemFlags;
    private Map<String, Object> nbt;

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
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

    public Map<String, Object> getNbt() {
        return nbt;
    }

    public void setNbt(Map<String, Object> nbt) {
        this.nbt = nbt;
    }
}
