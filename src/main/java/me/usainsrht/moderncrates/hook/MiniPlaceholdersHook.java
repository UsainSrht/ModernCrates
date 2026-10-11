package me.usainsrht.moderncrates.hook;

import io.github.miniplaceholders.api.Expansion;
import me.usainsrht.itemapi.itemtext.ItemText;
import me.usainsrht.itemapi.itemtext.ItemTextOptions;
import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.PlaceholderUtil;
import me.usainsrht.moderncrates.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;

/**
 * MiniPlaceholders expansion and Adventure MiniMessage TagResolver for ModernCrates.
 * Supports:
 *  - Crate:   <moderncrates_crate:<crate_id>[:options]> or <moderncrates_crate_<crate_id>[:options]>
 *  - Key:     <moderncrates_key:<crate_id>[:options]>   or <moderncrates_key_<crate_id>[:options]>
 *  - Reward:  <moderncrates_reward:<crate_id>:<reward_id>[:options]> or <moderncrates_reward_<crate_id>:<reward_id>[:options]>
 *  - VirtualKeys (audience): <moderncrates_virtualkeys:<crate_id>> or <moderncrates_virtualkeys_<crate_id>>
 *
 * Options supported:
 *  - no_sprite / nosprite / no-sprite : disables item icon/sprite
 *  - no_amount / noamount / no-amount : disables item amount display
 *  - name_only / text_only            : disables both sprite and amount (component name only)
 *  - sprite_only / icon_only          : outputs only the sprite icon
 *  - no_brackets / brackets           : controls bracket wrapping
 *  - no_hover / hover                 : controls tooltip hover events
 *  - amount=<n>                       : overrides displayed amount
 */
public class MiniPlaceholdersHook {

    private final ModernCratesPlugin plugin;
    private Expansion expansion;

