package me.usainsrht.moderncrates.util;

import me.usainsrht.moderncrates.api.animation.GuiItemConfig;
import me.usainsrht.moderncrates.api.reward.RewardDisplay;
import me.usainsrht.moderncrates.api.reward.RewardItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility for building ItemStacks from configuration data.
 */
public final class ItemBuilder {

    private ItemBuilder() {}

    public static ItemStack fromDisplay(RewardDisplay display) {
        if (display == null || display.getMaterial() == null) {
            return new ItemStack(Material.STONE);
        }
        Material mat = Material.matchMaterial(display.getMaterial().toUpperCase());
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat, display.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (display.getName() != null) {
            meta.displayName(TextUtil.parse(display.getName()));
        }
        if (display.getLore() != null) {
            meta.lore(display.getLore().stream()
                    .map(TextUtil::parse)
                    .collect(Collectors.toList()));
        }
        if (display.getEnchantments() != null) {
            applyEnchantments(meta, display.getEnchantments());
        }
        if (display.getItemFlags() != null) {
            applyItemFlags(meta, display.getItemFlags());
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack fromRewardItem(RewardItem rewardItem) {
        if (rewardItem == null || rewardItem.getMaterial() == null) {
            return new ItemStack(Material.STONE);
        }
        Material mat = Material.matchMaterial(rewardItem.getMaterial().toUpperCase());
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat, rewardItem.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (rewardItem.getName() != null) {
            meta.displayName(TextUtil.parse(rewardItem.getName()));
        }
        if (rewardItem.getLore() != null) {
            meta.lore(rewardItem.getLore().stream()
                    .map(TextUtil::parse)
                    .collect(Collectors.toList()));
        }
        if (rewardItem.getEnchantments() != null) {
            applyEnchantments(meta, rewardItem.getEnchantments());
        }
        if (rewardItem.getItemFlags() != null) {
            applyItemFlags(meta, rewardItem.getItemFlags());
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack fromGuiItemConfig(GuiItemConfig config) {
        if (config == null || config.getMaterial() == null) {
            return new ItemStack(Material.STONE);
        }
        Material mat = Material.matchMaterial(config.getMaterial().toUpperCase());
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (config.getName() != null) {
            meta.displayName(TextUtil.parse(config.getName()));
        }
        if (config.getLore() != null) {
            meta.lore(config.getLore().stream()
                    .map(TextUtil::parse)
                    .collect(Collectors.toList()));
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack create(String material, String name, List<String> lore) {
        Material mat = Material.matchMaterial(material.toUpperCase());
        if (mat == null) mat = Material.STONE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (name != null) {
            meta.displayName(TextUtil.parse(name));
        }
        if (lore != null) {
            meta.lore(lore.stream().map(TextUtil::parse).collect(Collectors.toList()));
        }
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private static void applyEnchantments(ItemMeta meta, Map<String, Integer> enchantments) {
        for (var entry : enchantments.entrySet()) {
            Enchantment ench = Enchantment.getByName(entry.getKey().toUpperCase());
            if (ench != null) {
                meta.addEnchant(ench, entry.getValue(), true);
            }
        }
    }

    private static void applyItemFlags(ItemMeta meta, List<String> flags) {
        for (String flag : flags) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
