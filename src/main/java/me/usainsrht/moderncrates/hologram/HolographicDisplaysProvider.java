package me.usainsrht.moderncrates.hologram;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.usainsrht.moderncrates.api.crate.HologramConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hologram provider using HolographicDisplays plugin.
 * Requires HolographicDisplays 3.x to be installed on the server.
 */
public class HolographicDisplaysProvider implements HologramProvider {

    private final HolographicDisplaysAPI api;
    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();

    public HolographicDisplaysProvider(Plugin plugin) {
        this.api = HolographicDisplaysAPI.get(plugin);
    }

    @Override
    public void createHologram(String crateId, Location location, HologramConfig config) {
        double x = location.getX() + config.getOffsetX();
        double y = location.getY() + config.getOffsetY();
        double z = location.getZ() + config.getOffsetZ();
        Location holoLoc = new Location(location.getWorld(), x, y, z);

        Hologram hologram = api.createHologram(holoLoc);

        if (config.getLines() != null) {
            for (String line : config.getLines()) {
                String legacy = LegacyComponentSerializer.legacySection().serialize(TextUtil.parse(line));
                hologram.getLines().appendText(legacy);
            }
        }

        holograms.put(crateId, hologram);
    }

    @Override
    public void removeHologram(String crateId) {
        Hologram hologram = holograms.remove(crateId);
        if (hologram != null) {
            hologram.delete();
        }
    }

    @Override
    public void removeAll() {
        holograms.values().forEach(Hologram::delete);
        holograms.clear();
    }
}
