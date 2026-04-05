package me.usainsrht.moderncrates.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Utility for text formatting using MiniMessage.
 * Supports legacy '&' codes by converting them to MiniMessage format.
 * All parsed components have italic(false) applied so Minecraft's default
 * italic on custom item names/lore is suppressed.
 */
public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private TextUtil() {}

    /**
     * Parses a string that may contain legacy '&' color codes or MiniMessage tags.
     * Applies italic(false) to suppress Minecraft's default italic on custom items.
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        Component parsed;
        if (text.contains("&")) {
            parsed = LEGACY.deserialize(text);
        } else {
            parsed = MINI_MESSAGE.deserialize(text);
        }
        return parsed.decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Parses with placeholder replacement.
     */
    public static Component parse(String text, String... replacements) {
        if (text == null) return Component.empty();
        String processed = text;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            processed = processed.replace(replacements[i], replacements[i + 1]);
        }
        return parse(processed);
    }

    /**
     * Converts a component to a plain string.
     */
    public static String plain(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(component);
    }
}
