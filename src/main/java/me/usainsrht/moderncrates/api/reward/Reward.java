package me.usainsrht.moderncrates.api.reward;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.requirement.RewardRequirements;
import me.usainsrht.yamlmessage.YamlMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
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
    private RewardAnnounce announce;
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

    public @Nullable RewardAnnounce getAnnounce() {
        return announce;
    }

    public void setAnnounce(@Nullable RewardAnnounce announce) {
        this.announce = announce;
    }

    public @Nullable RewardAnnounce getAnnounceConfig() {
        return announce;
    }

    public void setAnnounceConfig(@Nullable RewardAnnounce announce) {
        this.announce = announce;
    }

    public @NotNull RewardAnnounce getOrCreateAnnounce() {
        if (this.announce == null) {
            this.announce = new RewardAnnounce();
        }
        return this.announce;
    }

    public @Nullable Boolean isAnnounce() {
        return announce != null ? announce.getEnabled() : null;
    }

    public @Nullable Boolean isAnnounceEnabled() {
        return isAnnounce();
    }

    public void setAnnounce(@Nullable Boolean enabled) {
        if (enabled == null) {
            if (this.announce != null) {
                this.announce.setEnabled(null);
                if (this.announce.isEmpty()) {
                    this.announce = null;
                }
            }
        } else {
            getOrCreateAnnounce().setEnabled(enabled);
        }
    }

    public @Nullable YamlMessage getAnnouncementMessage() {
        return announce != null ? announce.getMessage() : null;
    }

    public void setAnnouncementMessage(@Nullable YamlMessage announcementMessage) {
        if (announcementMessage == null) {
            if (this.announce != null) {
                this.announce.setMessage((YamlMessage) null);
                if (this.announce.isEmpty()) {
                    this.announce = null;
                }
            }
        } else {
            getOrCreateAnnounce().setMessage(announcementMessage);
        }
    }

    public void setAnnouncementMessage(@Nullable String message) {
        if (message == null) {
            if (this.announce != null) {
                this.announce.setMessage((String) null);
                if (this.announce.isEmpty()) {
                    this.announce = null;
                }
            }
        } else {
            getOrCreateAnnounce().setMessage(message);
        }
    }

    public @Nullable String getAnnouncementMessageRaw() {
        return announce != null ? announce.getMessageRaw() : null;
    }

    public @Nullable YamlMessage getAnnounceMessage() {
        return getAnnouncementMessage();
    }

    public void setAnnounceMessage(@Nullable YamlMessage announceMessage) {
        setAnnouncementMessage(announceMessage);
    }

    public boolean getEffectiveEnabled(@Nullable Crate crate) {
        if (announce != null && announce.getEnabled() != null) {
            return announce.getEnabled();
        }
        if (announce != null && announce.hasMessage()) {
            return true;
        }
        if (crate != null && crate.getAnnounceConfig() != null) {
            return crate.getAnnounceConfig().isEnabled();
        }
        return false;
    }

    public boolean getEffectiveToEveryone(@Nullable Crate crate) {
        if (announce != null && announce.getToEveryone() != null) {
            return announce.getToEveryone();
        }
        if (crate != null && crate.getAnnounceConfig() != null) {
            return crate.getAnnounceConfig().isToEveryone();
        }
        return true;
    }

    public @Nullable YamlMessage getEffectiveMessage(@Nullable Crate crate) {
        if (announce != null && announce.hasMessage()) {
            return announce.getMessage();
        }
        if (crate != null && crate.getAnnounceConfig() != null) {
            return crate.getAnnounceConfig().getSingleMessage();
        }
        return null;
    }

    public @Nullable String getEffectiveMessageRaw(@Nullable Crate crate) {
        if (announce != null && announce.hasMessage()) {
            return announce.getMessageRaw();
        }
        if (crate != null && crate.getAnnounceConfig() != null) {
            return crate.getAnnounceConfig().getSingle();
        }
        return null;
    }

    public boolean shouldAnnounce(@Nullable Crate crate) {
        return getEffectiveEnabled(crate);
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
