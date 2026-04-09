package me.usainsrht.moderncrates.listener;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player disconnect cleanup.
 */
public class PlayerListener implements Listener {

    private final ModernCratesPlugin plugin;

    public PlayerListener(ModernCratesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        var crate = plugin.getAnimationManager().getCrateForSession(player);
        plugin.getAnimationManager().endSession(player, crate);
    }
}