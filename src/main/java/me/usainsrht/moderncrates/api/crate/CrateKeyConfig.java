package me.usainsrht.moderncrates.api.crate;

import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public int getCount() {
        if (itemStack != null) {
            return itemStack.getAmount();
        }
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
    }

    public List<String> getItemFlags() {
        return itemFlags;
    }

    public void setItemFlags(List<String> itemFlags) {
        this.itemFlags = itemFlags;
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

    private boolean hideTooltip;

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

    private Map<String, Integer> storedEnchantments;

    public Map<String, Integer> getStoredEnchantments() {
        return storedEnchantments;
    }

    public void setStoredEnchantments(Map<String, Integer> storedEnchantments) {
        this.storedEnchantments = storedEnchantments;
    }
}
