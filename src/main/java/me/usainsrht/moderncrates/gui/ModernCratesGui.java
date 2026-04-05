package me.usainsrht.moderncrates.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Base interface for all ModernCrates GUI screens.
 * Extends InventoryHolder so click/close events can be dispatched
 * by casting {@code event.getInventory().getHolder()}.
 */
public interface ModernCratesGui extends InventoryHolder {

    /**
     * Handle a click inside this GUI's inventory.
     * The event is already cancelled by the listener before this is called.
     */
    void handleClick(InventoryClickEvent event);

    /**
     * Handle this GUI's inventory being closed.
     * Default implementation does nothing.
     */
    default void handleClose(InventoryCloseEvent event) {}
}
