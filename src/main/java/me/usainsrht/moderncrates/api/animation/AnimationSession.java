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

    /**
     * Gets all rewards selected by this animation.
     * For animations that select a single reward, this returns a list containing that reward.
     * For animations that support multiple rewards (like the click type), this returns all of them.
     */
    default java.util.List<Reward> getSelectedRewards() {
        Reward single = getSelectedReward();
        return single != null ? java.util.List.of(single) : java.util.List.of();
    }

    /**
     * Returns true if the session is currently transitioning inventories (e.g. updating title).
     * Used to prevent closing/ending the session during transitions.
     */
    default boolean isTransitioning() {
        return false;
    }

    /**
     * Returns true when the player should not be able to close this GUI manually.
     * GUI-based sessions (CSGO, click, etc.) check the animation's not_closeable flag.
     */
    default boolean isNotCloseable() {
        return false;
    }
}
