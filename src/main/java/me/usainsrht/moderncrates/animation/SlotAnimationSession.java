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
 * Animation session for the Slot-type animation.
 * Each column is an independent reel with its own circular reward strip.
 * Reels spin at full speed, decelerate independently, and stop sequentially
 * (left-to-right) to simulate real slot machine mechanics.
 */
public class SlotAnimationSession implements AnimationSession, ModernCratesGui {

    private final Player player;
    private final Crate crate;
    private final Animation animation;
    private final SlotAnimationType type;

    private Inventory inventory;
    private ScheduledTask scrollTask;
    private ScheduledTask stayOpenFillerTask;
    private ScheduledTask closeTask;
    private Reward selectedReward;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final Random random = new Random();
    private final List<ReelState> reels = new ArrayList<>();
    private int reelsStoppedCount = 0;
    private int globalTickCounter = 0;

    // Pre-determined outcome
    private boolean willMatch;
    private Reward matchReward;

    // Filler
    private List<ItemStack> fillerItemStacks;
    private final Set<Integer> reservedFillerSlots = new HashSet<>();

    private static final int REEL_STRIP_SIZE = 40;
    private static final int HARDCODED_TICK_LIMIT = 2000;

    public SlotAnimationSession(Player player, Crate crate, Animation animation, SlotAnimationType type) {
        this.player = player;
        this.crate = crate;
        this.animation = animation;
        this.type = type;
    }

    /**
     * Tracks the independent state of a single slot machine reel (column).
     * Each reel has its own circular strip of rewards, speed, and deceleration.
     */
    private static class ReelState {
        final String name;
        final List<Integer> guiSlots;
        final Reward[] strip;
        int offset;
        int tickRate;
        int tickCounter;
        int shiftsDone;
        final int targetShifts;
        final int decelerationStart;
        boolean stopped;

        ReelState(String name, List<Integer> guiSlots, Reward[] strip,
                  int startTickRate, int targetShifts, int decelerationStart) {
            this.name = name;
            this.guiSlots = guiSlots;
            this.strip = strip;
            this.offset = 0;
            this.tickRate = startTickRate;
            this.tickCounter = 0;
            this.shiftsDone = 0;
            this.targetShifts = targetShifts;
            this.decelerationStart = decelerationStart;
            this.stopped = false;
        }

        Reward getVisible(int i) {
            return strip[((offset + i) % strip.length + strip.length) % strip.length];
        }
    }

    @Override
    public void start() {
        List<Reward> pool = new ArrayList<>(crate.getRewards().values());
        if (pool.isEmpty()) {
            finished.set(true);
            return;
        }

        // Pre-determine outcome
        willMatch = random.nextDouble() * 100 < animation.getMatchChance();
        if (willMatch) {
            matchReward = RewardSelector.selectWeighted(crate);
        }

        buildFillerItems();
        buildReels();
        rebuildReservedFillerSlots();

        // If NOT a match, ensure winner row rewards don't accidentally all match
        if (!willMatch && reels.size() > 1) {
            ensureNoAccidentalMatch();
        }

        // Create inventory
        String title = animation.getGuiTitle().replace("<crate>", crate.getName());
        inventory = Bukkit.createInventory(this, animation.getGuiRows() * 9, TextUtil.parse(title));

        fillBackground();
        placePointers();
        fillFillerSlots();
        updateAllReelDisplays();

        player.openInventory(inventory);

        // Start the master tick loop (runs every server tick)
        scrollTask = type.getScheduler().entitySpecificScheduler(player)
                .runAtFixedRate(this::tick, null, 1L, 1L);
    }

    private void buildReels() {
        List<String> columnNames = new ArrayList<>(animation.getSlotColumns().keySet());
        int baseTargetShifts = animation.getTotalTicks();
        int startRate = Math.max(1, animation.getStartTickRate());
        int stopDelay = animation.getColumnStopDelayTicks();
        // Deceleration window: each reel slows down over the last N shifts before stopping
        int decelWindow = Math.max(5, stopDelay);

        for (int col = 0; col < columnNames.size(); col++) {
            String colName = columnNames.get(col);
            List<Integer> guiSlots = animation.getSlotColumns().get(colName);

            // Later reels spin longer before stopping
            int targetShifts = baseTargetShifts + col * stopDelay;
            int decelerationStart = Math.max(0, targetShifts - decelWindow);

            // Generate the circular reel strip with random weighted rewards
            Reward[] strip = new Reward[REEL_STRIP_SIZE];
            for (int i = 0; i < REEL_STRIP_SIZE; i++) {
                strip[i] = RewardSelector.selectWeighted(crate);
            }

            // Plant the winning reward at the exact landing position for winner columns
            // After targetShifts shifts, visible window starts at offset=targetShifts.
            // Winner row is a 0-based index inside each column list.
            if (willMatch && animation.getRewardWinnerColumns().contains(colName)) {
                int winnerIdx = getWinnerIndexForReel(guiSlots);
                int landingPos = (targetShifts + winnerIdx) % REEL_STRIP_SIZE;
                strip[landingPos] = matchReward;
            }

            reels.add(new ReelState(colName, guiSlots, strip, startRate, targetShifts, decelerationStart));
        }
    }

