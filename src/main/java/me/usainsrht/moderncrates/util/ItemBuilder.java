package me.usainsrht.moderncrates.util;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
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
        return fromDisplay(display, 0.0);
    }

    public static ItemStack fromDisplay(Reward reward, Crate crate) {
        if (reward == null) {
            return new ItemStack(Material.STONE);
        }
        RewardDisplay display = reward.getDisplay();
        if (display == null) {
            return new ItemStack(Material.STONE);
        }
        double chancePercentage = 0.0;
        if (crate != null) {
            double totalWeight = crate.getTotalWeight();
            if (totalWeight > 0) {
                chancePercentage = (reward.getChance() / totalWeight) * 100.0;
            }
        } else {
            chancePercentage = reward.getChance();
        }
        return fromDisplay(display, chancePercentage);
    }

    public static ItemStack fromDisplay(RewardDisplay display, double chancePercentage) {
        if (display == null || display.getMaterial() == null) {
            return new ItemStack(Material.STONE);
        }
        Material mat = Material.matchMaterial(display.getMaterial().toUpperCase());
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat, display.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(java.util.Locale.US);
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.##", symbols);
        String formattedChance = df.format(chancePercentage);

        if (display.getName() != null) {
            meta.displayName(TextUtil.parse(display.getName()
                    .replace("<chance>", formattedChance)
                    .replace("%chance%", formattedChance)));
        }
        if (display.getLore() != null) {
            meta.lore(display.getLore().stream()
                    .map(line -> line.replace("<chance>", formattedChance).replace("%chance%", formattedChance))
                    .map(TextUtil::parse)
                    .collect(Collectors.toList()));
        }
        if (display.getEnchantments() != null) {
            applyEnchantments(meta, display.getEnchantments());
        }
        if (display.getItemFlags() != null) {
            applyItemFlags(meta, display.getItemFlags());
        }
        meta.setHideTooltip(display.isHideTooltip());
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
        meta.setHideTooltip(rewardItem.isHideTooltip());
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
        meta.setHideTooltip(config.isHideTooltip());
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

    public static ItemStack create(String material, String name, List<String> lore, boolean hideTooltip) {
        ItemStack item = create(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setHideTooltip(hideTooltip);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack withHideTooltip(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setHideTooltip(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static net.kyori.adventure.text.Component getRewardComponent(Reward reward, Crate crate) {
        if (reward == null) {
            return net.kyori.adventure.text.Component.empty();
        }
        if (reward.getDisplay() != null && reward.getDisplay().getName() != null) {
            double totalWeight = crate != null ? crate.getTotalWeight() : 0.0;
            double chancePercentage = totalWeight > 0 ? (reward.getChance() / totalWeight) * 100.0 : 0.0;
            java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(java.util.Locale.US);
            java.text.DecimalFormat df = new java.text.DecimalFormat("#.##", symbols);
            String formattedChance = df.format(chancePercentage);
            String rawName = reward.getDisplay().getName()
                    .replace("<chance>", formattedChance)
                    .replace("%chance%", formattedChance);
            return TextUtil.parse(rawName);
        }
        if (reward.getDisplay() != null && reward.getDisplay().getMaterial() != null) {
            org.bukkit.Material mat = org.bukkit.Material.matchMaterial(reward.getDisplay().getMaterial().toUpperCase());
            if (mat != null) {
                return net.kyori.adventure.text.Component.translatable(mat.translationKey())
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
            }
        }
        return net.kyori.adventure.text.Component.text(reward.getId());
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
