package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.crate.Crate;
import org.bukkit.entity.Player;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

/**
 * The Item Rise animation type - a physical crate animation where the reward
 * item rises from a container block with particles spiraling around it.
 * Only works with physical crates assigned to lidded containers (chest, barrel, shulker).
 */
public class ItemRiseAnimationType extends AbstractAnimationType {

    private final GracefulScheduling scheduler;
    private final ModernCratesPlugin plugin;

    public ItemRiseAnimationType(GracefulScheduling scheduler, ModernCratesPlugin plugin) {
        super("item_rise");
        this.scheduler = scheduler;
        this.plugin = plugin;
    }

    @Override
    public AnimationSession createSession(Player player, Crate crate, Animation animation) {
        Runnable onComplete = () -> plugin.getAnimationManager().endSession(player, null);
        return new ItemRiseAnimationSession(player, crate, animation, this, onComplete);
    }

    @Override
    public boolean isValidConfig(Animation animation) {
        return animation.getRiseTicks() > 0
                && animation.getRiseHeight() > 0;
    }

    public GracefulScheduling getScheduler() {
        return scheduler;
    }
}
