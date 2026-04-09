package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateLocation;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.SoundUtil;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lidded;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Animation session for the Item Rise animation.
 * Opens a lidded container block, spawns an item display entity that rises
 * from the container while cycling through rewards with spiral particles.
 * The final item at the top is the player's prize.
 */
public class ItemRiseAnimationSession implements AnimationSession {

    private final Player player;
    private final Crate crate;
    private final Animation animation;
    private final ItemRiseAnimationType type;
    private final Runnable onComplete;

    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Random random = new Random();

    private Reward selectedReward;
    private ScheduledTask openDelayTask;
    private ScheduledTask riseDelayTask;
    private ScheduledTask riseTask;
    private ScheduledTask settleTask;
    private ItemDisplay itemDisplay;
    private TextDisplay textDisplay;
    private Lidded liddedBlock;
    private Location blockLocation;

    // Rise state
    private int ticksPassed = 0;
    private int cycleIndex = 0;
    private List<Reward> cycleRewards;
    private double currentY;
    private double risePerTick;
    private double baseX;
    private double baseZ;

    public ItemRiseAnimationSession(Player player, Crate crate, Animation animation,
                                     ItemRiseAnimationType type, Runnable onComplete) {
        this.player = player;
        this.crate = crate;
        this.animation = animation;
        this.type = type;
        this.onComplete = onComplete;
    }

    @Override
    public void start() {
        // Validate physical crate with lidded block
        CrateLocation crateLoc = crate.getCrateLocation();
        if (crateLoc == null) {
            fallbackFinish();
            return;
        }

        Location loc = crateLoc.toBukkit();
        if (loc == null) {
            fallbackFinish();
            return;
        }

        blockLocation = loc;
        Block block = loc.getBlock();
        BlockState state = block.getState();
        if (!(state instanceof Lidded lidded)) {
            fallbackFinish();
            return;
        }
        this.liddedBlock = lidded;

        // Pre-select reward
        selectedReward = RewardSelector.selectWeighted(crate);
        if (selectedReward == null) {
            fallbackFinish();
            return;
        }

        // Generate reward cycle list
        cycleRewards = generateCycleList();

        // Schedule block opening on the region thread that owns the crate location
        int openDelay = Math.max(1, animation.getBlockOpenDelayTicks());
        openDelayTask = type.getScheduler().regionSpecificScheduler(blockLocation)
                .runDelayed(this::openBlock, openDelay);
    }

    private void openBlock() {
        if (cancelled.get()) return;

        liddedBlock.open();
        SoundUtil.play(player, animation.getRiseSounds());

        // Schedule rise start on the region thread
        int riseDelay = Math.max(1, animation.getRiseStartDelayTicks());
        riseDelayTask = type.getScheduler().regionSpecificScheduler(blockLocation)
                .runDelayed(this::spawnAndRise, riseDelay);
    }

    private void spawnAndRise() {
        if (cancelled.get()) return;

        World world = blockLocation.getWorld();
        if (world == null) {
            fallbackFinish();
            return;
        }

        baseX = blockLocation.getBlockX() + 0.5;
        baseZ = blockLocation.getBlockZ() + 0.5;
        currentY = blockLocation.getBlockY() + 1.0;
        risePerTick = animation.getRiseHeight() / animation.getRiseTicks();

        Reward firstReward = cycleRewards.isEmpty() ? selectedReward : cycleRewards.get(0);
        ItemStack displayItem = firstReward.getDisplay() != null
                ? ItemBuilder.fromDisplay(firstReward.getDisplay())
                : new ItemStack(org.bukkit.Material.STONE);
        String displayName = getRewardName(firstReward);

        Location spawnLoc = new Location(world, baseX, currentY, baseZ);

        itemDisplay = world.spawn(spawnLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(displayItem);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
        });

        textDisplay = world.spawn(spawnLoc.clone().add(0, 0.6, 0), TextDisplay.class, entity -> {
            entity.text(TextUtil.parse(displayName));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        });

