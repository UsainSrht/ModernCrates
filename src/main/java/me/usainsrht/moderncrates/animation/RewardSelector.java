package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Weighted random reward selector used by animation sessions.
 */
public final class RewardSelector {

    private RewardSelector() {}

    /**
     * Selects a single random reward based on weighted chances (all rewards — for display).
     */
    public static Reward selectWeighted(Crate crate) {
        return selectWeighted(crate.getRewards().values());
    }

    /**
     * Selects a single random reward the player is eligible to win.
     */
    public static Reward selectWeighted(Crate crate, Player player) {
        return selectWeighted(getEligibleRewards(crate, player));
    }

    private static Reward selectWeighted(Collection<Reward> rewards) {
        if (rewards.isEmpty()) return null;

        double totalWeight = rewards.stream().mapToDouble(Reward::getChance).sum();
        if (totalWeight <= 0) {
            return rewards.iterator().next();
        }

        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0;

        for (Reward reward : rewards) {
            cumulative += reward.getChance();
            if (random < cumulative) {
                return reward;
            }
        }
        return rewards.stream().reduce((a, b) -> b).orElse(null);
    }

    /**
     * Selects multiple unique rewards (or with replacement if not enough unique).
     */
    public static List<Reward> selectMultiple(Crate crate, int count) {
        List<Reward> selected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Reward r = selectWeighted(crate);
            if (r != null) selected.add(r);
        }
        return selected;
    }

    /**
     * Selects multiple rewards the player is eligible to win.
     */
    public static List<Reward> selectMultiple(Crate crate, int count, Player player) {
        List<Reward> selected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Reward r = selectWeighted(crate, player);
            if (r != null) selected.add(r);
        }
        return selected;
    }

    public static List<Reward> getEligibleRewards(Crate crate, Player player) {
        return crate.getRewards().values().stream()
                .filter(r -> r.canWin(player))
                .toList();
    }

    /**
     * Returns the candidate if the player can win it, otherwise selects a weighted eligible reward.
     */
    public static Reward resolveWinner(Reward candidate, Player player, Crate crate) {
        if (candidate != null && candidate.canWin(player)) {
            return candidate;
        }
        return selectWeighted(crate, player);
    }

    /**
     * Generates a list of random rewards for display in the animation scroll.
     * The reward at the specified index will be the actual winning reward.
     */
    public static List<Reward> generateScrollRewards(Crate crate, int slotCount, int winningIndex, Reward actualReward) {
        List<Reward> scroll = new ArrayList<>();
        List<Reward> pool = new ArrayList<>(crate.getRewards().values());
        if (pool.isEmpty()) return scroll;

        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < slotCount; i++) {
            if (i == winningIndex) {
                scroll.add(actualReward);
            } else {
                scroll.add(pool.get(random.nextInt(pool.size())));
            }
        }
        return scroll;
    }
}
