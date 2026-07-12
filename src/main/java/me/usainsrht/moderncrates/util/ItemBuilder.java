package me.usainsrht.moderncrates.util;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.animation.GuiItemConfig;
import me.usainsrht.moderncrates.api.reward.RewardDisplay;
import me.usainsrht.moderncrates.api.reward.RewardItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility for building ItemStacks from configuration data.
 */
public final class ItemBuilder {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ItemBuilder() {}

    public static ItemStack fromDisplay(RewardDisplay display) {
        return fromDisplay(display, 0.0);
    }

    public static ItemStack fromDisplay(Reward reward, Crate crate) {
        if (reward == null) {
            return new ItemStack(Material.STONE);
        }

        double chancePercentage = calculateChancePercentage(reward, crate);
        ItemStack item;

        if (reward.getDisplay() != null) {
            item = fromDisplay(reward.getDisplay(), chancePercentage);
        } else if (reward.hasItems()) {
            RewardItem firstItem = reward.getItems().values().iterator().next();
            item = fromRewardItem(firstItem);
            item = applyChancePlaceholdersToItem(item, chancePercentage);
        } else {
            return new ItemStack(Material.STONE);
        }

        appendChanceLoreTemplate(item, crate, chancePercentage);
        return item;
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

        String formattedChance = formatChance(chancePercentage);

        if (display.getName() != null) {
            meta.displayName(TextUtil.parse(replaceChancePlaceholders(display.getName(), formattedChance)));
        }
        if (display.getLore() != null) {
            meta.lore(display.getLore().stream()
                    .map(line -> replaceChancePlaceholders(line, formattedChance))
                    .map(TextUtil::parse)
                    .collect(Collectors.toList()));
        }
        if (display.getEnchantments() != null) {
            applyEnchantments(meta, display.getEnchantments());
        }
        if (display.getStoredEnchantments() != null) {
            applyStoredEnchantments(meta, display.getStoredEnchantments());
        }
        if (display.getItemFlags() != null) {
            applyItemFlags(meta, display.getItemFlags());
        }
        if (display.isHideEnchantments()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS);
        }
        meta.setHideTooltip(display.isHideTooltip());
        item.setItemMeta(meta);
        return item;
    }

    public static RewardItem toRewardItem(ItemStack stack) {
        RewardItem rewardItem = new RewardItem();
        if (stack == null || stack.getType() == Material.AIR) {
            return rewardItem;
        }

        rewardItem.setMaterial(stack.getType().name());
        rewardItem.setAmount(stack.getAmount());

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return rewardItem;
        }

        if (meta.hasDisplayName()) {
            rewardItem.setName(componentToMiniMessage(meta.displayName()));
        }
        if (meta.hasLore() && meta.lore() != null) {
            rewardItem.setLore(meta.lore().stream()
                    .map(ItemBuilder::componentToMiniMessage)
                    .collect(Collectors.toList()));
        }

        Map<String, Integer> enchantments = extractEnchantments(meta);
        if (!enchantments.isEmpty()) {
            rewardItem.setEnchantments(enchantments);
        }

        Map<String, Integer> storedEnchantments = extractStoredEnchantments(meta);
        if (!storedEnchantments.isEmpty()) {
            rewardItem.setStoredEnchantments(storedEnchantments);
        }

        if (!meta.getItemFlags().isEmpty()) {
            rewardItem.setItemFlags(meta.getItemFlags().stream()
                    .map(ItemFlag::name)
                    .collect(Collectors.toList()));
        }

        rewardItem.setHideTooltip(meta.isHideTooltip());
        rewardItem.setHideEnchantments(meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)
                || meta.hasItemFlag(ItemFlag.HIDE_STORED_ENCHANTS));

        return rewardItem;
    }

    public static RewardDisplay toRewardDisplay(ItemStack stack) {
        RewardDisplay display = new RewardDisplay();
        if (stack == null || stack.getType() == Material.AIR) {
            return display;
        }

        display.setMaterial(stack.getType().name());
        display.setAmount(stack.getAmount());

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return display;
        }

        if (meta.hasDisplayName()) {
            display.setName(componentToMiniMessage(meta.displayName()));
        }
        if (meta.hasLore() && meta.lore() != null) {
            display.setLore(meta.lore().stream()
                    .map(ItemBuilder::componentToMiniMessage)
                    .collect(Collectors.toList()));
        }

        Map<String, Integer> enchantments = extractEnchantments(meta);
        if (!enchantments.isEmpty()) {
            display.setEnchantments(enchantments);
        }

        Map<String, Integer> storedEnchantments = extractStoredEnchantments(meta);
        if (!storedEnchantments.isEmpty()) {
            display.setStoredEnchantments(storedEnchantments);
        }

        if (!meta.getItemFlags().isEmpty()) {
            display.setItemFlags(meta.getItemFlags().stream()
                    .map(ItemFlag::name)
                    .collect(Collectors.toList()));
        }

        display.setHideTooltip(meta.isHideTooltip());
        display.setHideEnchantments(meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)
                || meta.hasItemFlag(ItemFlag.HIDE_STORED_ENCHANTS));

        return display;
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
        if (rewardItem.getStoredEnchantments() != null) {
            applyStoredEnchantments(meta, rewardItem.getStoredEnchantments());
        }
        if (rewardItem.getItemFlags() != null) {
            applyItemFlags(meta, rewardItem.getItemFlags());
        }
        if (rewardItem.isHideEnchantments()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS);
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

        String formattedChance = formatChance(calculateChancePercentage(reward, crate));

        if (reward.getDisplay() != null && reward.getDisplay().getName() != null) {
            return TextUtil.parse(replaceChancePlaceholders(reward.getDisplay().getName(), formattedChance));
        }

        if (reward.hasItems()) {
            RewardItem firstItem = reward.getItems().values().iterator().next();
            if (firstItem.getName() != null) {
                return TextUtil.parse(replaceChancePlaceholders(firstItem.getName(), formattedChance));
            }
            if (firstItem.getMaterial() != null) {
                Material mat = Material.matchMaterial(firstItem.getMaterial().toUpperCase());
                if (mat != null) {
                    return net.kyori.adventure.text.Component.translatable(mat.translationKey())
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
                }
            }
        }

        if (reward.getDisplay() != null && reward.getDisplay().getMaterial() != null) {
            Material mat = Material.matchMaterial(reward.getDisplay().getMaterial().toUpperCase());
            if (mat != null) {
                return net.kyori.adventure.text.Component.translatable(mat.translationKey())
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
            }
        }

        return net.kyori.adventure.text.Component.text(reward.getId());
    }

    private static void appendChanceLoreTemplate(ItemStack item, Crate crate, double chancePercentage) {
        if (crate == null || !crate.isAutoShowChanceOnLore()) return;
        List<String> template = crate.getChanceLoreTemplate();
        if (template == null || template.isEmpty()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String formattedChance = formatChance(chancePercentage);
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        for (String line : template) {
            lore.add(TextUtil.parse(replaceChancePlaceholders(line, formattedChance)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private static ItemStack applyChancePlaceholdersToItem(ItemStack item, double chancePercentage) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String formattedChance = formatChance(chancePercentage);
        if (meta.hasDisplayName()) {
            meta.displayName(TextUtil.parse(
                    replaceChancePlaceholders(componentToMiniMessage(meta.displayName()), formattedChance)));
        }
        if (meta.hasLore() && meta.lore() != null) {
            meta.lore(meta.lore().stream()
                    .map(ItemBuilder::componentToMiniMessage)
                    .map(line -> replaceChancePlaceholders(line, formattedChance))
                    .map(TextUtil::parse)
                    .collect(Collectors.toList()));
        }
        item.setItemMeta(meta);
        return item;
    }

    private static double calculateChancePercentage(Reward reward, Crate crate) {
        if (crate != null) {
            double totalWeight = crate.getTotalWeight();
            if (totalWeight > 0) {
                return (reward.getChance() / totalWeight) * 100.0;
            }
        }
        return reward.getChance();
    }

    private static String formatChance(double chancePercentage) {
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(java.util.Locale.US);
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.##", symbols);
        return df.format(chancePercentage);
    }

    private static String replaceChancePlaceholders(String text, String formattedChance) {
        return text.replace("<chance>", formattedChance).replace("%chance%", formattedChance);
    }

    private static String componentToMiniMessage(Component component) {
        if (component == null) return "";
        return MINI_MESSAGE.serialize(component);
    }

    @SuppressWarnings("deprecation")
    private static Map<String, Integer> extractEnchantments(ItemMeta meta) {
        Map<String, Integer> enchantments = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            enchantments.put(entry.getKey().getKey().getKey().toUpperCase(), entry.getValue());
        }
        return enchantments;
    }

    @SuppressWarnings("deprecation")
    private static Map<String, Integer> extractStoredEnchantments(ItemMeta meta) {
        Map<String, Integer> storedEnchantments = new LinkedHashMap<>();
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            for (Map.Entry<Enchantment, Integer> entry : bookMeta.getStoredEnchants().entrySet()) {
                storedEnchantments.put(entry.getKey().getKey().getKey().toUpperCase(), entry.getValue());
            }
        }
        return storedEnchantments;
    }

    @SuppressWarnings("deprecation")
    private static void applyEnchantments(ItemMeta meta, Map<String, Integer> enchantments) {
        for (var entry : enchantments.entrySet()) {
            Enchantment ench = Enchantment.getByName(entry.getKey().toUpperCase());
            if (ench != null) {
                if (meta instanceof EnchantmentStorageMeta bookMeta) {
                    bookMeta.addStoredEnchant(ench, entry.getValue(), true);
                } else {
                    meta.addEnchant(ench, entry.getValue(), true);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void applyStoredEnchantments(ItemMeta meta, Map<String, Integer> storedEnchantments) {
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            for (var entry : storedEnchantments.entrySet()) {
                Enchantment ench = Enchantment.getByName(entry.getKey().toUpperCase());
                if (ench != null) {
                    bookMeta.addStoredEnchant(ench, entry.getValue(), true);
                }
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
