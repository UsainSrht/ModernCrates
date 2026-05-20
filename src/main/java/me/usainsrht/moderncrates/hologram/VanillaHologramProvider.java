package me.usainsrht.moderncrates.hologram;

import me.usainsrht.moderncrates.api.crate.HologramConfig;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
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
        float scale = config.getScale() <= 0 ? 1.0f : config.getScale();
        boolean seeThrough = config.isSeeThrough();
        int bgColor = config.getBackgroundColor();

        Display.Billboard billboard;
        try {
            billboard = Display.Billboard.valueOf(
                    config.getBillboard() != null ? config.getBillboard().toUpperCase() : "CENTER");
        } catch (IllegalArgumentException e) {
            billboard = Display.Billboard.CENTER;
        }

        final Display.Billboard finalBillboard = billboard;
        final float finalScale = scale;

        scheduling.regionSpecificScheduler(location).run(() -> {
            List<TextDisplay> displays = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                final String line = lines.get(i);
                Location loc = new Location(location.getWorld(), x, y - (i * 0.3 * finalScale), z);
                TextDisplay display = location.getWorld().spawn(loc, TextDisplay.class, d -> {
                    d.text(TextUtil.parse(line));
                    d.setBillboard(finalBillboard);
                    d.setSeeThrough(seeThrough);
                    d.setPersistent(false);
                    if (bgColor == -1) {
                        d.setDefaultBackground(true);
                    } else {
                        d.setDefaultBackground(false);
                        d.setBackgroundColor(Color.fromARGB(
                                (bgColor >> 24) & 0xFF,
                                (bgColor >> 16) & 0xFF,
                                (bgColor >> 8) & 0xFF,
                                bgColor & 0xFF));
                    }
                    if (finalScale != 1.0f) {
                        d.setTransformation(new Transformation(
                                new Vector3f(0f, 0f, 0f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(finalScale, finalScale, finalScale),
                                new AxisAngle4f(0, 0, 1, 0)));
                    }
                });
                displays.add(display);
            }
            holograms.put(crateId, displays);
        });
    }

    @Override
    public void removeHologram(String crateId) {
        List<TextDisplay> displays = holograms.remove(crateId);
        if (displays != null) removeDisplays(displays);
    }

    @Override
    public void removeAll() {
        List<List<TextDisplay>> all = new ArrayList<>(holograms.values());
        holograms.clear();
        for (List<TextDisplay> displays : all) removeDisplays(displays);
    }

    private void removeDisplays(List<TextDisplay> displays) {
        for (TextDisplay display : displays) {
            if (!display.isDead()) {
                if (!plugin.isEnabled()) { safelyRemoveDisplay(display); continue; }
                try {
                    scheduling.entitySpecificScheduler(display).run(t -> safelyRemoveDisplay(display), null);
                } catch (IllegalPluginAccessException ignored) {
                    safelyRemoveDisplay(display);
                }
            }
        }
    }

    private void safelyRemoveDisplay(TextDisplay display) {
        try { display.remove(); } catch (RuntimeException ignored) {}
    }
}
