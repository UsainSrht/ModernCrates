package me.usainsrht.moderncrates.listener;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.gui.ModernCratesGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Handles GUI click and close events using InventoryHolder dispatch.
 * All ModernCrates GUIs implement ModernCratesGui (extends InventoryHolder),
 * so click/close events are dispatched by casting the holder.
 */
public class GuiListener implements Listener {

    private final ModernCratesPlugin plugin;

    public GuiListener(ModernCratesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof ModernCratesGui gui) {
            event.setCancelled(true);
            if (event.getRawSlot() >= 0 && event.getRawSlot() < event.getInventory().getSize()) {
                gui.handleClick(event);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof ModernCratesGui gui) {
            gui.handleClose(event);
        }

        // Handle animation session close
        if (holder instanceof AnimationSession) {
            if (event.getReason() == InventoryCloseEvent.Reason.OPEN_NEW) {
                return; // Session is transitioning (e.g. click animation title update)
            }
            // Always end session and grant reward.
            // handleClose (called above) already determined the reward for early closes.
            var crate = plugin.getAnimationManager().getCrateForSession(player);
            plugin.getAnimationManager().endSession(player, crate);
        }
    }
}
