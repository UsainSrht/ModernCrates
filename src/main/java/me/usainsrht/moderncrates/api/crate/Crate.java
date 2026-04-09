package me.usainsrht.moderncrates.api.crate;

import me.usainsrht.moderncrates.api.reward.Reward;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a configured crate with all its settings.
 */
public class Crate {

    private final String id;
    private String name;
    private String animationId;
    private CrateKeyConfig keyConfig;
    private CrateItemConfig itemConfig;
    private List<CrateLocation> crateLocations = new ArrayList<>();
    private boolean bounceBack;
    private HologramConfig hologramConfig;
    private PreviewConfig previewConfig;
    private AnnounceConfig announceConfig;
    private Map<String, Reward> rewards;

    public Crate(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnimationId() {
        return animationId;
    }

    public void setAnimationId(String animationId) {
        this.animationId = animationId;
    }

    public CrateKeyConfig getKeyConfig() {
        return keyConfig;
    }

    public void setKeyConfig(CrateKeyConfig keyConfig) {
        this.keyConfig = keyConfig;
    }

    public CrateItemConfig getItemConfig() {
        return itemConfig;
    }

    public void setItemConfig(CrateItemConfig itemConfig) {
        this.itemConfig = itemConfig;
    }

    public List<CrateLocation> getCrateLocations() {
        return crateLocations;
    }

    public void setCrateLocations(List<CrateLocation> crateLocations) {
        this.crateLocations = crateLocations != null ? crateLocations : new ArrayList<>();
    }

    public void addCrateLocation(CrateLocation loc) {
        if (loc != null) crateLocations.add(loc);
    }

    public void clearCrateLocations() {
        crateLocations.clear();
    }

    /** Returns the first registered location, or null if none. */
    public CrateLocation getCrateLocation() {
        return crateLocations.isEmpty() ? null : crateLocations.get(0);
    }

    /** Backward-compat: sets list to a single location (or clears if null). */
    public void setCrateLocation(CrateLocation crateLocation) {
        crateLocations.clear();
        if (crateLocation != null) crateLocations.add(crateLocation);
    }

    public boolean isBounceBack() {
        return bounceBack;
    }

    public void setBounceBack(boolean bounceBack) {
        this.bounceBack = bounceBack;
    }

    public HologramConfig getHologramConfig() {
        return hologramConfig;
    }

    public void setHologramConfig(HologramConfig hologramConfig) {
        this.hologramConfig = hologramConfig;
    }

    public PreviewConfig getPreviewConfig() {
        return previewConfig;
    }

    public void setPreviewConfig(PreviewConfig previewConfig) {
        this.previewConfig = previewConfig;
    }

    public AnnounceConfig getAnnounceConfig() {
        return announceConfig;
    }

    public void setAnnounceConfig(AnnounceConfig announceConfig) {
        this.announceConfig = announceConfig;
    }

    public Map<String, Reward> getRewards() {
        return rewards;
    }

    public void setRewards(Map<String, Reward> rewards) {
        this.rewards = rewards;
    }

    public boolean isPhysical() {
        return !crateLocations.isEmpty();
    }

    public boolean requiresKey() {
        return keyConfig != null && keyConfig.isRequired();
    }

    /**
     * Gets the total weight (sum of all reward chances).
     */
    public double getTotalWeight() {
        return rewards.values().stream().mapToDouble(Reward::getChance).sum();
    }
}
