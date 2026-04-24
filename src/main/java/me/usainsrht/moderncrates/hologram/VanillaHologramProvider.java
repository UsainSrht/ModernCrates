package me.usainsrht.moderncrates.hologram;

import me.usainsrht.moderncrates.api.crate.HologramConfig;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hologram provider using vanilla Paper TextDisplay entities.
 * Requires Paper 1.19.4+ (display entity support).
 * Uses region-specific scheduling for Folia compatibility.
 */
public class VanillaHologramProvider implements HologramProvider {

    private final Plugin plugin;
    private final GracefulScheduling scheduling;
    private final Map<String, List<TextDisplay>> holograms = new ConcurrentHashMap<>();

    public VanillaHologramProvider(Plugin plugin, GracefulScheduling scheduling) {
        this.plugin = plugin;
        this.scheduling = scheduling;
    }

    @Override
    public void createHologram(String crateId, Location location, HologramConfig config) {
        if (config.getLines() == null || config.getLines().isEmpty()) {
            holograms.put(crateId, new ArrayList<>());
            return;
        }

        double x = location.getX() + config.getOffsetX();
        double y = location.getY() + config.getOffsetY();
        double z = location.getZ() + config.getOffsetZ();

        List<String> lines = new ArrayList<>(config.getLines());

        scheduling.regionSpecificScheduler(location).run(() -> {
            List<TextDisplay> displays = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                final String line = lines.get(i);
                Location loc = new Location(location.getWorld(), x, y - (i * 0.3), z);
                TextDisplay display = location.getWorld().spawn(loc, TextDisplay.class, d -> {
                    d.text(TextUtil.parse(line));
                    d.setBillboard(Display.Billboard.CENTER);
                    d.setPersistent(false);
                });
                displays.add(display);
            }
            holograms.put(crateId, displays);
        });
    }

    @Override
    public void removeHologram(String crateId) {
        List<TextDisplay> displays = holograms.remove(crateId);
        if (displays != null) {
            removeDisplays(displays);
        }
    }

    @Override
    public void removeAll() {
        List<List<TextDisplay>> all = new ArrayList<>(holograms.values());
        holograms.clear();
        for (List<TextDisplay> displays : all) {
            removeDisplays(displays);
        }
    }

    private void removeDisplays(List<TextDisplay> displays) {
        for (TextDisplay display : displays) {
            if (!display.isDead()) {
                if (!plugin.isEnabled()) {
                    safelyRemoveDisplay(display);
                    continue;
                }

                try {
                    scheduling.entitySpecificScheduler(display).run(t -> safelyRemoveDisplay(display), null);
                } catch (IllegalPluginAccessException ignored) {
                    // Shutdown race: plugin flipped disabled between the enabled check and scheduler registration.
                    safelyRemoveDisplay(display);
                }
            }
        }
    }

    private void safelyRemoveDisplay(TextDisplay display) {
        try {
            display.remove();
        } catch (RuntimeException ignored) {
            // Best-effort cleanup during shutdown/reload.
        }
    }
}