    public MiniPlaceholdersHook(ModernCratesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the ModernCrates expansion into MiniPlaceholders if available.
     */
    public void register() {
        if (!PlaceholderUtil.isMiniPlaceholdersAvailable()) {
            return;
        }

        try {
            unregister();

            String version = plugin.getPluginMeta().getVersion();
            Expansion.Builder builder = Expansion.builder("moderncrates")
                    .author("UsainSrht")
                    .version(version);

            // 1. Root-level placeholders
            builder.globalPlaceholder("crate", (queue, ctx) -> {
                if (!queue.hasNext()) return null;
                String crateId = queue.pop().value();
                Crate crate = plugin.getCrateRegistry().get(crateId);
                if (crate == null) return null;
                ItemStack item = getCrateItemStack(crate);
                return Tag.selfClosingInserting(formatItem(item, parseOptions(item, queue)));
            });

            builder.globalPlaceholder("key", (queue, ctx) -> {
                if (!queue.hasNext()) return null;
                String crateId = queue.pop().value();
                Crate crate = plugin.getCrateRegistry().get(crateId);
                if (crate == null) return null;
                ItemStack item = getKeyItemStack(plugin, crate);
                return Tag.selfClosingInserting(formatItem(item, parseOptions(item, queue)));
            });

            builder.globalPlaceholder("reward", (queue, ctx) -> {
                if (!queue.hasNext()) return null;
                String crateId = queue.pop().value();
                Crate crate = plugin.getCrateRegistry().get(crateId);
                if (crate == null || !queue.hasNext()) return null;
                String rewardId = queue.pop().value();
                Reward reward = crate.getRewards() != null ? crate.getRewards().get(rewardId) : null;
                if (reward == null) return null;
                ItemStack item = getRewardItemStack(reward, crate);
                return Tag.selfClosingInserting(formatItem(item, parseOptions(item, queue)));
            });

            builder.audiencePlaceholder(Player.class, "virtualkeys", (player, queue, ctx) -> {
                if (!queue.hasNext()) return null;
                String crateId = queue.pop().value();
                int keys = plugin.getVirtualKeyManager().getKeys(player, crateId);
                return Tag.selfClosingInserting(Component.text(keys));
            });

            // 2. Per-crate aliases (e.g. <moderncrates_crate_money>, <moderncrates_reward_money:rewardid>)
            for (Crate crate : plugin.getCrateRegistry().values()) {
                String id = crate.getId();

                builder.globalPlaceholder("crate_" + id, (queue, ctx) -> {
                    ItemStack item = getCrateItemStack(crate);
                    return Tag.selfClosingInserting(formatItem(item, parseOptions(item, queue)));
                });

                builder.globalPlaceholder("key_" + id, (queue, ctx) -> {
                    ItemStack item = getKeyItemStack(plugin, crate);
                    return Tag.selfClosingInserting(formatItem(item, parseOptions(item, queue)));
                });

                builder.globalPlaceholder("reward_" + id, (queue, ctx) -> {
                    if (!queue.hasNext()) return null;
                    String rewardId = queue.pop().value();
                    Reward reward = crate.getRewards() != null ? crate.getRewards().get(rewardId) : null;
                    if (reward == null) return null;
                    ItemStack item = getRewardItemStack(reward, crate);
                    return Tag.selfClosingInserting(formatItem(item, parseOptions(item, queue)));
                });

                builder.audiencePlaceholder(Player.class, "virtualkeys_" + id, (player, queue, ctx) -> {
                    int keys = plugin.getVirtualKeyManager().getKeys(player, id);
                    return Tag.selfClosingInserting(Component.text(keys));
                });
            }

            this.expansion = builder.build();
            this.expansion.register();
            plugin.getLogger().info("Registered MiniPlaceholders expansion with " + plugin.getCrateRegistry().size() + " crates.");
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register MiniPlaceholders expansion: " + t.getMessage());
        }
    }

    /**
     * Unregisters the expansion from MiniPlaceholders if registered.
     */
    public void unregister() {
        if (expansion != null && expansion.registered()) {
            try {
                expansion.unregister();
            } catch (Throwable ignored) {}
            expansion = null;
        }
    }

    /**
     * Creates a standalone Adventure TagResolver that resolves all ModernCrates tags
     * directly within ModernCrates messages, announcements, and menus (even without MiniPlaceholders).
     */
    public TagResolver createTagResolver() {
        return new TagResolver() {
            @Override
            public Tag resolve(String name, ArgumentQueue queue, net.kyori.adventure.text.minimessage.Context ctx) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.startsWith("moderncrates_")) {
                    lower = lower.substring(13);
                } else if (lower.startsWith("mc_")) {
                    lower = lower.substring(3);
                }

                // <moderncrates_crate:crate_id[:options]>
                if (lower.equals("crate") || lower.equals("crate_item")) {
                    if (!queue.hasNext()) return null;
                    String crateId = queue.pop().value();
                    Crate crate = plugin.getCrateRegistry().get(crateId);
                    if (crate == null) return null;
                    ItemStack item = getCrateItemStack(crate);
                    return Tag.selfClosingInserting(formatItem(item, parseOptions(item, queue)));
                }

                // <moderncrates_key:crate_id[:options]>
                if (lower.equals("key") || lower.equals("key_item")) {
                    if (!queue.hasNext()) return null;
                    String crateId = queue.pop().value();
                    Crate crate = plugin.getCrateRegistry().get(crateId);
                    if (crate == null) return null;
                    ItemStack item = getKeyItemStack(plugin, crate);
                    return Tag.selfClosingInserting(formatItem(item, parseOptions(item, queue)));
                }

                // <moderncrates_reward:crate_id:reward_id[:options]>
                if (lower.equals("reward") || lower.equals("reward_item")) {
                    if (!queue.hasNext()) return null;
                    String crateId = queue.pop().value();
                    Crate crate = plugin.getCrateRegistry().get(crateId);
                    if (crate == null || !queue.hasNext()) return null;
                    String rewardId = queue.pop().value();
                    Reward reward = crate.getRewards() != null ? crate.getRewards().get(rewardId) : null;
                    if (reward == null) return null;
                    ItemStack item = getRewardItemStack(reward, crate);
                    return Tag.selfClosingInserting(formatItem(item, parseOptions(item, queue)));
                }

                // <moderncrates_crate_<crate_id>[:options]>
                if (lower.startsWith("crate_")) {
                    String crateId = lower.substring(6);
                    Crate crate = plugin.getCrateRegistry().get(crateId);
                    if (crate == null) return null;
                    ItemStack item = getCrateItemStack(crate);
                    return Tag.selfClosingInserting(formatItem(item, parseOptions(item, queue)));
                }

                // <moderncrates_key_<crate_id>[:options]>
                if (lower.startsWith("key_")) {
                    String crateId = lower.substring(4);
                    Crate crate = plugin.getCrateRegistry().get(crateId);
                    if (crate == null) return null;
                    ItemStack item = getKeyItemStack(plugin, crate);
                    return Tag.selfClosingInserting(formatItem(item, parseOptions(item, queue)));
                }

                // <moderncrates_reward_<crate_id>:reward_id[:options]>
                if (lower.startsWith("reward_")) {
                    String crateId = lower.substring(7);
                    Crate crate = plugin.getCrateRegistry().get(crateId);
                    if (crate == null || !queue.hasNext()) return null;
                    String rewardId = queue.pop().value();
                    Reward reward = crate.getRewards() != null ? crate.getRewards().get(rewardId) : null;
                    if (reward == null) return null;
                    ItemStack item = getRewardItemStack(reward, crate);
                    return Tag.selfClosingInserting(formatItem(item, parseOptions(item, queue)));
                }

                return null;
            }

            @Override
            public boolean has(String name) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.startsWith("moderncrates_")) {
                    lower = lower.substring(13);
                } else if (lower.startsWith("mc_")) {
                    lower = lower.substring(3);
                }
                return lower.equals("crate") || lower.equals("key") || lower.equals("reward")
                        || lower.startsWith("crate_") || lower.startsWith("key_") || lower.startsWith("reward_");
            }
        };
    }

    /**
     * Formats an ItemStack using ItemText, with fallback for environments where
     * RegistryAccess or ItemText might not be initialized (e.g. unit testing).
     */
    public static Component formatItem(ItemStack item, ItemTextOptions options) {
        if (item == null) {
            return Component.empty();
        }
        try {
            return ItemText.format(item, options);
        } catch (Throwable t) {
            Component name = null;
            try {
                if (item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
                    name = item.getItemMeta().displayName();
                }
            } catch (Throwable ignored) {}
            if (name == null) {
                try {
                    name = Component.text(item.getType().name().toLowerCase(Locale.ROOT));
                } catch (Throwable ignored) {
                    name = Component.text("item");
                }
            }
            if (options != null && "<item_sprite>".equals(options.pattern())) {
                return Component.empty();
            }
            return name != null ? name : Component.empty();
        }
    }

    /**
     * Parses ItemText options from remaining arguments in the queue.
     */
    public static ItemTextOptions parseOptions(ItemStack item, ArgumentQueue queue) {
        boolean includeSprite = true;
        boolean includeAmount = true;
        boolean spriteOnly = false;
        Boolean brackets = null;
        Boolean hover = null;
        Integer customAmount = null;

        while (queue != null && queue.hasNext()) {
            String arg = queue.pop().value().toLowerCase(Locale.ROOT);
            switch (arg) {
                case "no_sprite", "nosprite", "no-sprite", "without_sprite", "without-sprite" -> includeSprite = false;
                case "no_amount", "noamount", "no-amount", "without_amount", "without-amount" -> includeAmount = false;
                case "name_only", "nameonly", "name-only", "text_only", "textonly", "text-only" -> {
                    includeSprite = false;
                    includeAmount = false;
                }
                case "sprite_only", "spriteonly", "sprite-only", "icon_only", "icononly", "icon-only" -> spriteOnly = true;
                case "brackets", "with_brackets", "with-brackets" -> brackets = true;
                case "no_brackets", "nobrackets", "no-brackets", "without_brackets", "without-brackets" -> brackets = false;
                case "hover", "with_hover", "with-hover" -> hover = true;
                case "no_hover", "nohover", "no-hover", "without_hover", "without-hover" -> hover = false;
                default -> {
                    if (arg.startsWith("amount=") || arg.startsWith("amount:")) {
                        try {
                            customAmount = Integer.parseInt(arg.substring(7));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        ItemTextOptions.Builder builder = ItemTextOptions.builder();
        int amount = customAmount != null ? customAmount : (item != null ? item.getAmount() : 1);

        if (spriteOnly) {
            builder.pattern("<item_sprite>");
        } else if (!includeSprite && !includeAmount) {
            builder.pattern("<item_displayname>");
        } else if (!includeSprite) {
            if (amount > 1) {
                builder.pattern("<subscript_number> <item_displayname>");
            } else {
                builder.pattern("<item_displayname>");
            }
        } else if (!includeAmount) {
            builder.pattern("<item_sprite> <item_displayname>");
        }

        if (brackets != null) {
            builder.displayBrackets(brackets);
        }
        if (hover != null) {
            builder.hoverEnabled(hover);
        }
        if (customAmount != null) {
            builder.amount(customAmount);
        }
        return builder.build();
    }

    public static ItemStack getCrateItemStack(Crate crate) {
        if (crate == null) return null;
        ItemStack item = null;
        if (crate.getItemConfig() != null && crate.getItemConfig().getItemStack() != null) {
            item = crate.getItemConfig().getItemStack().clone();
        }
        if (item == null) {
            item = ItemBuilder.create("CHEST", "<gold>" + crate.getName(), null);
        }
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && (!meta.hasDisplayName() || meta.displayName() == null)) {
                meta.displayName(TextUtil.parse("<gold>" + crate.getName()));
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    public static ItemStack getKeyItemStack(ModernCratesPlugin plugin, Crate crate) {
        if (crate == null) return null;
        ItemStack key = null;
        if (crate.getKeyConfig() != null && crate.getKeyConfig().getItemStack() != null) {
            key = crate.getKeyConfig().getItemStack().clone();
        }
        if (key == null && plugin != null && plugin.getKeyManager() != null) {
            key = plugin.getKeyManager().createKey(crate, 1);
        }
        if (key == null) {
            key = ItemBuilder.create("TRIPWIRE_HOOK", "<gold>" + crate.getName() + " Key", null);
        }
        if (key != null) {
            ItemMeta meta = key.getItemMeta();
            if (meta != null && (!meta.hasDisplayName() || meta.displayName() == null)) {
                meta.displayName(TextUtil.parse("<gold>" + crate.getName() + " Key"));
                key.setItemMeta(meta);
            }
        }
        return key;
    }

    public static ItemStack getRewardItemStack(Reward reward, Crate crate) {
        if (reward == null) return null;
        return ItemBuilder.fromDisplay(reward, crate);
    }
}