        // Start rise animation loop on the region thread
        riseTask = type.getScheduler().regionSpecificScheduler(blockLocation)
                .runAtFixedRate(this::riseTick, 1L, 1L);
    }

    private void riseTick() {
        if (cancelled.get()) {
            cleanupEntities();
            return;
        }

        ticksPassed++;
        currentY += risePerTick;

        // Move entities upward
        World world = blockLocation.getWorld();
        if (world != null) {
            if (itemDisplay != null && !itemDisplay.isDead()) {
                itemDisplay.teleportAsync(new Location(world, baseX, currentY, baseZ));
            }
            if (textDisplay != null && !textDisplay.isDead()) {
                textDisplay.teleportAsync(new Location(world, baseX, currentY + 0.6, baseZ));
            }
        }

        // Cycle displayed item
        int cycleTicks = Math.max(1, animation.getCycleTicks());
        if (ticksPassed % cycleTicks == 0 && !cycleRewards.isEmpty()) {
            cycleIndex = (cycleIndex + 1) % cycleRewards.size();
            Reward current = cycleRewards.get(cycleIndex);

            if (itemDisplay != null && !itemDisplay.isDead() && current.getDisplay() != null) {
                itemDisplay.setItemStack(ItemBuilder.fromDisplay(current.getDisplay()));
            }
            if (textDisplay != null && !textDisplay.isDead()) {
                textDisplay.text(TextUtil.parse(getRewardName(current)));
            }
            SoundUtil.play(player, animation.getTickSounds());
        }

        // Spawn spiral particles
        spawnParticles(world);

        // Check if rise is complete
        if (ticksPassed >= animation.getRiseTicks()) {
            if (riseTask != null) {
                riseTask.cancel();
                riseTask = null;
            }
            settleReward();
        }
    }

    private void spawnParticles(World world) {
        if (world == null) return;

        String particleTypeName = animation.getParticleType();
        if (particleTypeName == null || particleTypeName.isEmpty()) return;

        Particle particle;
        try {
            particle = Particle.valueOf(particleTypeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }

        double radius = animation.getParticleSpiralRadius();
        double speed = animation.getParticleSpiralSpeed();
        int count = Math.max(1, animation.getParticleCount());

        for (int i = 0; i < count; i++) {
            double angle = (ticksPassed * speed) + (i * 2 * Math.PI / count);
            double px = baseX + radius * Math.cos(angle);
            double pz = baseZ + radius * Math.sin(angle);
            world.spawnParticle(particle, px, currentY, pz, 1, 0, 0, 0, 0);
        }
    }

    private void settleReward() {
        // Show final reward
        if (itemDisplay != null && !itemDisplay.isDead() && selectedReward.getDisplay() != null) {
            itemDisplay.setItemStack(ItemBuilder.fromDisplay(selectedReward.getDisplay()));
        }
        if (textDisplay != null && !textDisplay.isDead()) {
            textDisplay.text(TextUtil.parse(getRewardName(selectedReward)));
        }

        SoundUtil.play(player, animation.getSettleSounds());
        SoundUtil.play(player, animation.getRewardSounds());

        // Wait, then cleanup on the region thread and complete on the player thread
        int settleTicks = Math.max(1, animation.getSettleDisplayTicks());
        settleTask = type.getScheduler().regionSpecificScheduler(blockLocation)
                .runDelayed(() -> {
                    cleanupEntities();
                    if (liddedBlock != null) {
                        try {
                            liddedBlock.close();
                        } catch (Exception ignored) {}
                    }
                    finished.set(true);
                    type.getScheduler().entitySpecificScheduler(player)
                            .run(() -> onComplete.run(), null);
                }, settleTicks);
    }

    private void fallbackFinish() {
        if (selectedReward == null) {
            selectedReward = RewardSelector.selectWeighted(crate);
        }
        finished.set(true);
        // Schedule onComplete for next tick to avoid re-entrant issues
        type.getScheduler().entitySpecificScheduler(player)
                .runDelayed(() -> onComplete.run(), null, 1L);
    }

    private void cleanupEntities() {
        if (itemDisplay != null && !itemDisplay.isDead()) {
            itemDisplay.remove();
            itemDisplay = null;
        }
        if (textDisplay != null && !textDisplay.isDead()) {
            textDisplay.remove();
            textDisplay = null;
        }
    }

    private List<Reward> generateCycleList() {
        List<Reward> cycle = new ArrayList<>();
        List<Reward> pool = new ArrayList<>(crate.getRewards().values());
        if (pool.isEmpty()) return cycle;

        int totalCycles = Math.max(1, animation.getRiseTicks() / Math.max(1, animation.getCycleTicks()));
        for (int i = 0; i < totalCycles - 1; i++) {
            cycle.add(pool.get(random.nextInt(pool.size())));
        }
        cycle.add(selectedReward); // last cycle shows actual reward
        return cycle;
    }

    private String getRewardName(Reward reward) {
        if (reward.getDisplay() != null && reward.getDisplay().getName() != null) {
            return reward.getDisplay().getName();
        }
        return reward.getId();
    }

    @Override
    public void cancel() {
        cancelled.set(true);
        if (openDelayTask != null) { openDelayTask.cancel(); openDelayTask = null; }
        if (riseDelayTask != null) { riseDelayTask.cancel(); riseDelayTask = null; }
        if (riseTask != null) { riseTask.cancel(); riseTask = null; }
        if (settleTask != null) { settleTask.cancel(); settleTask = null; }
        // Run entity/block cleanup on the region thread for Folia compatibility
        if (blockLocation != null) {
            type.getScheduler().regionSpecificScheduler(blockLocation)
                    .run(() -> {
                        cleanupEntities();
                        if (liddedBlock != null) {
                            try {
                                liddedBlock.close();
                            } catch (Exception ignored) {}
                        }
                    });
        } else {
            cleanupEntities();
        }
        if (selectedReward == null) {
            selectedReward = RewardSelector.selectWeighted(crate);
        }
        finished.set(true);
    }

    @Override
    public boolean isFinished() {
        return finished.get();
    }

    @Override
    public Reward getSelectedReward() {
        return selectedReward;
    }
}
