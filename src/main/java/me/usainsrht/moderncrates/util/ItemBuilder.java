package me.usainsrht.moderncrates.util;

import me.usainsrht.itemapi.yamlitem.YamlItem;
import me.usainsrht.moderncrates.api.animation.GuiItemConfig;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.reward.RewardDisplay;
import me.usainsrht.moderncrates.api.reward.RewardItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility for building and adapting ItemStacks from configuration data via YamlItem.
 */
public final class ItemBuilder {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ItemBuilder() {}

    public static ItemStack parse(ConfigurationSection section) {
        if (section == null) return new ItemStack(Material.STONE);
        return YamlItem.parse(section);
    }

    public static ItemStack parse(Map<?, ?> map) {
        if (map == null) return new ItemStack(Material.STONE);
        return YamlItem.parse(map);
    }

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
        if (display == null) {
            return new ItemStack(Material.STONE);
        }
        ItemStack item = display.getItemStack();
        if (item == null) {
            return new ItemStack(Material.STONE);
        }
        return applyChancePlaceholdersToItem(item.clone(), chancePercentage);
    }

    public static ItemStack fromRewardItem(RewardItem rewardItem) {
        if (rewardItem == null) {
            return new ItemStack(Material.STONE);
        }
        ItemStack item = rewardItem.getItemStack();
        return item != null ? item.clone() : new ItemStack(Material.STONE);
    }

    public static ItemStack fromGuiItemConfig(GuiItemConfig config) {
        if (config == null) {
            return new ItemStack(Material.STONE);
        }
        ItemStack item = config.getItemStack();
        return item != null ? item.clone() : new ItemStack(Material.STONE);
    }

    public static RewardItem toRewardItem(ItemStack stack) {
        RewardItem rewardItem = new RewardItem();
        if (stack == null || stack.getType() == Material.AIR) {
            return rewardItem;
        }
        rewardItem.setItemStack(stack.clone());
        rewardItem.setMaterial(stack.getType().name());
        rewardItem.setAmount(stack.getAmount());

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                rewardItem.setName(componentToMiniMessage(meta.displayName()));
            }
            if (meta.hasLore() && meta.lore() != null) {
                rewardItem.setLore(meta.lore().stream()
                        .map(ItemBuilder::componentToMiniMessage)
                        .collect(Collectors.toList()));
            }
            rewardItem.setHideTooltip(meta.isHideTooltip());
        }
        return rewardItem;
    }

    public static RewardDisplay toRewardDisplay(ItemStack stack) {
        RewardDisplay display = new RewardDisplay();
        if (stack == null || stack.getType() == Material.AIR) {
            return display;
        }
        display.setItemStack(stack.clone());
        display.setMaterial(stack.getType().name());
        display.setAmount(stack.getAmount());

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                display.setName(componentToMiniMessage(meta.displayName()));
            }
            if (meta.hasLore() && meta.lore() != null) {
                display.setLore(meta.lore().stream()
                        .map(ItemBuilder::componentToMiniMessage)
                        .collect(Collectors.toList()));
            }
            display.setHideTooltip(meta.isHideTooltip());
        }
        return display;
    }

    public static ItemStack create(String material, String name, List<String> lore) {
        return create(material, name, lore, false);
    }

    public static ItemStack create(String material, String name, List<String> lore, boolean hideTooltip) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("material", material != null ? material : "STONE");
        if (name != null) map.put("name", name);
        if (lore != null && !lore.isEmpty()) map.put("lore", lore);
        if (hideTooltip) map.put("hide_tooltip", true);
        return YamlItem.parse(map);
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

    public static Component getRewardComponent(Reward reward, Crate crate) {
        if (reward == null) {
            return Component.empty();
        }

        String formattedChance = formatChance(calculateChancePercentage(reward, crate));

        if (reward.getDisplay() != null) {
            ItemStack stack = reward.getDisplay().getItemStack();
            if (stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
                return TextUtil.parse(replaceChancePlaceholders(
                        componentToMiniMessage(stack.getItemMeta().displayName()), formattedChance));
            }
            if (reward.getDisplay().getName() != null) {
                return TextUtil.parse(replaceChancePlaceholders(reward.getDisplay().getName(), formattedChance));
            }
            if (stack != null && stack.getType() != Material.AIR) {
                return Component.translatable(stack.getType().translationKey())
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
            }
        }

        if (reward.hasItems()) {
            RewardItem firstItem = reward.getItems().values().iterator().next();
            ItemStack stack = firstItem.getItemStack();
            if (stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
                return TextUtil.parse(replaceChancePlaceholders(
                        componentToMiniMessage(stack.getItemMeta().displayName()), formattedChance));
            }
            if (firstItem.getName() != null) {
                return TextUtil.parse(replaceChancePlaceholders(firstItem.getName(), formattedChance));
            }
            if (stack != null && stack.getType() != Material.AIR) {
                return Component.translatable(stack.getType().translationKey())
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
            }
        }

        return Component.text(reward.getId());
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
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#.##", symbols);
        return df.format(chancePercentage);
    }

    private static String replaceChancePlaceholders(String text, String formattedChance) {
        return text.replace("<chance>", formattedChance).replace("%chance%", formattedChance);
    }

    private static String componentToMiniMessage(Component component) {
        if (component == null) return "";
        return MINI_MESSAGE.serialize(component);
    }
}
