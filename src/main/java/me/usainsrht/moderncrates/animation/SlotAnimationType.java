package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.crate.Crate;
import org.bukkit.entity.Player;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

/**
 * The Slot animation type - multiple columns of rewards cycle like a slot machine.
 * When all columns stop, if the winner row shows matching rewards, the player wins.
 */
public class SlotAnimationType extends AbstractAnimationType {

    private final GracefulScheduling scheduler;

    public SlotAnimationType(GracefulScheduling scheduler) {
        super("slot");
        this.scheduler = scheduler;
    }

    @Override
    public AnimationSession createSession(Player player, Crate crate, Animation animation) {
        return new SlotAnimationSession(player, crate, animation, this);
    }

    @Override
    public boolean isValidConfig(Animation animation) {
        return animation.getSlotColumns() != null && !animation.getSlotColumns().isEmpty()
                && animation.getRewardWinnerColumns() != null && !animation.getRewardWinnerColumns().isEmpty()
                && animation.getTotalTicks() > 0;
    }

    public GracefulScheduling getScheduler() {
        return scheduler;
    }
}
