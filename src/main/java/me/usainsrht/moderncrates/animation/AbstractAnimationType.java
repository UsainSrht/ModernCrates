package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationType;

/**
 * Base abstract animation type with common reward selection logic.
 */
public abstract class AbstractAnimationType implements AnimationType {

    private final String id;

    protected AbstractAnimationType(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isValidConfig(Animation animation) {
        return animation.getRewardSlots() != null && !animation.getRewardSlots().isEmpty();
    }
}
