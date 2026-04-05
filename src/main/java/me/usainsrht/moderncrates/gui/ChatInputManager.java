package me.usainsrht.moderncrates.gui;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages pending chat-based text inputs for GUI editors.
 * When a GUI needs text input, it closes the inventory, prompts the player,
 * and this manager captures the next chat message to feed back to the callback.
 */
public class ChatInputManager {

    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();

    /**
     * Register a pending text input for a player.
     * The next chat message from this player will be captured and passed to the callback.
     */
    public void awaitInput(Player player, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
    }

    /**
     * Try to handle a chat message as pending input.
     *
     * @return true if the message was consumed as input
     */
    public boolean handleChat(Player player, String message) {
        Consumer<String> callback = pendingInputs.remove(player.getUniqueId());
        if (callback != null) {
            callback.accept(message);
            return true;
        }
        return false;
    }

    /**
     * Cancel any pending input for a player.
     */
    public void cancel(Player player) {
        pendingInputs.remove(player.getUniqueId());
    }

    /**
     * Check if a player has a pending input.
     */
    public boolean hasPendingInput(Player player) {
        return pendingInputs.containsKey(player.getUniqueId());
    }

    public void clear() {
        pendingInputs.clear();
    }
}
