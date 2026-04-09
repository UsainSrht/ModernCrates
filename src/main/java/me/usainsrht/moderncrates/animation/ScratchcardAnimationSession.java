package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.gui.ModernCratesGui;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.SoundUtil;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Animation session for the Scratchcard-type animation.
 * Players scratch (click) hidden items to reveal rewards.
 * If enough revealed items match, the player wins that reward.
 */
public class ScratchcardAnimationSession implements AnimationSession, ModernCratesGui {

    private final Player player;
    private final Crate crate;
    private final Animation animation;
    private final ScratchcardAnimationType type;

    private Inventory inventory;
    private ScheduledTask shuffleTask;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private enum Phase { SHUFFLING, SCRATCHING, DONE }
    private Phase phase = Phase.SHUFFLING;
    private int shuffleCount = 0;
    private int scratchesRemaining;
    private final Map<Integer, Reward> slotRewardMap = new HashMap<>();
    private final Set<Integer> revealedSlots = new HashSet<>();
    private Reward selectedReward;

    public ScratchcardAnimationSession(Player player, Crate crate, Animation animation, ScratchcardAnimationType type) {
        this.player = player;
        this.crate = crate;
        this.animation = animation;
        this.type = type;
        this.scratchesRemaining = animation.getRewardAmount();
    }

    @Override
    public void start() {
        List<Integer> rewardSlots = animation.getRewardSlots();
        List<Reward> rewards = RewardSelector.selectMultiple(crate, rewardSlots.size());
        if (rewards.isEmpty()) {
            finished.set(true);
            return;
        }

        for (int i = 0; i < rewardSlots.size() && i < rewards.size(); i++) {
            slotRewardMap.put(rewardSlots.get(i), rewards.get(i));
        }

        String title = animation.getGuiTitleShuffling() != null
                ? animation.getGuiTitleShuffling()
                : animation.getGuiTitle().replace("<crate>", crate.getName());
        inventory = Bukkit.createInventory(this, animation.getGuiRows() * 9, TextUtil.parse(title));

        fillBackground(inventory);
        player.openInventory(inventory);

        phase = Phase.SHUFFLING;
        shuffleTask = type.getScheduler().entitySpecificScheduler(player)
                .runAtFixedRate(this::shuffleTick, null, 1L, animation.getShuffleTicks());
    }

    private void shuffleTick() {
        if (cancelled.get()) {
            cleanup();
            return;
        }

        shuffleCount++;
        SoundUtil.play(player, animation.getShuffleSounds());

        List<Integer> rewardSlots = animation.getRewardSlots();
        List<Reward> pool = new ArrayList<>(crate.getRewards().values());
        Random rand = ThreadLocalRandom.current();

        for (int slot : rewardSlots) {
            if (slot >= 0 && slot < inventory.getSize() && !pool.isEmpty()) {
                Reward r = pool.get(rand.nextInt(pool.size()));
                if (r.getDisplay() != null) {
                    inventory.setItem(slot, ItemBuilder.fromDisplay(r.getDisplay()));
                }
            }
        }

        if (shuffleCount >= animation.getShuffleAmount()) {
            if (shuffleTask != null) {
                shuffleTask.cancel();
                shuffleTask = null;
            }
            startScratchPhase();
        }
    }

    private void startScratchPhase() {
        phase = Phase.SCRATCHING;

        String title = animation.getGuiTitle()
                .replace("<crate>", crate.getName())
                .replace("<amount>", String.valueOf(scratchesRemaining));

        Inventory newInv = Bukkit.createInventory(this, animation.getGuiRows() * 9, TextUtil.parse(title));
        fillBackground(newInv);

        ItemStack hideItem = animation.getRewardHideItem() != null
                ? ItemBuilder.fromGuiItemConfig(animation.getRewardHideItem())
                : ItemBuilder.create("YELLOW_STAINED_GLASS_PANE", "<yellow>Scratch me!", null);

        for (int slot : animation.getRewardSlots()) {
            if (slot >= 0 && slot < newInv.getSize()) {
                newInv.setItem(slot, hideItem);
            }
        }

        SoundUtil.play(player, animation.getHideSounds());
        inventory = newInv;
        player.openInventory(inventory);
    }

