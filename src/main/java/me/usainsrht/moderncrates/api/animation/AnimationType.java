package me.usainsrht.moderncrates.api.animation;

import me.usainsrht.moderncrates.api.crate.Crate;
import org.bukkit.entity.Player;

/**
 * Represents an animation type - the core logic of how an animation works.
 * Animation types are hardcoded (e.g., "csgo", "click") but can be
 * extended by external plugins via the API.
 *
 * An AnimationType is a factory that creates AnimationSession instances
 * for each individual crate opening.
 */
public interface AnimationType {

    /**
     * Gets the unique identifier for this animation type.
     */
    String getId();

    /**
     * Creates a new animation session for a player opening a crate.
     *
     * @param player    the player opening the crate
     * @param crate     the crate being opened
     * @param animation the animation configuration
     * @return a new animation session
     */
    AnimationSession createSession(Player player, Crate crate, Animation animation);

    /**
     * Validates that the given animation configuration is compatible with this type.
     *
     * @param animation the animation to validate
     * @return true if valid
     */
    boolean isValidConfig(Animation animation);
}
