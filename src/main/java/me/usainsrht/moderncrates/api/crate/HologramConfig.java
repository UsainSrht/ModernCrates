package me.usainsrht.moderncrates.api.crate;

import java.util.List;

/**
 * Configuration for the hologram displayed above a physical crate.
 */
public class HologramConfig {

    private List<String> lines;
    private double offsetX;
    private double offsetY;
    private double offsetZ;

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public void setOffsetZ(double offsetZ) {
        this.offsetZ = offsetZ;
    }
}