    private void ensureNoAccidentalMatch() {
        List<String> winnerCols = animation.getRewardWinnerColumns();
        if (winnerCols.size() < 2) return;

        // Find which reels correspond to the first and last winner columns
        ReelState firstWinnerReel = null;
        ReelState lastWinnerReel = null;
        for (ReelState reel : reels) {
            if (winnerCols.contains(reel.name)) {
                if (firstWinnerReel == null) firstWinnerReel = reel;
                lastWinnerReel = reel;
            }
        }
        if (firstWinnerReel == null || lastWinnerReel == null || firstWinnerReel == lastWinnerReel) return;

        int firstLandingPos = (firstWinnerReel.targetShifts + getWinnerIndexForReel(firstWinnerReel.guiSlots)) % REEL_STRIP_SIZE;
        Reward firstReward = firstWinnerReel.strip[firstLandingPos];

        int lastLandingPos = (lastWinnerReel.targetShifts + getWinnerIndexForReel(lastWinnerReel.guiSlots)) % REEL_STRIP_SIZE;
        Reward lastReward = lastWinnerReel.strip[lastLandingPos];

        if (firstReward != null && lastReward != null && firstReward.getId().equals(lastReward.getId())) {
            Reward different = findDifferentReward(firstReward);
            if (different != null) {
                lastWinnerReel.strip[lastLandingPos] = different;
            }
        }
    }

    /**
     * Master tick - called every server tick. Each reel advances independently
     * based on its own tick rate, decelerates on its own schedule, and stops
     * at its own target threshold.
     */
    private void tick() {
        if (cancelled.get()) {
            cancelAllTasks();
            return;
        }

        globalTickCounter++;
        boolean anyShifted = false;

        for (ReelState reel : reels) {
            if (reel.stopped) continue;

            reel.tickCounter++;

            if (reel.tickCounter % reel.tickRate == 0) {
                // Advance this reel by one position (items scroll downward)
                reel.offset++;
                reel.shiftsDone++;
                anyShifted = true;

                // Update the visible inventory slots for this reel
                updateReelDisplay(reel);

                // Deceleration: once past the decel start, increase tick rate periodically
                if (reel.shiftsDone >= reel.decelerationStart) {
                    int shiftsIntoDecel = reel.shiftsDone - reel.decelerationStart;
                    int decelMod = Math.max(1, animation.getTickRateModifier() > 0
                            ? animation.getTickRateModifier() : 2);
                    if (shiftsIntoDecel > 0 && shiftsIntoDecel % decelMod == 0) {
                        reel.tickRate++;
                    }
                }

                // Check if this reel has reached its stop target
                if (reel.shiftsDone >= reel.targetShifts) {
                    reel.stopped = true;
                    reelsStoppedCount++;
                    SoundUtil.play(player, animation.getRewardSounds());
                }
            }
        }

        if (anyShifted) {
            SoundUtil.play(player, animation.getTickSounds());
            fillFillerSlots();
        }

        // All reels have stopped - finish after a brief pause
        if (reelsStoppedCount >= reels.size()) {
            if (scrollTask != null) {
                scrollTask.cancel();
                scrollTask = null;
            }
            type.getScheduler().entitySpecificScheduler(player)
                    .runDelayed(() -> {
                        if (!cancelled.get()) finishAnimation();
                    }, null, 5L);
            return;
        }

        // Emergency timeout
        if (globalTickCounter > HARDCODED_TICK_LIMIT) {
            if (scrollTask != null) {
                scrollTask.cancel();
                scrollTask = null;
            }
            for (ReelState reel : reels) {
                reel.stopped = true;
            }
            if (!finished.get()) {
                finishAnimation();
            }
        }
    }

