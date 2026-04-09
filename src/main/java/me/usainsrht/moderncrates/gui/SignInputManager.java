package me.usainsrht.moderncrates.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages sign-based text input for GUI editors.
 * When a GUI needs text input, it places a temporary sign, opens the sign editor,
 * and this manager captures the completed sign text to feed back to the callback.
 */
public class SignInputManager {

    private final Map<UUID, Consumer<String[]>> pendingInputs = new ConcurrentHashMap<>();

    /**
     * Register a pending sign input for a player.
     * The next sign change from this player will be captured and lines passed to the callback.
     */
    public void awaitInput(Player player, Consumer<String[]> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
    }

    /**
     * Try to handle a sign change as pending input.
     *
     * @return true if the sign change was consumed as input
     */
    public boolean handleSignChange(Player player, Sign sign, Side side) {
        Consumer<String[]> callback = pendingInputs.remove(player.getUniqueId());
        if (callback != null) {
            String[] lines = new String[4];
            for (int i = 0; i < 4; i++) {
                lines[i] = PlainTextComponentSerializer.plainText().serialize(sign.getSide(side).line(i));
            }
            callback.accept(lines);
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
