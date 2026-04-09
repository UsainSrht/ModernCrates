package me.usainsrht.moderncrates.hologram;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import me.usainsrht.moderncrates.api.crate.HologramConfig;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hologram provider using FancyHolograms plugin.
 * Requires FancyHolograms 2.x to be installed on the server.
 * FancyHolograms supports MiniMessage natively, so no conversion is needed.
 */
public class FancyHologramsProvider implements HologramProvider {

    private static final String PREFIX = "moderncrates_";
    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();

    @Override
    public void createHologram(String crateId, Location location, HologramConfig config) {
        double x = location.getX() + config.getOffsetX();
        double y = location.getY() + config.getOffsetY();
        double z = location.getZ() + config.getOffsetZ();
        Location holoLoc = new Location(location.getWorld(), x, y, z);

        String name = PREFIX + crateId;

        TextHologramData data = new TextHologramData(name, holoLoc);
        List<String> lines = new ArrayList<>();
        if (config.getLines() != null) {
            lines.addAll(config.getLines());
        }
        data.setText(lines);

        var manager = FancyHologramsPlugin.get().getHologramManager();
        Hologram hologram = manager.create(data);
        manager.addHologram(hologram);

        holograms.put(crateId, hologram);
    }

    @Override
    public void removeHologram(String crateId) {
        Hologram hologram = holograms.remove(crateId);
        if (hologram != null) {
            var manager = FancyHologramsPlugin.get().getHologramManager();
            manager.removeHologram(hologram);
        }
    }

    @Override
    public void removeAll() {
        var manager = FancyHologramsPlugin.get().getHologramManager();
        holograms.values().forEach(manager::removeHologram);
        holograms.clear();
    }
}
