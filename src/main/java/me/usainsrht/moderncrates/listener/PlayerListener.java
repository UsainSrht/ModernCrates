package me.usainsrht.moderncrates.listener;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player disconnect cleanup and chat input capture for GUI editors.
 */
public class PlayerListener implements Listener {

    private final ModernCratesPlugin plugin;

    public PlayerListener(ModernCratesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // End session with reward grant â€” the key was already consumed
        var crate = plugin.getAnimationManager().getCrateForSession(player);
        plugin.getAnimationManager().endSession(player, crate);
        plugin.getChatInputManager().cancel(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getChatInputManager().hasPendingInput(player)) return;

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        event.setCancelled(true);

        // Run callback on main thread since GUI operations require it
        plugin.getScheduling().globalRegionalScheduler().run(() -> {
            plugin.getChatInputManager().handleChat(player, message);
        });
    }
}
