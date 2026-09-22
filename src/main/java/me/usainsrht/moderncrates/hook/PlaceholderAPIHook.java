package me.usainsrht.moderncrates.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Hook for PlaceholderAPI integration with graceful fallback.
 */
public final class PlaceholderAPIHook {

    private PlaceholderAPIHook() {}

    /**
     * Checks whether PlaceholderAPI is installed and enabled.
     */
    public static boolean isAvailable() {
        try {
            return Bukkit.getServer() != null
                    && Bukkit.getPluginManager() != null
                    && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Replaces placeholders in the given text for the given player.
     * Uses PlaceholderAPI if available, otherwise resolves standard built-in placeholders.
     */
    public static String setPlaceholders(Player player, String text) {
        if (text == null || text.isEmpty() || player == null) {
            return text;
        }

        if (isAvailable()) {
            try {
                return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
            } catch (Throwable ignored) {
                // Fallback to built-in resolver on error
            }
        }

        return applyBuiltinPlaceholders(player, text);
    }

    /**
     * Fallback resolution for common placeholders when PlaceholderAPI is unavailable.
     */
    public static String applyBuiltinPlaceholders(Player player, String text) {
        if (text == null || player == null || !text.contains("%")) {
            return text;
        }

        String result = text;
        if (result.contains("%player_name%")) {
            result = result.replace("%player_name%", player.getName());
        }
        if (result.contains("%player_uuid%")) {
            result = result.replace("%player_uuid%", player.getUniqueId().toString());
        }
        if (result.contains("%player_level%")) {
            result = result.replace("%player_level%", String.valueOf(player.getLevel()));
        }
        if (result.contains("%player_exp%")) {
            result = result.replace("%player_exp%", String.valueOf(player.getExp()));
        }
        if (result.contains("%player_health%")) {
            result = result.replace("%player_health%", String.valueOf(player.getHealth()));
        }
        if (result.contains("%player_food%")) {
            result = result.replace("%player_food%", String.valueOf(player.getFoodLevel()));
        }
        if (result.contains("%player_gamemode%")) {
            result = result.replace("%player_gamemode%", player.getGameMode().name().toLowerCase(Locale.ROOT));
        }
        if (result.contains("%player_world%")) {
            result = result.replace("%player_world%", player.getWorld().getName());
        }

        // Check for %player_has_permission_<node>%
        if (result.contains("%player_has_permission_")) {
            int startIdx;
            while ((startIdx = result.indexOf("%player_has_permission_")) != -1) {
                int endIdx = result.indexOf("%", startIdx + 23);
                if (endIdx == -1) break;
                String permNode = result.substring(startIdx + 23, endIdx);
                boolean hasPerm = player.hasPermission(permNode);
                result = result.substring(0, startIdx) + hasPerm + result.substring(endIdx + 1);
            }
        }

        return result;
    }
}
