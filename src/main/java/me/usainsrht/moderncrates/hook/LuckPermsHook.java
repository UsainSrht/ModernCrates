package me.usainsrht.moderncrates.hook;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Hook for LuckPerms metadata settings check.
 */
public final class LuckPermsHook {

    private LuckPermsHook() {}

    /**
     * Checks whether LuckPerms is installed and enabled on the server.
     */
    public static boolean isAvailable() {
        try {
            return Bukkit.getServer() != null
                    && Bukkit.getPluginManager() != null
                    && Bukkit.getPluginManager().isPluginEnabled("LuckPerms");
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Checks if the given player has the specified metadata key set indicating
     * that crate announcements should not be sent to them.
     *
     * @param player  The player to check.
     * @param metaKey The LuckPerms meta key.
     * @return {@code true} if the meta tag is present and set to true/non-false; {@code false} otherwise.
     */
    public static boolean isMuted(Player player, String metaKey) {
        if (player == null || metaKey == null || metaKey.isBlank()) {
            return false;
        }

        if (!isAvailable()) {
            return false;
        }

        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            CachedMetaData metaData = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
            String val = metaData.getMetaValue(metaKey);
            if (val != null) {
                String trimmed = val.trim();
                return "true".equalsIgnoreCase(trimmed) || (!trimmed.isEmpty() && !"false".equalsIgnoreCase(trimmed));
            }
        } catch (Throwable ignored) {
            // Fail-safe if LuckPerms API encounters an issue or is absent
        }

        return false;
    }
}
