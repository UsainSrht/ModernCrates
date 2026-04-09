package me.usainsrht.moderncrates.hologram;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.HologramConfig;
import org.bukkit.Location;

/**
 * Abstraction for hologram creation/removal.
 * Implementations delegate to different hologram backends.
 */
public interface HologramProvider {

    /**
     * Creates a hologram above a crate.
     *
     * @param crateId unique crate identifier used as the hologram key
     * @param location base block location of the crate
     * @param config hologram lines and offset
     */
    void createHologram(String crateId, Location location, HologramConfig config);

    /**
     * Removes the hologram for a specific crate.
     */
    void removeHologram(String crateId);

    /**
     * Removes all holograms managed by this provider.
     */
    void removeAll();
}
