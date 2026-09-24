package me.usainsrht.moderncrates.api.reward;

import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private boolean hideTooltip;
    private boolean hideEnchantments;
    private Map<String, Integer> storedEnchantments;
    private ItemStack itemStack;

    public ItemStack getItemStack() {
        if (itemStack != null) {
            return itemStack.clone();
        }
        if (material != null) {
            ItemStack built = ItemBuilder.create(material, name, lore, hideTooltip);
            if (amount > 1) {
                built.setAmount(amount);
            }
            return built;
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

    public int getAmount() {
        if (itemStack != null) {
            return itemStack.getAmount();
        }
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
        if (this.itemStack != null) this.itemStack.setAmount(Math.max(1, amount));
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

    public boolean isHideEnchantments() {
        return hideEnchantments;
    }

    public void setHideEnchantments(boolean hideEnchantments) {
        this.hideEnchantments = hideEnchantments;
    }

    public Map<String, Integer> getStoredEnchantments() {
        return storedEnchantments;
    }

    public void setStoredEnchantments(Map<String, Integer> storedEnchantments) {
        this.storedEnchantments = storedEnchantments;
    }
}
