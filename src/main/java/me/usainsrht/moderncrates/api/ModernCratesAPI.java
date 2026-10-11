package me.usainsrht.moderncrates.api;

import me.usainsrht.moderncrates.api.animation.AnimationType;
import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.crate.Crate;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

/**
 * Main API entry point for ModernCrates.
 * External plugins can use this to register custom animation types,
 * animations, and crates programmatically.
 */
public interface ModernCratesAPI {

    // ========================
    // Animation Type Registry
    // ========================

    void registerAnimationType(String id, AnimationType type);

    void unregisterAnimationType(String id);

    Optional<AnimationType> getAnimationType(String id);

    Collection<String> getRegisteredAnimationTypeIds();

    // ========================
    // Animation Registry
    // ========================

    void registerAnimation(String id, Animation animation);

    void unregisterAnimation(String id);

    Optional<Animation> getAnimation(String id);

    Collection<String> getRegisteredAnimationIds();

    // ========================
    // Crate Registry
    // ========================

    void registerCrate(Crate crate);

    void unregisterCrate(String id);

    Optional<Crate> getCrate(String id);

    Collection<Crate> getCrates();

    Collection<String> getCrateIds();

    // ========================
    // Crate Operations
    // ========================

    void openCrate(Player player, Crate crate);

    void giveCrateKey(Player player, String crateId, int amount);

    void giveCrateItem(Player player, String crateId, int amount);

    int getVirtualKeys(Player player, String crateId);

    void setVirtualKeys(Player player, String crateId, int amount);

    default void addVirtualKeys(Player player, String crateId, int amount) {
        setVirtualKeys(player, crateId, getVirtualKeys(player, crateId) + amount);
    }

    default boolean removeVirtualKeys(Player player, String crateId, int amount) {
        int current = getVirtualKeys(player, crateId);
        if (current <= 0) return false;
        setVirtualKeys(player, crateId, Math.max(0, current - amount));
        return true;
    }

    default java.util.Map<String, Integer> getVirtualKeys(Player player) {
        return java.util.Map.of();
    }

    // ========================
    // Provider
    // ========================

    static ModernCratesAPI getInstance() {
        return ModernCratesProvider.get();
    }
}
