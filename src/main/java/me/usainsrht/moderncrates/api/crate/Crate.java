package me.usainsrht.moderncrates.api.crate;

import me.usainsrht.moderncrates.api.reward.Reward;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private boolean autoShowChanceOnLore;
    private List<String> chanceLoreTemplate = new ArrayList<>();
    private AutoSortMode autoSortOnChance = AutoSortMode.DISABLED;

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

    public AnnounceConfig getAnnounce() {
        return announceConfig;
    }

    public void setAnnounce(AnnounceConfig announceConfig) {
        this.announceConfig = announceConfig;
    }

    public Map<String, Reward> getRewards() {
        return rewards;
    }

    public void setRewards(Map<String, Reward> rewards) {
        this.rewards = rewards;
        if (getAutoSortOnChance().isEnabled()) {
            sortRewards();
        }
    }

    public AutoSortMode getAutoSortOnChance() {
        return autoSortOnChance != null ? autoSortOnChance : AutoSortMode.DISABLED;
    }

    public void setAutoSortOnChance(AutoSortMode autoSortOnChance) {
        this.autoSortOnChance = autoSortOnChance != null ? autoSortOnChance : AutoSortMode.DISABLED;
        if (this.autoSortOnChance.isEnabled()) {
            sortRewards();
        }
    }

    public boolean isAutoSortOnChance() {
        return getAutoSortOnChance().isEnabled();
    }

    public void sortRewards() {
        if (rewards == null || rewards.size() <= 1) return;
        AutoSortMode mode = getAutoSortOnChance();
        if (!mode.isEnabled()) return;

        List<Map.Entry<String, Reward>> list = new ArrayList<>(rewards.entrySet());
        if (mode == AutoSortMode.ASCENDING) {
            list.sort(Comparator.<Map.Entry<String, Reward>>comparingDouble(e -> e.getValue().getChance())
                    .thenComparing(Map.Entry::getKey));
        } else if (mode == AutoSortMode.DESCENDING) {
            list.sort(Comparator.<Map.Entry<String, Reward>>comparingDouble((Map.Entry<String, Reward> e) -> e.getValue().getChance()).reversed()
                    .thenComparing(Map.Entry::getKey));
        }

        Map<String, Reward> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Reward> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        this.rewards = sortedMap;
    }

    public boolean isAutoShowChanceOnLore() {
        return autoShowChanceOnLore;
    }

    public void setAutoShowChanceOnLore(boolean autoShowChanceOnLore) {
        this.autoShowChanceOnLore = autoShowChanceOnLore;
    }

    public List<String> getChanceLoreTemplate() {
        return chanceLoreTemplate;
    }

    public void setChanceLoreTemplate(List<String> chanceLoreTemplate) {
        this.chanceLoreTemplate = chanceLoreTemplate != null ? chanceLoreTemplate : new ArrayList<>();
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
