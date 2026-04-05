package me.usainsrht.moderncrates.api.crate;

import me.usainsrht.moderncrates.api.reward.Reward;

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
    private CrateLocation crateLocation;
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

    public CrateLocation getCrateLocation() {
        return crateLocation;
    }

    public void setCrateLocation(CrateLocation crateLocation) {
        this.crateLocation = crateLocation;
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
        return crateLocation != null;
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
