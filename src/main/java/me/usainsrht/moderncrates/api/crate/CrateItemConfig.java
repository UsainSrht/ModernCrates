package me.usainsrht.moderncrates.api.crate;

import java.util.List;

/**
 * Configuration for the crate block item representation.
 */
public class CrateItemConfig {

    private String material;
    private String name;
    private List<String> lore;

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }
}
