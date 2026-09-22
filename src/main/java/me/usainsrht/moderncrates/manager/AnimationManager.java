package me.usainsrht.moderncrates.manager;

import me.usainsrht.itemapi.itemtext.ItemText;
import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.animation.AnimationType;
import me.usainsrht.moderncrates.api.crate.AnnounceConfig;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.reward.RewardItem;
import me.usainsrht.moderncrates.hook.LuckPermsHook;
import me.usainsrht.moderncrates.util.BlockKey;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.LiddedBlockUtil;
import me.usainsrht.moderncrates.util.PlaceholderUtil;
import me.usainsrht.yamlmessage.YamlMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active animation sessions for players opening crates.
 */
public class AnimationManager {

    private final ModernCratesPlugin plugin;
    private final GracefulScheduling scheduling;

    private final Map<UUID, AnimationSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Crate> sessionCrates = new ConcurrentHashMap<>();
    /** Blocks currently being animated (prevents concurrent opens on the same physical crate). */
    private final Map<BlockKey, UUID> activeBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, BlockKey> playerBlocks = new ConcurrentHashMap<>();
    /** Lidded blocks opened by open_lid for cleanup on session end. */
    private final Map<UUID, BlockKey> openedLids = new ConcurrentHashMap<>();

    public AnimationManager(ModernCratesPlugin plugin, GracefulScheduling scheduling) {
        this.plugin = plugin;
        this.scheduling = scheduling;
    }

