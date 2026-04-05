package me.usainsrht.moderncrates.manager;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.HologramConfig;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages holograms above physical crates using TextDisplay entities.
 */
public class HologramManager {

    private final Map<String, List<TextDisplay>> holograms = new ConcurrentHashMap<>();

    public void createHologram(Crate crate) {
        if (crate.getCrateLocation() == null || crate.getHologramConfig() == null) return;

        Location baseLoc = crate.getCrateLocation().toBukkit();
        if (baseLoc == null || baseLoc.getWorld() == null) return;

        HologramConfig config = crate.getHologramConfig();
        double x = baseLoc.getX() + config.getOffsetX();
        double y = baseLoc.getY() + config.getOffsetY();
        double z = baseLoc.getZ() + config.getOffsetZ();

        List<TextDisplay> displays = new ArrayList<>();

        if (config.getLines() != null) {
            for (int i = 0; i < config.getLines().size(); i++) {
                Location loc = new Location(baseLoc.getWorld(), x, y - (i * 0.3), z);
                TextDisplay display = (TextDisplay) baseLoc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
                display.text(TextUtil.parse(config.getLines().get(i)));
                display.setBillboard(Display.Billboard.CENTER);
                display.setPersistent(false);
                displays.add(display);
            }
        }

        holograms.put(crate.getId(), displays);
    }

    public void removeHologram(String crateId) {
        List<TextDisplay> displays = holograms.remove(crateId);
        if (displays != null) {
            displays.forEach(TextDisplay::remove);
        }
    }

    public void removeAll() {
        holograms.values().forEach(list -> list.forEach(TextDisplay::remove));
        holograms.clear();
    }

    public void refreshAll(Map<String, Crate> crates) {
        removeAll();
        crates.values().forEach(this::createHologram);
    }
}
