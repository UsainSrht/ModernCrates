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
 * Multiple columns of rewards spin vertically like a slot machine.
 * Columns stop sequentially; if the winner row matches, the player wins.
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

    private int ticksPassed = 0;
    private int shiftsDone = 0;
    private int tickRate;
    private final Random random = new Random();

    // Column state
    private final Map<String, Reward[]> columnRewards = new LinkedHashMap<>();
    private final Set<String> stoppedColumns = new LinkedHashSet<>();
    private final Map<String, Integer> columnStopThresholds = new LinkedHashMap<>();

    // Pre-determined outcome
    private boolean willMatch;
    private Reward matchReward;

    // Filler
    private List<ItemStack> fillerItemStacks;

    private static final int HARDCODED_STOP_THRESHOLD = 1200;

    public SlotAnimationSession(Player player, Crate crate, Animation animation, SlotAnimationType type) {
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

        // Pre-determine outcome
        willMatch = random.nextDouble() * 100 < animation.getMatchChance();
        if (willMatch) {
            matchReward = RewardSelector.selectWeighted(crate);
        }

        buildFillerItems();

        // Calculate stop thresholds for each column
        List<String> allColumnNames = new ArrayList<>(animation.getSlotColumns().keySet());
        for (int i = 0; i < allColumnNames.size(); i++) {
            columnStopThresholds.put(allColumnNames.get(i),
                    animation.getTotalTicks() + i * animation.getColumnStopDelayTicks());
        }

        // Initialize column rewards
        for (var entry : animation.getSlotColumns().entrySet()) {
            int size = entry.getValue().size();
            Reward[] rewards = new Reward[size];
            for (int i = 0; i < size; i++) {
                rewards[i] = RewardSelector.selectWeighted(crate);
            }
            columnRewards.put(entry.getKey(), rewards);
        }

        // Create inventory
        String title = animation.getGuiTitle().replace("<crate>", crate.getName());
        inventory = Bukkit.createInventory(this, animation.getGuiRows() * 9, TextUtil.parse(title));

        fillBackground();
        placePointers();
        fillFillerSlots();
        updateAllColumnDisplays();

        player.openInventory(inventory);

        // Start scrolling
        scrollTask = type.getScheduler().entitySpecificScheduler(player)
                .runAtFixedRate(this::tick, null, 1L, 1L);
    }

    private void tick() {
        if (cancelled.get()) {
            cancelAllTasks();
            return;
        }

        if (ticksPassed % tickRate == 0) {
            // Shift all active columns downward
            for (var entry : animation.getSlotColumns().entrySet()) {
                String colName = entry.getKey();
                if (stoppedColumns.contains(colName)) continue;

                List<Integer> slots = entry.getValue();
                Reward[] rewards = columnRewards.get(colName);

                // Shift down: bottom discarded, each gets previous, top gets new
                for (int i = rewards.length - 1; i > 0; i--) {
                    rewards[i] = rewards[i - 1];
                }
                rewards[0] = RewardSelector.selectWeighted(crate);

                // Update inventory
                for (int i = 0; i < slots.size() && i < rewards.length; i++) {
                    int slot = slots.get(i);
                    if (slot >= 0 && slot < inventory.getSize()) {
                        inventory.setItem(slot, buildRewardDisplay(rewards[i]));
                    }
                }
            }

            fillFillerSlots();
            SoundUtil.play(player, animation.getTickSounds());
            shiftsDone++;

            // Check for column stops
            for (var entry : columnStopThresholds.entrySet()) {
                if (!stoppedColumns.contains(entry.getKey()) && shiftsDone >= entry.getValue()) {
                    stopColumn(entry.getKey());
                }
            }

            // Check if all stopped
            if (stoppedColumns.size() >= animation.getSlotColumns().size()) {
                if (scrollTask != null) {
                    scrollTask.cancel();
                    scrollTask = null;
                }
                type.getScheduler().entitySpecificScheduler(player)
                        .runDelayed(() -> {
                            if (!cancelled.get()) finishAnimation();
                        }, null, Math.max(1, tickRate));
                return;
            }
        }

        ticksPassed++;
        if (animation.getTickRateModifier() > 0 && ticksPassed % animation.getTickRateModifier() == 0) {
            tickRate++;
        }
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

    private void stopColumn(String columnName) {
        stoppedColumns.add(columnName);

        if (willMatch && animation.getRewardWinnerColumns().contains(columnName)) {
            // Force winner position to matchReward
            Reward[] rewards = columnRewards.get(columnName);
            int winnerIdx = animation.getRewardWinnerIndex();
            if (rewards != null && winnerIdx >= 0 && winnerIdx < rewards.length) {
                rewards[winnerIdx] = matchReward;
                List<Integer> slots = animation.getSlotColumns().get(columnName);
                if (winnerIdx < slots.size()) {
                    int slot = slots.get(winnerIdx);
                    if (slot >= 0 && slot < inventory.getSize()) {
                        inventory.setItem(slot, buildRewardDisplay(matchReward));
                    }
                }
            }
        }

        SoundUtil.play(player, animation.getRewardSounds());
    }

    private void finishAnimation() {
        // Check if all winner columns match
        List<String> winnerCols = animation.getRewardWinnerColumns();
        int winnerIdx = animation.getRewardWinnerIndex();

        Set<String> winnerRewardIds = new HashSet<>();
        Reward firstWinnerReward = null;

        for (String colName : winnerCols) {
            Reward[] rewards = columnRewards.get(colName);
            if (rewards != null && winnerIdx >= 0 && winnerIdx < rewards.length) {
                Reward r = rewards[winnerIdx];
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
            SoundUtil.play(player, animation.getRewardSounds());
        } else {
            // Ensure winner positions actually differ (avoid accidental match on a loss)
            if (!willMatch && winnerRewardIds.size() <= 1 && winnerCols.size() > 1 && firstWinnerReward != null) {
                // Force last column's winner to differ
                String lastCol = winnerCols.get(winnerCols.size() - 1);
                Reward[] lastRewards = columnRewards.get(lastCol);
                if (lastRewards != null && winnerIdx >= 0 && winnerIdx < lastRewards.length) {
                    Reward different = findDifferentReward(firstWinnerReward);
                    if (different != null) {
                        lastRewards[winnerIdx] = different;
                        List<Integer> slots = animation.getSlotColumns().get(lastCol);
                        if (winnerIdx < slots.size()) {
                            int slot = slots.get(winnerIdx);
                            if (slot >= 0 && slot < inventory.getSize()) {
                                inventory.setItem(slot, buildRewardDisplay(different));
                            }
                        }
                    }
                }
            }
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

    private void updateAllColumnDisplays() {
        for (var entry : animation.getSlotColumns().entrySet()) {
            List<Integer> slots = entry.getValue();
            Reward[] rewards = columnRewards.get(entry.getKey());
            for (int i = 0; i < slots.size() && i < rewards.length; i++) {
                int slot = slots.get(i);
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, buildRewardDisplay(rewards[i]));
                }
            }
        }
    }

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
