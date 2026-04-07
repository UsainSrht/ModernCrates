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
 * Animation session for the Click-type animation.
 * Players click hidden items to reveal their rewards.
 */
public class ClickAnimationSession implements AnimationSession, ModernCratesGui {

    private final Player player;
    private final Crate crate;
    private final Animation animation;
    private final ClickAnimationType type;

    private Inventory inventory;
    private ScheduledTask shuffleTask;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    // State
    private enum Phase { SHUFFLING, CLICKING, DONE }
    private Phase phase = Phase.SHUFFLING;
    private int shuffleCount = 0;
    private int clicksRemaining;
    private List<Reward> selectedRewards;
    private final Map<Integer, Reward> slotRewardMap = new HashMap<>();
    private final Set<Integer> revealedSlots = new HashSet<>();
    private Reward fallbackReward; // Used when player closes early without revealing any slots

    public ClickAnimationSession(Player player, Crate crate, Animation animation, ClickAnimationType type) {
        this.player = player;
        this.crate = crate;
        this.animation = animation;
        this.type = type;
        this.clicksRemaining = animation.getRewardAmount();
    }

    @Override
    public void start() {
        // Pre-select all rewards for slots
        List<Integer> rewardSlots = animation.getRewardSlots();
        selectedRewards = RewardSelector.selectMultiple(crate, rewardSlots.size());
        if (selectedRewards.isEmpty()) {
            finished.set(true);
            return;
        }

        // Map rewards to slots
        for (int i = 0; i < rewardSlots.size() && i < selectedRewards.size(); i++) {
            slotRewardMap.put(rewardSlots.get(i), selectedRewards.get(i));
        }

        // Create inventory (shuffling title)
        String title = animation.getGuiTitleShuffling() != null
                ? animation.getGuiTitleShuffling()
                : animation.getGuiTitle().replace("<crate>", crate.getName());
        int rows = animation.getGuiRows();
        inventory = Bukkit.createInventory(this, rows * 9, TextUtil.parse(title));

        fillBackground();
        player.openInventory(inventory);

        // Start shuffle phase
        phase = Phase.SHUFFLING;
        shuffleTask = type.getScheduler().entitySpecificScheduler(player)
                .runAtFixedRate(() -> shuffleTick(), null, 1L, animation.getShuffleTicks());
    }

    private void shuffleTick() {
        if (cancelled.get()) {
            cleanup();
            return;
        }

        shuffleCount++;
        SoundUtil.play(player, animation.getShuffleSounds());

        // Show random rewards in reward slots
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
            // End shuffle, transition to clicking
            if (shuffleTask != null) {
                shuffleTask.cancel();
                shuffleTask = null;
            }
            startClickPhase();
        }
    }

    private void startClickPhase() {
        phase = Phase.CLICKING;

        // Rebuild inventory with click title
        String title = animation.getGuiTitle()
                .replace("<crate>", crate.getName())
                .replace("<amount>", String.valueOf(clicksRemaining));

        // Close and reopen with new title
        Inventory newInv = Bukkit.createInventory(this, animation.getGuiRows() * 9, TextUtil.parse(title));
        fillBackground(newInv);

        // Place hide items
        ItemStack hideItem = animation.getRewardHideItem() != null
                ? ItemBuilder.fromGuiItemConfig(animation.getRewardHideItem())
                : createDefaultHideItem();

        for (int slot : animation.getRewardSlots()) {
            if (slot >= 0 && slot < newInv.getSize()) {
                newInv.setItem(slot, hideItem);
            }
        }

        SoundUtil.play(player, animation.getHideSounds());
        inventory = newInv;
        player.openInventory(inventory);
    }

    /**
     * Called when a player clicks a slot in the click animation.
     */
    public void handleClick(int slot) {
        if (phase != Phase.CLICKING || cancelled.get()) return;
        if (revealedSlots.contains(slot)) return;

        Reward reward = slotRewardMap.get(slot);
        if (reward == null) return; // Not a reward slot

        // Reveal this slot
        revealedSlots.add(slot);
        clicksRemaining--;

        if (reward.getDisplay() != null) {
            inventory.setItem(slot, ItemBuilder.fromDisplay(reward.getDisplay()));
        }
        SoundUtil.play(player, animation.getRevealSounds());

        if (clicksRemaining <= 0) {
            // All clicks used - show results then close
            phase = Phase.DONE;

            // Schedule close after showRevealedItemsFor ticks
            type.getScheduler().entitySpecificScheduler(player)
                    .runDelayed(() -> {
                        finished.set(true);
                        player.closeInventory();
                    }, null, Math.max(1, animation.getShowRevealedItemsFor()));
        } else {
            // Update title with remaining clicks
            updateTitle();
        }
    }

    private void updateTitle() {
        String title = animation.getGuiTitle()
                .replace("<crate>", crate.getName())
                .replace("<amount>", String.valueOf(clicksRemaining));
        Inventory newInv = Bukkit.createInventory(this, animation.getGuiRows() * 9, TextUtil.parse(title));

        // Copy contents
        for (int i = 0; i < inventory.getSize(); i++) {
            newInv.setItem(i, inventory.getItem(i));
        }
        inventory = newInv;
        player.openInventory(inventory);
    }

    private void fillBackground() {
        fillBackground(inventory);
    }

    private void fillBackground(Inventory inv) {
        if (animation.getGuiFill() != null) {
            ItemStack fill = ItemBuilder.fromGuiItemConfig(animation.getGuiFill());
            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, fill);
            }
        }
    }

    private ItemStack createDefaultHideItem() {
        return ItemBuilder.create("YELLOW_STAINED_GLASS_PANE", "<yellow>Click to reveal!", null);
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
        // For click type, return first revealed reward
        if (!revealedSlots.isEmpty()) {
            return slotRewardMap.get(revealedSlots.iterator().next());
        }
        // Player closed early without revealing anything
        return fallbackReward;
    }

    /**
     * Gets all revealed rewards (click type can have multiple).
     */
    public List<Reward> getRevealedRewards() {
        return revealedSlots.stream()
                .map(slotRewardMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (event.getReason() == InventoryCloseEvent.Reason.OPEN_NEW) {
            return; // Inventory is transitioning (e.g. title update)
        }
        if (!finished.get()) {
            // Player closed early â€” stop everything, ensure a reward is granted
            cleanup();
            if (revealedSlots.isEmpty()) {
                fallbackReward = RewardSelector.selectWeighted(crate);
            }
            finished.set(true);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (phase == Phase.CLICKING && !cancelled.get()) {
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < inventory.getSize()) {
                handleClick(slot);
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Phase getPhase() {
        return phase;
    }

    public boolean isRewardSlot(int slot) {
        return slotRewardMap.containsKey(slot);
    }
}
