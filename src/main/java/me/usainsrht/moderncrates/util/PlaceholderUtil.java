package me.usainsrht.moderncrates.util;

import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.MiniPlaceholders;
import io.github.miniplaceholders.api.placeholder.AudiencePlaceholder;
import io.github.miniplaceholders.api.resolver.AudienceTagResolver;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.hook.LuckPermsHook;
import me.usainsrht.moderncrates.util.TextUtil;
import me.usainsrht.yamlmessage.YamlMessage;
import me.usainsrht.yamlmessage.model.SoundData;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility for constructing Adventure MiniMessage {@link TagResolver}s,
 * including MiniPlaceholders (global and audience placeholders) when available.
 */
public final class PlaceholderUtil {

    private PlaceholderUtil() {}

    /**
     * Checks if MiniPlaceholders is installed and enabled.
     */
    public static boolean isMiniPlaceholdersAvailable() {
        try {
            return Bukkit.getServer() != null
                    && Bukkit.getPluginManager() != null
                    && Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders");
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Creates a parsed string placeholder resolver: &lt;key&gt; -> value.
     */
    public static @NotNull TagResolver parsed(@NotNull String key, @Nullable String value) {
        return Placeholder.parsed(key, value != null ? value : "");
    }

    /**
     * Creates a component placeholder resolver: &lt;key&gt; -> component.
     */
    public static @NotNull TagResolver component(@NotNull String key, @Nullable Component component) {
        return Placeholder.component(key, component != null ? component : Component.empty());
    }

    /**
     * Combines custom tag resolvers with global and audience-aware MiniPlaceholders resolvers (if available).
     *
     * @param audience        The target audience (can be null for global context).
     * @param customResolvers Custom resolvers to include.
     * @return Array of combined {@link TagResolver}s.
     */
    public static @NotNull TagResolver[] combine(@Nullable Audience audience, @NotNull TagResolver... customResolvers) {
        List<TagResolver> resolvers = new ArrayList<>(customResolvers.length + 3);

        for (TagResolver resolver : customResolvers) {
            if (resolver != null) {
                resolvers.add(resolver);
            }
        }

        if (isMiniPlaceholdersAvailable()) {
            try {
                TagResolver global = MiniPlaceholders.globalPlaceholders();
                if (global != null) {
                    resolvers.add(global);
                }
            } catch (Throwable ignored) {}

            if (audience != null) {
                try {
                    TagResolver audienceBound = getAudienceBoundResolver(audience);
                    if (audienceBound != null) {
                        resolvers.add(audienceBound);
                    }
                } catch (Throwable ignored) {}
            }
        }

        return resolvers.toArray(new TagResolver[0]);
    }

    /**
     * Combines custom tag resolvers with MiniPlaceholders global and audience resolvers.
     */
    public static @NotNull TagResolver[] combine(@NotNull TagResolver... customResolvers) {
        return combine(null, customResolvers);
    }

    private static @Nullable TagResolver getAudienceBoundResolver(@NotNull Audience audience) {
        return new TagResolver() {
            @Override
            public net.kyori.adventure.text.minimessage.tag.Tag resolve(
                    @NotNull String name,
                    @NotNull net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue args,
                    @NotNull net.kyori.adventure.text.minimessage.Context ctx) {
                try {
                    Collection<Expansion> expansions = MiniPlaceholders.expansionsAvailable();
                    if (expansions == null) return null;
                    for (Expansion expansion : expansions) {
                        if (expansion == null) continue;
                        Collection<AudiencePlaceholder<?>> placeholders = expansion.registeredAudiencePlaceholders();
                        if (placeholders == null) continue;
                        for (AudiencePlaceholder<?> placeholder : placeholders) {
                            if (placeholder != null && placeholder.has(name)) {
                                Class<?> targetClass = placeholder.targetClass();
                                if (targetClass != null && !targetClass.isInstance(audience)) {
                                    continue;
                                }
                                try {
                                    @SuppressWarnings("unchecked")
                                    AudienceTagResolver<Audience> res = (AudienceTagResolver<Audience>) placeholder.resolver();
                                    if (res != null) {
                                        return res.tag(audience, args, ctx);
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                return null;
            }

            @Override
            public boolean has(@NotNull String name) {
                try {
                    Collection<Expansion> expansions = MiniPlaceholders.expansionsAvailable();
                    if (expansions == null) return false;
                    for (Expansion expansion : expansions) {
                        if (expansion == null) continue;
                        Collection<AudiencePlaceholder<?>> placeholders = expansion.registeredAudiencePlaceholders();
                        if (placeholders == null) continue;
                        for (AudiencePlaceholder<?> placeholder : placeholders) {
                            if (placeholder != null && placeholder.has(name)) {
                                Class<?> targetClass = placeholder.targetClass();
                                if (targetClass != null && !targetClass.isInstance(audience)) {
                                    continue;
                                }
                                return true;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                return false;
            }
        };
    }

    /**
     * Builds standard crate & player resolvers.
     */
    public static @NotNull TagResolver[] crateResolvers(@Nullable Player player, @Nullable Crate crate) {
        List<TagResolver> list = new ArrayList<>();
        if (player != null) {
            list.add(parsed("player", player.getName()));
            list.add(component("player_display", player.displayName()));
        }
        if (crate != null) {
            list.add(parsed("crate", crate.getName()));
            list.add(parsed("crate_id", crate.getId()));
            list.add(component("crate_name", TextUtil.parse(crate.getName())));
        }
        return combine(player, list.toArray(new TagResolver[0]));
    }

    /**
     * Builds standard crate, reward, & player resolvers.
     */
    public static @NotNull TagResolver[] rewardResolvers(
            @Nullable Player player,
            @Nullable Crate crate,
            @Nullable Reward reward,
            @Nullable Component rewardDisplayName) {
        List<TagResolver> list = new ArrayList<>();
        if (player != null) {
            list.add(parsed("player", player.getName()));
            list.add(component("player_display", player.displayName()));
        }
        if (crate != null) {
            list.add(parsed("crate", crate.getName()));
            list.add(parsed("crate_id", crate.getId()));
            list.add(component("crate_name", TextUtil.parse(crate.getName())));
        }
        if (reward != null) {
            list.add(parsed("reward_id", reward.getId()));
        }
        if (rewardDisplayName != null) {
            list.add(component("reward_name", rewardDisplayName));
            list.add(parsed("reward", TextUtil.plain(rewardDisplayName)));
        }
        return combine(player, list.toArray(new TagResolver[0]));
    }

    /**
     * Dispatches a {@link YamlMessage} to an {@link Audience}, avoiding the Player/OfflinePlayer overload ambiguity.
     */
    public static void send(@Nullable YamlMessage message, @Nullable Audience audience, @Nullable String prefix, @NotNull TagResolver... resolvers) {
        if (message == null || message.isEmpty() || audience == null) return;
        TagResolver[] combined = combine(audience, resolvers);
        message.send(audience, prefix, combined);
    }

    /**
     * Dispatches a {@link YamlMessage} to an {@link Audience}, avoiding the Player/OfflinePlayer overload ambiguity.
     */
    public static void send(@Nullable YamlMessage message, @Nullable Audience audience, @NotNull TagResolver... resolvers) {
        if (message == null || message.isEmpty() || audience == null) return;
        TagResolver[] combined = combine(audience, resolvers);
        message.send(audience, combined);
    }

    /**
     * Dispatches pre-parsed Adventure components and sounds to a single {@link Audience}.
     */
    public static void sendParsed(
            @Nullable Audience audience,
            @NotNull List<Component> chatComponents,
            @Nullable Component actionbarComponent,
            @Nullable Title title,
            @NotNull List<Sound> sounds) {
        if (audience == null) return;
        for (Component component : chatComponents) {
            audience.sendMessage(component);
        }
        if (actionbarComponent != null) {
            audience.sendActionBar(actionbarComponent);
        }
        if (title != null) {
            audience.showTitle(title);
        }
        for (Sound sound : sounds) {
            audience.playSound(sound);
        }
    }

    /**
     * Broadcasts an announcement parsed ONCE using the opener's context and resolvers,
     * delivering the pre-parsed components to the opener, console, and unmuted viewers.
     *
     * @param message    The {@link YamlMessage} announcement.
     * @param opener     The player opening the crate.
     * @param toEveryone Whether the announcement should be broadcast to console and online players.
     * @param prefix     Optional message prefix.
     * @param muteKey    Metadata key used to check if viewers muted crate announcements.
     * @param resolvers  Custom tag resolvers for the announcement.
     */
    public static void broadcastAnnouncement(
            @Nullable YamlMessage message,
            @NotNull Player opener,
            boolean toEveryone,
            @Nullable String prefix,
            @Nullable String muteKey,
            @NotNull TagResolver... resolvers) {
        if (message == null || message.isEmpty() || opener == null) return;

        TagResolver[] combined = combine(opener, resolvers);

        List<Component> chatComponents = new ArrayList<>();
        if (message.chat() != null && !message.chat().isEmpty()) {
            boolean hasPrefix = prefix != null && !prefix.isEmpty();
            for (String raw : message.chat()) {
                String formatted = hasPrefix ? prefix + raw : raw;
                chatComponents.add(MiniMessage.miniMessage().deserialize(formatted, combined));
            }
        }

        Component actionbarComponent = null;
        if (message.actionbar() != null && !message.actionbar().isEmpty()) {
            actionbarComponent = MiniMessage.miniMessage().deserialize(message.actionbar(), combined);
        }

        Title title = null;
        if (message.title() != null) {
            title = message.title().toTitle(combined);
        }

        List<Sound> sounds = new ArrayList<>();
        if (message.sounds() != null && !message.sounds().isEmpty()) {
            for (SoundData soundData : message.sounds()) {
                if (soundData != null) {
                    sounds.add(soundData.toSound());
                }
            }
        }

        // Opener always sees their own announcement
        sendParsed(opener, chatComponents, actionbarComponent, title, sounds);

        if (!toEveryone) return;

        // Console receives announcement
        if (Bukkit.getServer() != null) {
            sendParsed(Bukkit.getConsoleSender(), chatComponents, actionbarComponent, title, sounds);

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.getUniqueId().equals(opener.getUniqueId())) continue;
                if (muteKey != null && !muteKey.isBlank() && LuckPermsHook.isMuted(viewer, muteKey)) continue;

                sendParsed(viewer, chatComponents, actionbarComponent, title, sounds);
            }
        }
    }
}
