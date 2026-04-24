package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.crate.Crate;
import org.bukkit.entity.Player;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

/**
 * BlockDismantle animation type – spawns 6 BlockDisplay entities that together
 * form the visual shell of a configurable block. The top face launches upward
 * in a random direction, the four side faces fall outward, and the bottom
 * remains. Reward ItemDisplay entities appear inside the emptied shell.
 */
public class BlockDismantleAnimationType extends AbstractAnimationType {

    private final GracefulScheduling scheduler;
    private final ModernCratesPlugin plugin;

    public BlockDismantleAnimationType(GracefulScheduling scheduler, ModernCratesPlugin plugin) {
        super("block_dismantle");
        this.scheduler = scheduler;
        this.plugin = plugin;
    }

    @Override
    public AnimationSession createSession(Player player, Crate crate, Animation animation) {
        Runnable onComplete = () -> plugin.getAnimationManager().endSession(player, null);
        return new BlockDismantleAnimationSession(player, crate, animation, this, onComplete);
    }

    @Override
    public boolean isValidConfig(Animation animation) {
        return animation.getDismantleFallDurationTicks() > 0
                && animation.getDismantleBlockType() != null
                && !animation.getDismantleBlockType().isEmpty();
    }

    public GracefulScheduling getScheduler() {
        return scheduler;
    }
}

