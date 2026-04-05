package me.usainsrht.moderncrates.api.animation;

import java.util.List;
import java.util.Map;

/**
 * Configuration for a GUI item used in animations.
 */
public class GuiItemConfig {

    private String material;
    private String name;
    private List<String> lore;
    private Map<String, Object> nbt;

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }

    public Map<String, Object> getNbt() { return nbt; }
    public void setNbt(Map<String, Object> nbt) { this.nbt = nbt; }
}
