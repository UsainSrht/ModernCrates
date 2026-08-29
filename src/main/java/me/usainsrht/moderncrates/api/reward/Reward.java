package me.usainsrht.moderncrates.api.reward;

import me.usainsrht.yamlmessage.YamlMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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
    private YamlMessage announceMessage;
    private String requiredPermission;

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
        this.announceMessage = announce != null ? YamlMessage.parse(announce) : null;
    }

    public @Nullable YamlMessage getAnnounceMessage() {
        if (announceMessage != null) return announceMessage;
        if (announce != null) {
            this.announceMessage = YamlMessage.parse(announce);
            return this.announceMessage;
        }
        return null;
    }

    public void setAnnounceMessage(@Nullable YamlMessage announceMessage) {
        this.announceMessage = announceMessage;
        if (announceMessage != null && announceMessage.chat() != null && !announceMessage.chat().isEmpty()) {
            this.announce = String.join("\n", announceMessage.chat());
        } else if (announceMessage == null) {
            this.announce = null;
        }
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public void setRequiredPermission(String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    public boolean hasRequiredPermission() {
        return requiredPermission != null && !requiredPermission.isEmpty();
    }

    public boolean canWin(Player player) {
        return !hasRequiredPermission() || player.hasPermission(requiredPermission);
    }

    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    public boolean hasCommands() {
        return commands != null && !commands.isEmpty();
    }
}
