package me.usainsrht.moderncrates.api.reward;

import java.util.List;
import java.util.Map;

/**
 * Represents a single reward within a crate.
 */
public class Reward {

    private final String id;
    private double chance;
    private RewardDisplay display;
    private Map<String, RewardItem> items;
    private List<String> commands;
    private String announce;

    public Reward(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = chance;
    }

    public RewardDisplay getDisplay() {
        return display;
    }

    public void setDisplay(RewardDisplay display) {
        this.display = display;
    }

    public Map<String, RewardItem> getItems() {
        return items;
    }

    public void setItems(Map<String, RewardItem> items) {
        this.items = items;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public String getAnnounce() {
        return announce;
    }

    public void setAnnounce(String announce) {
        this.announce = announce;
    }

    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    public boolean hasCommands() {
        return commands != null && !commands.isEmpty();
    }
}
