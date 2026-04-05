package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.crate.Crate;
import org.bukkit.entity.Player;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

/**
 * The CSGO animation type - a scrolling reel that slows down gradually.
 * Supports horizontal (classic CSGO) and vertical (roulette) configurations
 * depending on how reward_slots are arranged in the YAML.
 */
public class CsgoAnimationType extends AbstractAnimationType {

    private final GracefulScheduling scheduler;

    public CsgoAnimationType(GracefulScheduling scheduler) {
        super("csgo");
        this.scheduler = scheduler;
    }

    @Override
    public AnimationSession createSession(Player player, Crate crate, Animation animation) {
        return new CsgoAnimationSession(player, crate, animation, this);
    }

    @Override
    public boolean isValidConfig(Animation animation) {
        return super.isValidConfig(animation)
                && animation.getTotalTicks() > 0
                && animation.getRewardIndex() > 0;
    }

    public GracefulScheduling getScheduler() {
        return scheduler;
    }
}
