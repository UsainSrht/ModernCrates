package me.usainsrht.moderncrates.hologram;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import me.usainsrht.moderncrates.api.crate.HologramConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hologram provider using DecentHolograms plugin.
 * Requires DecentHolograms to be installed on the server.
 */
public class DecentHologramsProvider implements HologramProvider {

    private static final String PREFIX = "moderncrates_";
    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();

    @Override
    public void createHologram(String crateId, Location location, HologramConfig config) {
        double x = location.getX() + config.getOffsetX();
        double y = location.getY() + config.getOffsetY();
        double z = location.getZ() + config.getOffsetZ();
        Location holoLoc = new Location(location.getWorld(), x, y, z);

        List<String> legacyLines = new ArrayList<>();
        if (config.getLines() != null) {
            for (String line : config.getLines()) {
                legacyLines.add(LegacyComponentSerializer.legacySection().serialize(TextUtil.parse(line)));
            }
        }

        String name = PREFIX + crateId;
        Hologram hologram = DHAPI.createHologram(name, holoLoc, legacyLines);
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