    public AnimationManager(GracefulScheduling scheduling) {
        this(ModernCratesPlugin.getPlugin(ModernCratesPlugin.class), scheduling);
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

    public boolean startSession(Player player, Crate crate, AnimationType type, Animation animation) {
        return startSession(player, crate, type, animation, null);
    }

    public boolean isBlockInUse(Location location) {
        if (location == null || location.getWorld() == null) return false;
        return activeBlocks.containsKey(BlockKey.from(location));
    }

    public boolean startSession(Player player, Crate crate, AnimationType type, Animation animation, Location interactedLocation) {
        if (hasActiveSession(player)) return false;

        BlockKey blockKey = null;
        if (interactedLocation != null && interactedLocation.getWorld() != null && animation.isLocksPhysicalBlock()) {
            blockKey = BlockKey.from(interactedLocation);
            UUID existing = activeBlocks.putIfAbsent(blockKey, player.getUniqueId());
            if (existing != null) return false;
        }

        AnimationSession session = interactedLocation != null
                ? type.createSession(player, crate, animation, interactedLocation)
                : type.createSession(player, crate, animation);
        activeSessions.put(player.getUniqueId(), session);
        sessionCrates.put(player.getUniqueId(), crate);
        if (blockKey != null) {
            playerBlocks.put(player.getUniqueId(), blockKey);
        }
        openLidIfConfigured(player, animation, interactedLocation);
        session.start();
        return true;
    }

    private void openLidIfConfigured(Player player, Animation animation, Location interactedLocation) {
        if (!animation.isOpenLid() || interactedLocation == null) return;
        // item_rise manages its own lid timing as part of the animation
        if ("item_rise".equals(animation.getTypeId())) return;
        if (!LiddedBlockUtil.isLidded(interactedLocation)) return;

        BlockKey lidKey = BlockKey.from(interactedLocation);
        openedLids.put(player.getUniqueId(), lidKey);
        scheduling.regionSpecificScheduler(interactedLocation)
                .run(() -> LiddedBlockUtil.open(interactedLocation));
    }

    private void releaseLid(UUID playerId) {
        BlockKey lidKey = openedLids.remove(playerId);
        if (lidKey == null) return;

        Location location = lidKey.toLocation();
        if (location == null) return;

        scheduling.regionSpecificScheduler(location)
                .run(() -> LiddedBlockUtil.close(location));
    }

    private void releaseBlock(UUID playerId) {
        BlockKey blockKey = playerBlocks.remove(playerId);
        if (blockKey != null) {
            activeBlocks.remove(blockKey, playerId);
        }
        releaseLid(playerId);
    }

    public void endSession(Player player, Crate crate) {
        AnimationSession session = activeSessions.remove(player.getUniqueId());
        Crate sessionCrate = sessionCrates.remove(player.getUniqueId());
        releaseBlock(player.getUniqueId());
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
        releaseBlock(player.getUniqueId());
        if (session != null) {
            session.cancel();
        }
    }

    private void grantReward(Player player, Crate crate, Reward reward) {
        if (!reward.canWin(player)) return;

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
        if (!reward.shouldAnnounce(crate)) {
            return;
        }

        ItemStack displayItem = ItemBuilder.fromDisplay(reward, crate);
        Component rewardDisplayName = ItemText.format(displayItem);
        TagResolver[] resolvers = PlaceholderUtil.rewardResolvers(player, crate, reward, rewardDisplayName);
        String prefix = plugin != null ? plugin.getPluginConfig().getPrefix() : null;

        // Per-reward custom announcement
        YamlMessage rewardAnnounce = reward.getAnnouncementMessage();
        if (rewardAnnounce != null && !rewardAnnounce.isEmpty()) {
            boolean toEveryone = crate != null && crate.getAnnounceConfig() != null && crate.getAnnounceConfig().isToEveryone();
            dispatchAnnouncement(rewardAnnounce, player, toEveryone, prefix, resolvers);
            return;
        }

        // Default crate announcement
        if (crate != null && crate.getAnnounceConfig() != null) {
            AnnounceConfig annConfig = crate.getAnnounceConfig();
            YamlMessage singleMsg = annConfig.getSingleMessage();
            if (!singleMsg.isEmpty()) {
                dispatchAnnouncement(singleMsg, player, annConfig.isToEveryone(), prefix, resolvers);
            }
        }
    }

    private void announceRewards(Player player, Crate crate, List<Reward> rewards) {
        if (rewards == null || rewards.isEmpty()) return;

        List<Reward> announcedRewards = rewards.stream()
                .filter(reward -> reward.shouldAnnounce(crate))
                .toList();

        if (announcedRewards.isEmpty()) {
            return;
        }

        if (announcedRewards.size() == 1) {
            announceReward(player, crate, announcedRewards.get(0));
            return;
        }

        if (crate == null || crate.getAnnounceConfig() == null) {
            for (Reward reward : announcedRewards) {
                announceReward(player, crate, reward);
            }
            return;
        }

        AnnounceConfig annConfig = crate.getAnnounceConfig();
        String prefix = plugin != null ? plugin.getPluginConfig().getPrefix() : null;

        // Fallback to single announcements if multiple is not configured
        YamlMessage multipleHeader = annConfig.getMultipleMessage();
        if (multipleHeader.isEmpty()) {
            for (Reward reward : announcedRewards) {
                announceReward(player, crate, reward);
            }
            return;
        }

        // Send custom announcements for any rewards that have custom overrides
        for (Reward reward : announcedRewards) {
            YamlMessage rewardAnnounce = reward.getAnnouncementMessage();
            if (rewardAnnounce != null && !rewardAnnounce.isEmpty()) {
                ItemStack displayItem = ItemBuilder.fromDisplay(reward, crate);
                Component rewardDisplayName = ItemText.format(displayItem);
                TagResolver[] resolvers = PlaceholderUtil.rewardResolvers(player, crate, reward, rewardDisplayName);
                dispatchAnnouncement(rewardAnnounce, player, annConfig.isToEveryone(), prefix, resolvers);
            }
        }

        // Send the multiple rewards announcement header
        TagResolver[] crateResolvers = PlaceholderUtil.crateResolvers(player, crate);
        dispatchAnnouncement(multipleHeader, player, annConfig.isToEveryone(), prefix, crateResolvers);

        // Send multiple item lines
        YamlMessage itemMessage = annConfig.getMultipleItemMessage();
        if (!itemMessage.isEmpty()) {
            for (Reward reward : announcedRewards) {
                ItemStack displayItem = ItemBuilder.fromDisplay(reward, crate);
                Component rewardDisplayName = ItemText.format(displayItem);
                TagResolver[] resolvers = PlaceholderUtil.rewardResolvers(player, crate, reward, rewardDisplayName);
                dispatchAnnouncement(itemMessage, player, annConfig.isToEveryone(), prefix, resolvers);
            }
        }
    }

    private void dispatchAnnouncement(
            YamlMessage message,
            Player opener,
            boolean toEveryone,
            String prefix,
            TagResolver... resolvers) {
        if (message == null || message.isEmpty()) return;

        String muteKey = plugin != null ? plugin.getPluginConfig().getAnnouncementMuteMetaKey() : "mute-crate-announcements";
        PlaceholderUtil.broadcastAnnouncement(message, opener, toEveryone, prefix, muteKey, resolvers);
    }

    public void cancelAll() {
        activeSessions.values().forEach(AnimationSession::cancel);
        activeSessions.clear();
        sessionCrates.clear();
        for (BlockKey lidKey : openedLids.values()) {
            Location location = lidKey.toLocation();
            if (location != null) {
                scheduling.regionSpecificScheduler(location)
                        .run(() -> LiddedBlockUtil.close(location));
            }
        }
        openedLids.clear();
        activeBlocks.clear();
        playerBlocks.clear();
    }
}
