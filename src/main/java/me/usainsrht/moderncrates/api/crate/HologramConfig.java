package me.usainsrht.moderncrates.api.crate;

import java.util.List;

/**
 * Configuration for the hologram displayed above a physical crate.
 */
public class HologramConfig {

    private List<String> lines;
    private double offsetX;
    private double offsetY = 2.5;
    private double offsetZ;

    // Display options (vanilla TextDisplay)
    private float scale = 1.0f;
    private String billboard = "CENTER";
    private boolean seeThrough = true;
    private boolean shadowed = true;
    /** ARGB background color. -1 means use the default (vanilla semi-transparent black). */
    private int backgroundColor = -1;

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

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public String getBillboard() {
        return billboard;
    }

    public void setBillboard(String billboard) {
        this.billboard = billboard;
    }

    public boolean isSeeThrough() {
        return seeThrough;
    }

    public void setSeeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public boolean isShadowed() {
        return shadowed;
    }

    public void setShadowed(boolean shadowed) {
        this.shadowed = shadowed;
    }
}
