package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.animation.GuiItemConfig;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.gui.ModernCratesGui;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.SoundUtil;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Animation session for the CSGO-type scrolling animation.
 * Matches the proven AitoCrates approach: items shift in from one side,
 * tick rate gradually increases to slow down, and whatever lands at
 * the pointer position becomes the reward.
 */
public class CsgoAnimationSession implements AnimationSession, ModernCratesGui {

    private final Player player;
    private final Crate crate;
    private final Animation animation;
    private final CsgoAnimationType type;

    private Inventory inventory;
    private ScheduledTask scrollTask;
    private ScheduledTask stayOpenFillerTask;
    private ScheduledTask closeTask;
    private Reward selectedReward;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    // Animation state — mirrors the old AitoCrates approach
    private int ticksPassed = 0;
    private int itemsTicked = 0;
    private int tickRate;
    private Reward[] displayedRewards;
    private final Random random = new Random();

    // Pre-built filler items
    private List<ItemStack> fillerItemStacks;

    private static final int HARDCODED_STOP_THRESHOLD = 1200;

    public CsgoAnimationSession(Player player, Crate crate, Animation animation, CsgoAnimationType type) {
        this.player = player;
        this.crate = crate;
        this.animation = animation;
        this.type = type;
        this.tickRate = Math.max(1, animation.getStartTickRate());
    }

