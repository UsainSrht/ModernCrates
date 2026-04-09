package me.usainsrht.moderncrates.hologram;

import me.usainsrht.moderncrates.api.crate.HologramConfig;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hologram provider using vanilla Paper TextDisplay entities.
 * Requires Paper 1.19.4+ (display entity support).
 */
public class VanillaHologramProvider implements HologramProvider {

    private final Map<String, List<TextDisplay>> holograms = new ConcurrentHashMap<>();

    @Override
    public void createHologram(String crateId, Location location, HologramConfig config) {
        double x = location.getX() + config.getOffsetX();
        double y = location.getY() + config.getOffsetY();
        double z = location.getZ() + config.getOffsetZ();

        List<TextDisplay> displays = new ArrayList<>();

        if (config.getLines() != null) {
            for (int i = 0; i < config.getLines().size(); i++) {
                Location loc = new Location(location.getWorld(), x, y - (i * 0.3), z);
                TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
                display.text(TextUtil.parse(config.getLines().get(i)));
                display.setBillboard(Display.Billboard.CENTER);
                display.setPersistent(false);
                displays.add(display);
            }
        }

        holograms.put(crateId, displays);
    }

    @Override
    public void removeHologram(String crateId) {
        List<TextDisplay> displays = holograms.remove(crateId);
        if (displays != null) {
            displays.forEach(TextDisplay::remove);
        }
    }

    @Override
    public void removeAll() {
        holograms.values().forEach(list -> list.forEach(TextDisplay::remove));
        holograms.clear();
    }
}
