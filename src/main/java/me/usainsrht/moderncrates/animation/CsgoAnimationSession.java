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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Animation session for the CSGO-type scrolling animation.
 * Used for both horizontal CSGO scroll and vertical roulette systems.
 */
public class CsgoAnimationSession implements AnimationSession, ModernCratesGui {

    private final Player player;
    private final Crate crate;
    private final Animation animation;
    private final CsgoAnimationType type;

    private Inventory inventory;
    private ScheduledTask task;
    private Reward selectedReward;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    // Animation state
    private int currentTick = 0;
    private int currentTickRate;
    private int ticksSinceLastRateChange = 0;
    private List<Reward> scrollRewards;
    private int scrollOffset = 0;

    public CsgoAnimationSession(Player player, Crate crate, Animation animation, CsgoAnimationType type) {
        this.player = player;
        this.crate = crate;
        this.animation = animation;
        this.type = type;
        this.currentTickRate = animation.getStartTickRate();
    }

    @Override
    public void start() {
        // Select the winning reward
        selectedReward = RewardSelector.selectWeighted(crate);
        if (selectedReward == null) {
            finished.set(true);
            return;
        }

        // Generate scroll rewards (enough to fill the animation)
        int slotCount = animation.getRewardSlots().size();
        int totalNeeded = slotCount + animation.getTotalTicks() + 10;
        scrollRewards = new ArrayList<>();
        List<Reward> pool = new ArrayList<>(crate.getRewards().values());
        if (pool.isEmpty()) {
            finished.set(true);
            return;
        }

        Random rand = ThreadLocalRandom.current();
        for (int i = 0; i < totalNeeded; i++) {
            scrollRewards.add(pool.get(rand.nextInt(pool.size())));
        }

        // Place the winning reward at the correct position
        int rewardIndex = animation.getRewardIndex() - 1; // 1-based to 0-based
        int winPosition = animation.getTotalTicks() + rewardIndex;
        if (winPosition >= 0 && winPosition < scrollRewards.size()) {
            scrollRewards.set(winPosition, selectedReward);
        }

        // Create inventory
        String title = animation.getGuiTitle().replace("<crate>", crate.getName());
        int rows = animation.getGuiRows();
        inventory = Bukkit.createInventory(this, rows * 9, TextUtil.parse(title));

        // Fill background
        fillBackground();

        // Place pointers
        placePointers();

        // Initial draw
        drawSlots();

        player.openInventory(inventory);

        // Start tick loop with MorePaperLib
        task = type.getScheduler().entitySpecificScheduler(player)
                .runAtFixedRate(() -> tick(), null, 1L, 1L);
    }

    private void tick() {
        if (cancelled.get()) {
            cleanup();
            return;
        }

        ticksSinceLastRateChange++;

        // Check if we should advance
        if (ticksSinceLastRateChange < currentTickRate) {
            return;
        }
        ticksSinceLastRateChange = 0;
        currentTick++;

        if (currentTick <= animation.getTotalTicks()) {
            // Advance scroll
            scrollOffset++;
            drawSlots();

            // Play tick sound
            SoundUtil.play(player, animation.getTickSounds());

            // Increase tick rate (slow down)
            if (animation.getTickRateModifier() > 0 && currentTick % animation.getTickRateModifier() == 0) {
                currentTickRate++;
            }
        } else if (currentTick == animation.getTotalTicks() + 1) {
            // Animation finished - show reward
            SoundUtil.play(player, animation.getRewardSounds());

            // Apply end-of-animation items if configured
            if (animation.getEndOfAnimationItem() != null && animation.getEndOfAnimationSlots() != null) {
                ItemStack endItem = ItemBuilder.fromGuiItemConfig(animation.getEndOfAnimationItem());
                for (int slot : animation.getEndOfAnimationSlots()) {
                    if (slot >= 0 && slot < inventory.getSize()) {
                        inventory.setItem(slot, endItem);
                    }
                }
            }
        } else if (currentTick > animation.getTotalTicks() + animation.getStayOpenAfterRewardTicks()) {
            // Time to close
            finished.set(true);
            player.closeInventory();
            cleanup();
        }
    }

    private void drawSlots() {
        List<Integer> slots = animation.getRewardSlots();
        for (int i = 0; i < slots.size(); i++) {
            int rewardIdx = scrollOffset + i;
            if (rewardIdx >= 0 && rewardIdx < scrollRewards.size()) {
                Reward reward = scrollRewards.get(rewardIdx);
                ItemStack displayItem = reward.getDisplay() != null
                        ? ItemBuilder.fromDisplay(reward.getDisplay())
                        : new ItemStack(Material.STONE);
                int slot = slots.get(i);
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, displayItem);
                }
            }
        }
    }

    private void fillBackground() {
        GuiItemConfig fill = animation.getGuiFill();
        if (fill == null) return;
        ItemStack fillItem = ItemBuilder.fromGuiItemConfig(fill);

        // Fill all slots first
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, fillItem);
        }

        // Fill filler slots with animated items
        List<Integer> fillerSlots = animation.getFillerSlots();
        if (fillerSlots != null && !fillerSlots.isEmpty()) {
            Map<String, GuiItemConfig> fillerItems = animation.getFillerItems();
            if (fillerItems != null && !fillerItems.isEmpty()) {
                List<ItemStack> fillerItemStacks = fillerItems.values().stream()
                        .map(ItemBuilder::fromGuiItemConfig)
                        .toList();
                Random rand = ThreadLocalRandom.current();
                for (int slot : fillerSlots) {
                    if (slot >= 0 && slot < inventory.getSize()) {
                        inventory.setItem(slot, fillerItemStacks.get(rand.nextInt(fillerItemStacks.size())));
                    }
                }
            } else {
                // Default: use all stained glass pane colors
                Material[] panes = {
                        Material.WHITE_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
                        Material.MAGENTA_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                        Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE,
                        Material.PINK_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
                        Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE,
                        Material.PURPLE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE,
                        Material.BROWN_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE,
                        Material.RED_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE
                };
                Random rand = ThreadLocalRandom.current();
                for (int slot : fillerSlots) {
                    if (slot >= 0 && slot < inventory.getSize()) {
                        inventory.setItem(slot, new ItemStack(panes[rand.nextInt(panes.length)]));
                    }
                }
            }
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

    private void cleanup() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void cancel() {
        cancelled.set(true);
        cleanup();
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
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