    private void handleScratch(int slot) {
        if (phase != Phase.SCRATCHING || cancelled.get()) return;
        if (revealedSlots.contains(slot)) return;

        Reward reward = slotRewardMap.get(slot);
        if (reward == null) return;

        revealedSlots.add(slot);
        scratchesRemaining--;

        if (reward.getDisplay() != null) {
            inventory.setItem(slot, ItemBuilder.fromDisplay(reward.getDisplay()));
        }
        SoundUtil.play(player, animation.getRevealSounds());

        if (scratchesRemaining <= 0) {
            evaluateResult();
        } else {
            updateTitle();
        }
    }

    private void evaluateResult() {
        phase = Phase.DONE;

        // Count reward occurrences among revealed slots
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Reward> rewardById = new HashMap<>();
        for (int slot : revealedSlots) {
            Reward r = slotRewardMap.get(slot);
            if (r != null) {
                counts.merge(r.getId(), 1, Integer::sum);
                rewardById.put(r.getId(), r);
            }
        }

        // Find the most common reward
        String bestId = null;
        int bestCount = 0;
        for (var entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                bestId = entry.getKey();
            }
        }

        boolean won = bestCount >= animation.getMatchRequired();

        if (won && bestId != null) {
            selectedReward = rewardById.get(bestId);
            SoundUtil.play(player, animation.getWinSounds());
            SoundUtil.play(player, animation.getRewardSounds());
        } else {
            selectedReward = RewardSelector.selectWeighted(crate);
            SoundUtil.play(player, animation.getLoseSounds());
        }

        // Update title to show result
        String resultTitle = won
                ? (animation.getWinTitle() != null ? animation.getWinTitle() : "<green><bold>YOU WIN!")
                : (animation.getLoseTitle() != null ? animation.getLoseTitle() : "<red><bold>Better luck next time!");

        Inventory newInv = Bukkit.createInventory(this, animation.getGuiRows() * 9, TextUtil.parse(resultTitle));
        for (int i = 0; i < inventory.getSize(); i++) {
            newInv.setItem(i, inventory.getItem(i));
        }
        inventory = newInv;
        player.openInventory(inventory);

        // Auto-close after delay
        type.getScheduler().entitySpecificScheduler(player)
                .runDelayed(() -> {
                    finished.set(true);
                    player.closeInventory();
                }, null, Math.max(1, animation.getShowRevealedItemsFor()));
    }

    private void updateTitle() {
        String title = animation.getGuiTitle()
                .replace("<crate>", crate.getName())
                .replace("<amount>", String.valueOf(scratchesRemaining));
        Inventory newInv = Bukkit.createInventory(this, animation.getGuiRows() * 9, TextUtil.parse(title));
        for (int i = 0; i < inventory.getSize(); i++) {
            newInv.setItem(i, inventory.getItem(i));
        }
        inventory = newInv;
        player.openInventory(inventory);
    }

    private void fillBackground(Inventory inv) {
        if (animation.getGuiFill() != null) {
            ItemStack fill = ItemBuilder.fromGuiItemConfig(animation.getGuiFill());
            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, fill);
            }
        }
    }

    @Override
    public void cancel() {
        cancelled.set(true);
        cleanup();
    }

    private void cleanup() {
        if (shuffleTask != null) {
            shuffleTask.cancel();
            shuffleTask = null;
        }
    }

    @Override
    public boolean isFinished() {
        return finished.get();
    }

    @Override
    public Reward getSelectedReward() {
        if (selectedReward != null) return selectedReward;
        return RewardSelector.selectWeighted(crate);
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (event.getReason() == InventoryCloseEvent.Reason.OPEN_NEW) return;
        if (!finished.get()) {
            cleanup();
            if (selectedReward == null) {
                selectedReward = RewardSelector.selectWeighted(crate);
            }
            finished.set(true);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (phase == Phase.SCRATCHING && !cancelled.get()) {
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < inventory.getSize()) {
                handleScratch(slot);
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
