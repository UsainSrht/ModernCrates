package me.usainsrht.moderncrates.manager;

import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.animation.AnimationType;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.reward.RewardItem;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active animation sessions for players opening crates.
 */
public class AnimationManager {

    private final Map<UUID, AnimationSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Crate> sessionCrates = new ConcurrentHashMap<>();

    public boolean hasActiveSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public AnimationSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public Crate getCrateForSession(Player player) {
        return sessionCrates.get(player.getUniqueId());
    }

    public void startSession(Player player, Crate crate, AnimationType type, Animation animation) {
        if (hasActiveSession(player)) return;

        AnimationSession session = type.createSession(player, crate, animation);
        activeSessions.put(player.getUniqueId(), session);
        sessionCrates.put(player.getUniqueId(), crate);
        session.start();
    }

    public void endSession(Player player, Crate crate) {
        AnimationSession session = activeSessions.remove(player.getUniqueId());
        Crate sessionCrate = sessionCrates.remove(player.getUniqueId());
        if (session == null) return;

        if (!session.isFinished()) {
            session.cancel();
        }

        // Use the tracked crate if the provided one is null
        Crate actualCrate = crate != null ? crate : sessionCrate;

        // Grant rewards
        Reward reward = session.getSelectedReward();
        if (reward != null && actualCrate != null) {
            grantReward(player, actualCrate, reward);
        }
    }

    public void cancelSession(Player player) {
        AnimationSession session = activeSessions.remove(player.getUniqueId());
        sessionCrates.remove(player.getUniqueId());
        if (session != null) {
            session.cancel();
        }
    }

    private void grantReward(Player player, Crate crate, Reward reward) {
        // Give items
        if (reward.hasItems()) {
            for (RewardItem item : reward.getItems().values()) {
                if (item.getMaterial() != null) {
                    ItemStack stack = ItemBuilder.fromRewardItem(item);
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
                    // Drop overflow items at player location
                    overflow.values().forEach(dropped ->
                            player.getWorld().dropItemNaturally(player.getLocation(), dropped));
                }
            }
        }

        // Execute commands
        if (reward.hasCommands()) {
            for (String command : reward.getCommands()) {
                String parsed = command.replace("<player>", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }

        // Announce
        announceReward(player, crate, reward);
    }

    private void announceReward(Player player, Crate crate, Reward reward) {
        // Per-reward custom announcement
        if (reward.getAnnounce() != null) {
            String msg = reward.getAnnounce()
                    .replace("<player>", player.getName())
                    .replace("<reward_name>", getRewardName(reward));
            Component comp = TextUtil.parse(msg);
            if (crate.getAnnounceConfig() != null && crate.getAnnounceConfig().isToEveryone()) {
                Bukkit.getServer().sendMessage(comp);
            } else {
                player.sendMessage(comp);
            }
            return;
        }

        // Default crate announcement
        if (crate.getAnnounceConfig() != null) {
            var annConfig = crate.getAnnounceConfig();
            String msg = annConfig.getSingle()
                    .replace("<player>", player.getName())
                    .replace("<reward_name>", getRewardName(reward));
            Component comp = TextUtil.parse(msg);
            if (annConfig.isToEveryone()) {
                Bukkit.getServer().sendMessage(comp);
            } else {
                player.sendMessage(comp);
            }
        }
    }

    private String getRewardName(Reward reward) {
        if (reward.getDisplay() != null && reward.getDisplay().getName() != null) {
            return reward.getDisplay().getName();
        }
        return reward.getId();
    }

    public void cancelAll() {
        activeSessions.values().forEach(AnimationSession::cancel);
        activeSessions.clear();
        sessionCrates.clear();
    }
}
