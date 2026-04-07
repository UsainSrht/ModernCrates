package me.usainsrht.moderncrates.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for playing sounds from configuration strings.
 */
public final class SoundUtil {

    private static final Map<String, String> LEGACY_ALIASES = Map.of();
    private static final Set<String> WARNED_INVALID_SOUNDS = ConcurrentHashMap.newKeySet();

    private SoundUtil() {}

    public static void play(Player player, List<String> sounds) {
        if (sounds == null || sounds.isEmpty() || player == null) return;
        for (String soundName : sounds) {
            playInternal(player, soundName);
        }
    }

    public static void play(Player player, String soundName) {
        if (soundName == null || player == null) return;
        play(player, List.of(soundName));
    }

    private static void playInternal(Player player, String rawSoundName) {
        if (rawSoundName == null) return;
        String soundName = rawSoundName.trim();
        if (soundName.isEmpty()) return;

        for (String candidate : resolveCandidates(soundName)) {
            try {
                player.playSound(player.getLocation(), candidate, 1.0f, 1.0f);
                return;
            } catch (IllegalArgumentException ignored) {
                // Try next candidate.
            }
        }

        String warningKey = soundName.toLowerCase(Locale.ROOT);
        if (WARNED_INVALID_SOUNDS.add(warningKey)) {
            Bukkit.getLogger().warning("[ModernCrates] Unknown sound key in config: " + soundName);
        }
    }

    private static List<String> resolveCandidates(String soundName) {
        String normalized = LEGACY_ALIASES.getOrDefault(soundName.toUpperCase(Locale.ROOT), soundName);
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(normalized);
        candidates.add(normalized.toLowerCase(Locale.ROOT));

        if (!normalized.contains(":") && !normalized.contains(".")) {
            String key = normalized.toLowerCase(Locale.ROOT).replace('_', '.');
            candidates.add(key);
            candidates.add("minecraft:" + key);
        }

        return new ArrayList<>(candidates);
    }
}

