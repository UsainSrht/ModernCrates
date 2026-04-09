package me.usainsrht.moderncrates.manager;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateLocation;
import me.usainsrht.moderncrates.hologram.HologramProvider;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages holograms above physical crates.
 * Delegates to the configured {@link HologramProvider} implementation.
 * Supports multiple locations per crate.
 */
public class HologramManager {

    private HologramProvider provider;
    /** Tracks the hologram IDs spawned for each crate (keyed by crate ID). */
    private final Map<String, List<String>> crateHologramIds = new HashMap<>();

    public HologramManager() {
        this.provider = null;
    }

    public HologramManager(HologramProvider provider) {
        this.provider = provider;
    }

    public void setProvider(HologramProvider provider) {
        this.provider = provider;
    }

    public HologramProvider getProvider() {
        return provider;
    }

    public void createHologram(Crate crate) {
        if (provider == null) return;
        List<CrateLocation> locations = crate.getCrateLocations();
        if (locations.isEmpty() || crate.getHologramConfig() == null) return;

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < locations.size(); i++) {
            Location baseLoc = locations.get(i).toBukkit();
            if (baseLoc == null || baseLoc.getWorld() == null) continue;
            String holoId = crate.getId() + "_loc_" + i;
            provider.createHologram(holoId, baseLoc, crate.getHologramConfig());
            ids.add(holoId);
        }
        crateHologramIds.put(crate.getId(), ids);
    }

    public void removeHologram(String crateId) {
        if (provider == null) return;
        List<String> ids = crateHologramIds.remove(crateId);
        if (ids != null) {
            for (String id : ids) {
                provider.removeHologram(id);
            }
        } else {
            // Fallback for any legacy single-ID hologram
            provider.removeHologram(crateId);
        }
    }

    public void removeAll() {
        if (provider == null) return;
        crateHologramIds.clear();
        provider.removeAll();
    }

    public void refreshAll(Map<String, Crate> crates) {
        removeAll();
        crates.values().forEach(this::createHologram);
    }
}
