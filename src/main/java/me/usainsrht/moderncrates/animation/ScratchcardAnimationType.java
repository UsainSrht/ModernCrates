package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.crate.Crate;
import org.bukkit.entity.Player;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

/**
 * The Scratchcard animation type - like click but player needs matching
 * revealed items to win the prize.
 */
public class ScratchcardAnimationType extends AbstractAnimationType {

    private final GracefulScheduling scheduler;

    public ScratchcardAnimationType(GracefulScheduling scheduler) {
        super("scratchcard");
        this.scheduler = scheduler;
    }

    @Override
    public AnimationSession createSession(Player player, Crate crate, Animation animation) {
        return new ScratchcardAnimationSession(player, crate, animation, this);
    }

    @Override
    public boolean isValidConfig(Animation animation) {
        return super.isValidConfig(animation)
                && animation.getRewardAmount() > 0
                && animation.getMatchRequired() > 0;
    }

    public GracefulScheduling getScheduler() {
        return scheduler;
    }
}
