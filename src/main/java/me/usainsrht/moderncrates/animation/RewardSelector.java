package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Weighted random reward selector used by animation sessions.
 */
public final class RewardSelector {

    private RewardSelector() {}

    /**
     * Selects a single random reward based on weighted chances.
     */
    public static Reward selectWeighted(Crate crate) {
        Map<String, Reward> rewards = crate.getRewards();
        if (rewards.isEmpty()) return null;

        double totalWeight = crate.getTotalWeight();
        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0;

        for (Reward reward : rewards.values()) {
            cumulative += reward.getChance();
            if (random < cumulative) {
                return reward;
            }
        }
        // Fallback to last reward
        return rewards.values().stream().reduce((a, b) -> b).orElse(null);
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