    private void updateReelDisplay(ReelState reel) {
        for (int i = 0; i < reel.guiSlots.size(); i++) {
            int slot = reel.guiSlots.get(i);
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, buildRewardDisplay(reel.getVisible(i)));
            }
        }
    }

    private void updateAllReelDisplays() {
        for (ReelState reel : reels) {
            updateReelDisplay(reel);
        }
    }

    private void finishAnimation() {
        List<String> winnerCols = animation.getRewardWinnerColumns();

        Set<String> winnerRewardIds = new HashSet<>();
        Reward firstWinnerReward = null;

        for (ReelState reel : reels) {
            if (winnerCols.contains(reel.name)) {
                int winnerIdx = getWinnerIndexForReel(reel.guiSlots);
                Reward r = reel.getVisible(winnerIdx);
                if (r != null) {
                    winnerRewardIds.add(r.getId());
                    if (firstWinnerReward == null) firstWinnerReward = r;
                }
            }
        }

        boolean allMatch = winnerRewardIds.size() == 1 && firstWinnerReward != null;

        if (allMatch) {
            selectedReward = firstWinnerReward;
            SoundUtil.play(player, animation.getWinSounds());
        } else {
            selectedReward = RewardSelector.selectWeighted(crate);
            SoundUtil.play(player, animation.getLoseSounds());
        }

        stayOpenAfterReward(allMatch);
    }

    private Reward findDifferentReward(Reward exclude) {
        for (Reward r : crate.getRewards().values()) {
            if (!r.getId().equals(exclude.getId())) return r;
        }
        return null;
    }

    private void stayOpenAfterReward(boolean won) {
        int stayTicks = animation.getStayOpenAfterRewardTicks();

        if (won && animation.getEndOfAnimationItem() != null
                && animation.getEndOfAnimationSlots() != null
                && !animation.getEndOfAnimationSlots().isEmpty()) {
            ItemStack endItem = ItemBuilder.fromGuiItemConfig(animation.getEndOfAnimationItem());
            for (int slot : animation.getEndOfAnimationSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, endItem);
                }
            }
        } else {
            stayOpenFillerTask = type.getScheduler().entitySpecificScheduler(player)
                    .runAtFixedRate(this::fillFillerSlots, null, 1L, 1L);
        }

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

    private void fillFillerSlots() {
        List<Integer> fillerSlots = animation.getFillerSlots();
        if (fillerSlots == null || fillerSlots.isEmpty()
                || fillerItemStacks == null || fillerItemStacks.isEmpty()) return;

        for (int slot : fillerSlots) {
            if (reservedFillerSlots.contains(slot)) continue;
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, fillerItemStacks.get(random.nextInt(fillerItemStacks.size())));
            }
        }
    }

    private int getWinnerIndexForReel(List<Integer> guiSlots) {
        if (guiSlots == null || guiSlots.isEmpty()) return 0;
        int configured = Math.max(0, animation.getRewardWinnerIndex());
        return Math.min(configured, guiSlots.size() - 1);
    }

    private void rebuildReservedFillerSlots() {
        reservedFillerSlots.clear();
        for (ReelState reel : reels) {
            reservedFillerSlots.addAll(reel.guiSlots);
        }
        if (animation.getDownPointer() != null) {
            reservedFillerSlots.add(animation.getDownPointer().getSlot());
        }
        if (animation.getUpPointer() != null) {
            reservedFillerSlots.add(animation.getUpPointer().getSlot());
        }
    }

    private void buildFillerItems() {
        Map<String, GuiItemConfig> configItems = animation.getFillerItems();
        if (configItems != null && !configItems.isEmpty()) {
            fillerItemStacks = new ArrayList<>(configItems.values().stream()
                    .map(ItemBuilder::fromGuiItemConfig).toList());
        } else {
            fillerItemStacks = List.of(
                    new ItemStack(Material.WHITE_STAINED_GLASS_PANE),
                    new ItemStack(Material.ORANGE_STAINED_GLASS_PANE),
                    new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE),
                    new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE),
                    new ItemStack(Material.YELLOW_STAINED_GLASS_PANE),
                    new ItemStack(Material.LIME_STAINED_GLASS_PANE),
                    new ItemStack(Material.PINK_STAINED_GLASS_PANE),
                    new ItemStack(Material.CYAN_STAINED_GLASS_PANE),
                    new ItemStack(Material.PURPLE_STAINED_GLASS_PANE),
                    new ItemStack(Material.BLUE_STAINED_GLASS_PANE),
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
        // Slot animation: no clicking allowed
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (!finished.get()) {
            cancelAllTasks();
            if (selectedReward == null) {
                selectedReward = RewardSelector.selectWeighted(crate);
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
