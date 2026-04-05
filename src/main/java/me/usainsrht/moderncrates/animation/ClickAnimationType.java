package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.crate.Crate;
import org.bukkit.entity.Player;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

/**
 * The Click animation type - rewards are hidden and players click to reveal them.
 * Supports shuffle phases and multiple reward selections.
 */
public class ClickAnimationType extends AbstractAnimationType {

    private final GracefulScheduling scheduler;

    public ClickAnimationType(GracefulScheduling scheduler) {
        super("click");
        this.scheduler = scheduler;
    }

    @Override
    public AnimationSession createSession(Player player, Crate crate, Animation animation) {
        return new ClickAnimationSession(player, crate, animation, this);
    }

    @Override
    public boolean isValidConfig(Animation animation) {
        return super.isValidConfig(animation)
                && animation.getRewardAmount() > 0;
    }

    public GracefulScheduling getScheduler() {
        return scheduler;
    }
}
