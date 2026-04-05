package me.usainsrht.moderncrates.api.animation;

import me.usainsrht.moderncrates.api.reward.Reward;

/**
 * Represents an active animation session for a single crate opening.
 * Each time a player opens a crate, a new session is created.
 * The session manages the lifecycle of the animation GUI.
 */
public interface AnimationSession {

    /**
     * Starts the animation. Opens the GUI and begins the tick loop.
     */
    void start();

    /**
     * Cancels the animation prematurely. Cleans up resources.
     */
    void cancel();

    /**
     * Returns whether the animation has finished.
     */
    boolean isFinished();

    /**
     * Gets the final reward selected by this animation.
     * Only valid after the animation has finished.
     */
    Reward getSelectedReward();
}
