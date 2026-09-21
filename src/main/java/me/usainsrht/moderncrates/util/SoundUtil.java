package me.usainsrht.moderncrates.util;

import me.usainsrht.yamlmessage.YamlMessage;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Utility for playing sounds exclusively created and dispatched via {@link YamlMessage}.
 */
public final class SoundUtil {

    private SoundUtil() {}

    /**
     * Plays a configured sound message to the target audience.
     */
    public static void play(@Nullable Audience audience, @Nullable YamlMessage soundMessage) {
        if (audience == null || soundMessage == null || soundMessage.isEmpty()) return;
        soundMessage.send(audience);
    }

    /**
     * Plays a list of raw sound strings/configurations to the target audience via YamlMessage.
     */
    public static void play(@Nullable Audience audience, @Nullable List<String> sounds) {
        if (audience == null || sounds == null || sounds.isEmpty()) return;
        YamlMessage message = YamlMessage.parse(Map.of("sounds", sounds));
        play(audience, message);
    }

    /**
     * Plays a single raw sound string/configuration to the target audience via YamlMessage.
     */
    public static void play(@Nullable Audience audience, @Nullable String soundName) {
        if (audience == null || soundName == null || soundName.isBlank()) return;
        YamlMessage message = YamlMessage.parse(Map.of("sounds", soundName));
        play(audience, message);
    }
}


