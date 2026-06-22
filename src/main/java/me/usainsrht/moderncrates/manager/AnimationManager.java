package me.usainsrht.moderncrates.manager;

import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.animation.AnimationType;
import me.usainsrht.moderncrates.api.crate.Crate;
import org.bukkit.Location;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.reward.RewardItem;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active animation sessions for players opening crates.
 */
public class AnimationManager {

    private final GracefulScheduling scheduling;

    private final Map<UUID, AnimationSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Crate> sessionCrates = new ConcurrentHashMap<>();

    public AnimationManager(GracefulScheduling scheduling) {
        this.scheduling = scheduling;
    }

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
        startSession(player, crate, type, animation, null);
    }

    public void startSession(Player player, Crate crate, AnimationType type, Animation animation, Location interactedLocation) {
        if (hasActiveSession(player)) return;

        AnimationSession session = interactedLocation != null
                ? type.createSession(player, crate, animation, interactedLocation)
                : type.createSession(player, crate, animation);
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
        List<Reward> rewards = session.getSelectedRewards();
        if (rewards != null && !rewards.isEmpty() && actualCrate != null) {
            for (Reward reward : rewards) {
                grantReward(player, actualCrate, reward);
            }
            announceRewards(player, actualCrate, rewards);
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
                            player.getWorld().dropItem(player.getLocation(), dropped));
                }
            }
        }

        // Execute commands
        if (reward.hasCommands()) {
            for (String command : reward.getCommands()) {
                String parsed = command.replace("<player>", player.getName());
                scheduling.globalRegionalScheduler().run(() ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed));
            }
        }
    }

    private void announceReward(Player player, Crate crate, Reward reward) {
        ItemStack displayItem = ItemBuilder.fromDisplay(reward, crate);
        Component rewardDisplayName = displayItem.displayName().hoverEvent(displayItem.asHoverEvent());

        // Per-reward custom announcement
        if (reward.getAnnounce() != null) {
            String msg = reward.getAnnounce()
                    .replace("<player>", player.getName())
                    .replace("<reward_name>", "%%REWARD_NAME%%");
            Component comp = TextUtil.parse(msg)
                    .replaceText(builder -> builder.matchLiteral("%%REWARD_NAME%%").replacement(rewardDisplayName));
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
                    .replace("<reward_name>", "%%REWARD_NAME%%");
            Component comp = TextUtil.parse(msg)
                    .replaceText(builder -> builder.matchLiteral("%%REWARD_NAME%%").replacement(rewardDisplayName));
            if (annConfig.isToEveryone()) {
                Bukkit.getServer().sendMessage(comp);
            } else {
                player.sendMessage(comp);
            }
        }
    }

    private void announceRewards(Player player, Crate crate, List<Reward> rewards) {
        if (crate.getAnnounceConfig() == null) return;
        var annConfig = crate.getAnnounceConfig();

        if (rewards.size() == 1) {
            announceReward(player, crate, rewards.get(0));
            return;
        }

        // Fallback to single announcements if multiple is not configured
        String multipleHeader = annConfig.getMultiple();
        if (multipleHeader == null || multipleHeader.isEmpty()) {
            for (Reward reward : rewards) {
                announceReward(player, crate, reward);
            }
            return;
        }

        // Send custom announcements for any rewards that have custom overrides
        for (Reward reward : rewards) {
            if (reward.getAnnounce() != null) {
                ItemStack displayItem = ItemBuilder.fromDisplay(reward, crate);
                Component rewardDisplayName = displayItem.displayName().hoverEvent(displayItem.asHoverEvent());
                String msg = reward.getAnnounce()
                        .replace("<player>", player.getName())
                        .replace("<reward_name>", "%%REWARD_NAME%%");
                Component comp = TextUtil.parse(msg)
                        .replaceText(builder -> builder.matchLiteral("%%REWARD_NAME%%").replacement(rewardDisplayName));
                if (annConfig.isToEveryone()) {
                    Bukkit.getServer().sendMessage(comp);
                } else {
                    player.sendMessage(comp);
                }
            }
        }

        // Send the multiple rewards announcement
        String headerMsg = multipleHeader.replace("<player>", player.getName());
        Component headerComp = TextUtil.parse(headerMsg);

        List<Component> messageLines = new ArrayList<>();
        messageLines.add(headerComp);

        String multipleItemFormat = annConfig.getMultipleItem();
        if (multipleItemFormat != null && !multipleItemFormat.isEmpty()) {
            for (Reward reward : rewards) {
                ItemStack displayItem = ItemBuilder.fromDisplay(reward, crate);
                Component rewardDisplayName = displayItem.displayName().hoverEvent(displayItem.asHoverEvent());
                String itemMsg = multipleItemFormat
                        .replace("<player>", player.getName())
                        .replace("<reward_name>", "%%REWARD_NAME%%");
                Component itemComp = TextUtil.parse(itemMsg)
                        .replaceText(builder -> builder.matchLiteral("%%REWARD_NAME%%").replacement(rewardDisplayName));
                messageLines.add(itemComp);
            }
        }

        if (annConfig.isToEveryone()) {
            for (Component line : messageLines) {
                Bukkit.getServer().sendMessage(line);
            }
        } else {
            for (Component line : messageLines) {
                player.sendMessage(line);
            }
        }
    }

    public void cancelAll() {
        activeSessions.values().forEach(AnimationSession::cancel);
        activeSessions.clear();
        sessionCrates.clear();
    }
}
