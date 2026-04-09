package me.usainsrht.moderncrates.manager;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.hologram.HologramProvider;
import me.usainsrht.moderncrates.hologram.VanillaHologramProvider;
import org.bukkit.Location;

import java.util.Map;

/**
 * Manages holograms above physical crates.
 * Delegates to the configured {@link HologramProvider} implementation.
 */
public class HologramManager {

    private HologramProvider provider;

    public HologramManager() {
        this.provider = new VanillaHologramProvider();
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
        if (crate.getCrateLocation() == null || crate.getHologramConfig() == null) return;

        Location baseLoc = crate.getCrateLocation().toBukkit();
        if (baseLoc == null || baseLoc.getWorld() == null) return;

        provider.createHologram(crate.getId(), baseLoc, crate.getHologramConfig());
    }

    public void removeHologram(String crateId) {
        provider.removeHologram(crateId);
    }

    public void removeAll() {
        provider.removeAll();
    }

    public void refreshAll(Map<String, Crate> crates) {
        removeAll();
        crates.values().forEach(this::createHologram);
    }
}