    @Override
    public void start() {
        List<Reward> pool = new ArrayList<>(crate.getRewards().values());
        if (pool.isEmpty()) {
            finished.set(true);
            return;
        }

        // Build filler items list (from config or default colored glass panes)
        buildFillerItems();

        // Initialize displayed rewards array
        List<Integer> rewardSlots = animation.getRewardSlots();
        displayedRewards = new Reward[rewardSlots.size()];

        // Create inventory
        String title = animation.getGuiTitle().replace("<crate>", crate.getName());
        inventory = Bukkit.createInventory(this, animation.getGuiRows() * 9, TextUtil.parse(title));

        // Fill background with gui_fill
        fillBackground();

        // Place pointer items
        placePointers();

        // Fill filler slots with initial random items
        fillFillerSlots();

        // Initialize reward slots with random rewards
        for (int i = 0; i < rewardSlots.size(); i++) {
            Reward reward = RewardSelector.selectWeighted(crate);
            displayedRewards[i] = reward;
            int slot = rewardSlots.get(i);
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, buildRewardDisplay(reward));
            }
        }

        player.openInventory(inventory);

        // Start tick loop (runs every server tick = 1 tick interval)
        scrollTask = type.getScheduler().entitySpecificScheduler(player)
                .runAtFixedRate(this::tick, null, 1L, 1L);
    }

    private void tick() {
        if (cancelled.get()) {
            cancelAllTasks();
            return;
        }

        if (ticksPassed % tickRate == 0) {
            // Animate filler slots every item tick
            fillFillerSlots();

            // Shift rewards: move each reward one position to the left
            List<Integer> rewardSlots = animation.getRewardSlots();
            for (int i = 0; i < rewardSlots.size() - 1; i++) {
                displayedRewards[i] = displayedRewards[i + 1];
                inventory.setItem(rewardSlots.get(i), inventory.getItem(rewardSlots.get(i + 1)));
            }

            // Generate new random reward on the right edge
            Reward generatedReward = RewardSelector.selectWeighted(crate);
            displayedRewards[displayedRewards.length - 1] = generatedReward;
            inventory.setItem(rewardSlots.get(rewardSlots.size() - 1), buildRewardDisplay(generatedReward));

            // Play tick sound
            SoundUtil.play(player, animation.getTickSounds());

            itemsTicked++;

            if (itemsTicked >= animation.getTotalTicks()) {
                // Stop the scroll task
                if (scrollTask != null) {
                    scrollTask.cancel();
                    scrollTask = null;
                }

                // Delay by current tickRate before revealing reward
                // (prevents the last tick from ending the animation instantly)
                type.getScheduler().entitySpecificScheduler(player)
                        .runDelayed(() -> {
                            if (!cancelled.get()) {
                                finishAnimation();
                            }
                        }, null, Math.max(1, tickRate));
                return; // Don't increment ticksPassed anymore
            }
        }

        ticksPassed++;
        // Increase tick rate (slow down) every tickRateModifier ticks
        if (animation.getTickRateModifier() > 0 && ticksPassed % animation.getTickRateModifier() == 0) {
            tickRate++;
        }
        // Emergency stop to prevent infinite loops
        if (ticksPassed > HARDCODED_STOP_THRESHOLD) {
            if (scrollTask != null) {
                scrollTask.cancel();
                scrollTask = null;
            }
            if (!finished.get()) {
                finishAnimation();
            }
        }
    }

    /**
     * Called when the scroll animation completes (after tickRate delay).
     * Determines the reward, plays sounds, and starts the stay-open phase.
     */
    private void finishAnimation() {
        // Determine the winning reward from the pointer position
        int rewardIndex = animation.getRewardIndex() - 1; // 1-based to 0-based
        if (rewardIndex >= 0 && rewardIndex < displayedRewards.length && displayedRewards[rewardIndex] != null) {
            selectedReward = displayedRewards[rewardIndex];
        } else {
            // Fallback: select a random weighted reward
            selectedReward = RewardSelector.selectWeighted(crate);
        }

        // Play reward sound
        SoundUtil.play(player, animation.getRewardSounds());

        // Start the stay-open-after-reward phase
        stayOpenAfterReward();
    }

    /**
     * Handles the stay-open phase after the reward is determined.
     * Either shows end-of-animation items or keeps animating fillers,
     * then closes the inventory after the configured delay.
     */
    private void stayOpenAfterReward() {
        int stayTicks = animation.getStayOpenAfterRewardTicks();

        if (animation.getEndOfAnimationItem() != null
                && animation.getEndOfAnimationSlots() != null
                && !animation.getEndOfAnimationSlots().isEmpty()) {
            // Place static end-of-animation items
            ItemStack endItem = ItemBuilder.fromGuiItemConfig(animation.getEndOfAnimationItem());
            for (int slot : animation.getEndOfAnimationSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, endItem);
                }
            }
        } else {
            // Animate filler slots during stay-open period
            stayOpenFillerTask = type.getScheduler().entitySpecificScheduler(player)
                    .runAtFixedRate(this::fillFillerSlots, null, 1L, 1L);
        }

        // Schedule inventory close
        closeTask = type.getScheduler().entitySpecificScheduler(player)
                .runDelayed(() -> {
                    if (stayOpenFillerTask != null) {
                        stayOpenFillerTask.cancel();
                        stayOpenFillerTask = null;
                    }
                    finished.set(true);
                    player.closeInventory();
                }, null, Math.max(1, stayTicks));
    }

    /**
     * Fills filler slots with random items from the filler items list.
     * Called every item tick during scrolling and during stay-open phase.
     */
    private void fillFillerSlots() {
        List<Integer> fillerSlots = animation.getFillerSlots();
        if (fillerSlots == null || fillerSlots.isEmpty()
                || fillerItemStacks == null || fillerItemStacks.isEmpty()) return;

        for (int slot : fillerSlots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, fillerItemStacks.get(random.nextInt(fillerItemStacks.size())));
            }
        }
    }

    /**
     * Builds the list of filler ItemStacks from config or defaults.
     */
    private void buildFillerItems() {
        Map<String, GuiItemConfig> configItems = animation.getFillerItems();
        if (configItems != null && !configItems.isEmpty()) {
            fillerItemStacks = new ArrayList<>(configItems.values().stream()
                    .map(ItemBuilder::fromGuiItemConfig)
                    .toList());
        } else {
            // Default: all stained glass pane colors
            fillerItemStacks = List.of(
                    new ItemStack(Material.WHITE_STAINED_GLASS_PANE),
                    new ItemStack(Material.ORANGE_STAINED_GLASS_PANE),
                    new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE),
                    new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE),
                    new ItemStack(Material.YELLOW_STAINED_GLASS_PANE),
                    new ItemStack(Material.LIME_STAINED_GLASS_PANE),
                    new ItemStack(Material.PINK_STAINED_GLASS_PANE),
                    new ItemStack(Material.GRAY_STAINED_GLASS_PANE),
                    new ItemStack(Material.CYAN_STAINED_GLASS_PANE),
                    new ItemStack(Material.PURPLE_STAINED_GLASS_PANE),
                    new ItemStack(Material.BLUE_STAINED_GLASS_PANE),
                    new ItemStack(Material.BROWN_STAINED_GLASS_PANE),
                    new ItemStack(Material.GREEN_STAINED_GLASS_PANE),
                    new ItemStack(Material.RED_STAINED_GLASS_PANE),
                    new ItemStack(Material.BLACK_STAINED_GLASS_PANE)
            );
        }
    }

    private ItemStack buildRewardDisplay(Reward reward) {
        if (reward != null && reward.getDisplay() != null) {
            return ItemBuilder.fromDisplay(reward.getDisplay());
        }
        return new ItemStack(Material.STONE);
    }

    private void fillBackground() {
        GuiItemConfig fill = animation.getGuiFill();
        if (fill == null) return;
        ItemStack fillItem = ItemBuilder.fromGuiItemConfig(fill);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, fillItem);
        }
    }

    private void placePointers() {
        if (animation.getDownPointer() != null) {
            int slot = animation.getDownPointer().getSlot();
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, ItemBuilder.fromGuiItemConfig(animation.getDownPointer()));
            }
        }
        if (animation.getUpPointer() != null) {
            int slot = animation.getUpPointer().getSlot();
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, ItemBuilder.fromGuiItemConfig(animation.getUpPointer()));
            }
        }
    }

    private void cancelAllTasks() {
        if (scrollTask != null) { scrollTask.cancel(); scrollTask = null; }
        if (stayOpenFillerTask != null) { stayOpenFillerTask.cancel(); stayOpenFillerTask = null; }
        if (closeTask != null) { closeTask.cancel(); closeTask = null; }
    }

    @Override
    public void cancel() {
        cancelled.set(true);
        cancelAllTasks();
    }

    @Override
    public boolean isFinished() {
        return finished.get();
    }

    @Override
    public Reward getSelectedReward() {
        return selectedReward;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // CSGO animation: no clicking allowed, event is already cancelled by listener
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (!finished.get()) {
            // Player closed early — stop everything and determine reward
            cancelAllTasks();
            if (selectedReward == null) {
                int ri = animation.getRewardIndex() - 1;
                if (displayedRewards != null && ri >= 0 && ri < displayedRewards.length
                        && displayedRewards[ri] != null) {
                    selectedReward = displayedRewards[ri];
                } else {
                    selectedReward = RewardSelector.selectWeighted(crate);
                }
            }
            SoundUtil.play(player, animation.getRewardSounds());
            finished.set(true);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
