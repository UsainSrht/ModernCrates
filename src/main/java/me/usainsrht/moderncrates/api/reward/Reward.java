package me.usainsrht.moderncrates.api.reward;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.requirement.RewardRequirements;
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
    private Boolean announce;
    private YamlMessage announcementMessage;
    private String announcementMessageRaw;
    private String requiredPermission;
    private RewardRequirements requirements;

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

    public @Nullable Boolean getAnnounce() {
        return announce;
    }

    public @Nullable Boolean isAnnounce() {
        return announce;
    }

    public void setAnnounce(@Nullable Boolean announce) {
        this.announce = announce;
    }

    public @Nullable YamlMessage getAnnouncementMessage() {
        if (announcementMessage != null) return announcementMessage;
        if (announcementMessageRaw != null) {
            this.announcementMessage = YamlMessage.parse(announcementMessageRaw);
            return this.announcementMessage;
        }
        return null;
    }

    public void setAnnouncementMessage(@Nullable YamlMessage announcementMessage) {
        this.announcementMessage = announcementMessage;
        if (announcementMessage != null && announcementMessage.chat() != null && !announcementMessage.chat().isEmpty()) {
            this.announcementMessageRaw = String.join("\n", announcementMessage.chat());
        } else if (announcementMessage == null) {
            this.announcementMessageRaw = null;
        }
    }

    public void setAnnouncementMessage(@Nullable String message) {
        this.announcementMessageRaw = message;
        this.announcementMessage = message != null ? YamlMessage.parse(message) : null;
    }

    public @Nullable String getAnnouncementMessageRaw() {
        return announcementMessageRaw;
    }

    public @Nullable YamlMessage getAnnounceMessage() {
        return getAnnouncementMessage();
    }

    public void setAnnounceMessage(@Nullable YamlMessage announceMessage) {
        setAnnouncementMessage(announceMessage);
    }

    public boolean shouldAnnounce(@Nullable Crate crate) {
        if (announce != null) {
            return announce;
        }
        if (getAnnouncementMessage() != null && !getAnnouncementMessage().isEmpty()) {
            return true;
        }
        if (crate != null && crate.getAnnounceConfig() != null) {
            return crate.getAnnounceConfig().isDefaultAnnounce();
        }
        return false;
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
        if (hasRequiredPermission() && (player == null || !player.hasPermission(requiredPermission))) {
            return false;
        }
        if (requirements != null && !requirements.evaluate(player)) {
            return false;
        }
        return true;
    }

    public @Nullable RewardRequirements getRequirements() {
        return requirements;
    }

    public void setRequirements(@Nullable RewardRequirements requirements) {
        this.requirements = requirements;
    }

    public boolean hasRequirements() {
        return requirements != null && !requirements.isEmpty();
    }

    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    public boolean hasCommands() {
        return commands != null && !commands.isEmpty();
    }
}
