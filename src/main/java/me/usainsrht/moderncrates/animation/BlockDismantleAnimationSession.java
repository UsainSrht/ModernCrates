package me.usainsrht.moderncrates.animation;

import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationSession;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateLocation;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.SoundUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Animation session for the BlockDismantle animation.
 *
 * <p>Phases:
 * <ol>
 *   <li>SPAWN  – hide the real crate block (save BlockData, set to AIR), spawn 6 thin
 *       BlockDisplay entities that together form the visual shell of the block.</li>
 *   <li>DISMANTLE – side faces interpolate outward + downward; top face interpolates
 *       upward in a random horizontal direction then falls back down; bottom face stays.</li>
 *   <li>REWARD  – spawn up to {@code dismantleRewardCount} ItemDisplay entities inside
 *       the opened cavity with the selected reward item.</li>
 *   <li>CLEANUP – after {@code dismantleDisplayDurationTicks}, remove all display entities
 *       and restore the original block.</li>
 * </ol>
 *
 * <p>All BlockDisplay / block-mutating work is scheduled on the region-specific scheduler
 * so the session is fully Folia-compatible.
 */
public class BlockDismantleAnimationSession implements AnimationSession {

    // -----------------------------------------------------------------------
    // Face thickness – thin enough to look like a face, not a slab
    // -----------------------------------------------------------------------
    private static final float FACE_THICKNESS = 0.05f;

    // -----------------------------------------------------------------------
    // Injected state
    // -----------------------------------------------------------------------
    private final Player player;
    private final Crate crate;
    private final Animation animation;
    private final BlockDismantleAnimationType type;
    private final Runnable onComplete;

    // -----------------------------------------------------------------------
    // Runtime state
    // -----------------------------------------------------------------------
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Random random = new Random();

    private Location blockLocation;
    private BlockData savedBlockData;
    private Reward selectedReward;

    // The 6 shell faces: 0=bottom, 1=top, 2=north, 3=south, 4=west, 5=east
    private final List<BlockDisplay> faceDisplays = new ArrayList<>();
    private final List<ItemDisplay> rewardDisplays = new ArrayList<>();

    // Scheduled tasks
    private ScheduledTask dismantleTask;
    private ScheduledTask topFallTask;
    private ScheduledTask rewardTask;
    private ScheduledTask cleanupTask;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public BlockDismantleAnimationSession(Player player, Crate crate, Animation animation,
                                           BlockDismantleAnimationType type, Runnable onComplete) {
        this.player = player;
        this.crate = crate;
        this.animation = animation;
        this.type = type;
        this.onComplete = onComplete;
    }

    // ============================= LIFECYCLE ================================

    @Override
    public void start() {
        CrateLocation crateLoc = crate.getCrateLocation();
        if (crateLoc == null) { fallbackFinish(); return; }

        Location loc = crateLoc.toBukkit();
        if (loc == null) { fallbackFinish(); return; }

        blockLocation = loc.getBlock().getLocation(); // integer-aligned

        selectedReward = RewardSelector.selectWeighted(crate);
        if (selectedReward == null) { fallbackFinish(); return; }

        // Phase 1: hide block + spawn faces on the region thread
        type.getScheduler().regionSpecificScheduler(blockLocation)
                .run(this::spawnShell);
    }

    @Override
    public void cancel() {
        cancelled.set(true);
        cancelTasks();
        if (blockLocation != null) {
            type.getScheduler().regionSpecificScheduler(blockLocation)
                    .run(this::cleanupAll);
        } else {
            cleanupAll();
        }
        if (selectedReward == null) selectedReward = RewardSelector.selectWeighted(crate);
        finished.set(true);
    }

    @Override
    public boolean isFinished() { return finished.get(); }

    @Override
    public Reward getSelectedReward() { return selectedReward; }

    // ========================= PHASE 1 – SPAWN SHELL ========================

    /**
     * Replaces the crate block with AIR and spawns the 6 face displays.
     * Must execute on the region thread that owns {@code blockLocation}.
     */
    private void spawnShell() {
        if (cancelled.get()) return;

        World world = blockLocation.getWorld();
        if (world == null) { fallbackFinish(); return; }

        Block block = blockLocation.getBlock();
        savedBlockData = block.getBlockData().clone();

        // Resolve the face block material from config (fallback to BARREL)
        Material faceMaterial = Material.matchMaterial(animation.getDismantleBlockType().toUpperCase());
        if (faceMaterial == null || !faceMaterial.isBlock()) faceMaterial = Material.BARREL;

        BlockData faceBlockData = faceMaterial.createBlockData();

        // Hide real block
        block.setType(Material.AIR, false);

        // Spawn bottom face (stays put the entire animation)
        faceDisplays.add(spawnFace(world, faceBlockData, bottomTransform()));
        // Spawn top face (launches upward then falls)
        faceDisplays.add(spawnFace(world, faceBlockData, topTransform()));
        // North, South, West, East
        faceDisplays.add(spawnFace(world, faceBlockData, northTransform()));
        faceDisplays.add(spawnFace(world, faceBlockData, southTransform()));
        faceDisplays.add(spawnFace(world, faceBlockData, westTransform()));
        faceDisplays.add(spawnFace(world, faceBlockData, eastTransform()));

        SoundUtil.play(player, animation.getDismantleOpenSounds());

        // Brief pause so the initial spawn renders, then do dismantle
        int startDelay = Math.max(1, 5);
        dismantleTask = type.getScheduler().regionSpecificScheduler(blockLocation)
                .runDelayed(this::dismantle, startDelay);
    }

    // ========================= PHASE 2 – DISMANTLE ==========================

    /**
     * Initiates the interpolated dismantle movement on all 6 faces.
     */
    private void dismantle() {
        if (cancelled.get()) return;

        int fallTicks = Math.max(1, animation.getDismantleFallDurationTicks());
        int riseTicks = Math.max(1, animation.getDismantleTopRiseDurationTicks());
        float height = (float) animation.getDismantleTopLaunchHeight();
        float hRange = (float) animation.getDismantleTopHorizontalRange();

        // ---- Bottom (index 0) stays, no interpolation needed ----

        // ---- Top face (index 1) – rise first ----
        if (faceDisplays.size() > 1) {
            BlockDisplay topFace = faceDisplays.get(1);
            float offX = (random.nextFloat() * 2 - 1) * hRange;
            float offZ = (random.nextFloat() * 2 - 1) * hRange;

            // Phase A: fly upward in a random horizontal direction
            Transformation riseTarget = new Transformation(
                    new Vector3f(offX, height, offZ),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1f, FACE_THICKNESS, 1f),
                    new AxisAngle4f(0, 0, 1, 0)
            );
            applyInterpolation(topFace, riseTarget, riseTicks);

            // Phase B: after rise duration, fall back down past the ground
            topFallTask = type.getScheduler().regionSpecificScheduler(blockLocation)
                    .runDelayed(() -> {
                        if (cancelled.get() || topFace.isDead()) return;
                        Transformation fallTarget = new Transformation(
                                new Vector3f(offX, -2f, offZ),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(1f, FACE_THICKNESS, 1f),
                                new AxisAngle4f(0, 0, 1, 0)
                        );
                        applyInterpolation(topFace, fallTarget, fallTicks);
                    }, riseTicks);
        }

        // ---- Side faces: fall outward + downward ----
        // North (index 2): translate -Z and -Y
        animateSide(2, fallTicks, 0f, -0.8f, -1.5f);
        // South (index 3): translate +Z and -Y
        animateSide(3, fallTicks, 0f, -0.8f, 1.5f);
        // West  (index 4): translate -X and -Y
        animateSide(4, fallTicks, -1.5f, -0.8f, 0f);
        // East  (index 5): translate +X and -Y
        animateSide(5, fallTicks, 1.5f, -0.8f, 0f);

        // After fall, start the reward phase
        int rewardDelay = fallTicks + (int) (fallTicks * 0.2) + 1;
        rewardTask = type.getScheduler().regionSpecificScheduler(blockLocation)
                .runDelayed(this::showRewards, rewardDelay);
    }

    /**
     * Translates a side face to {@code (tx, ty, tz)} over {@code ticks} ticks,
     * additionally tilting it outward slightly so it "falls" convincingly.
     */
    private void animateSide(int faceIndex, int ticks, float tx, float ty, float tz) {
        if (faceDisplays.size() <= faceIndex) return;
        BlockDisplay face = faceDisplays.get(faceIndex);
        if (face.isDead()) return;

        // Determine scale/rotation based on which side it is
        Transformation current = face.getTransformation();

        Transformation target = new Transformation(
                new Vector3f(
                        current.getTranslation().x + tx,
                        current.getTranslation().y + ty,
                        current.getTranslation().z + tz
                ),
                new AxisAngle4f(0, 0, 1, 0),
                current.getScale(),
                new AxisAngle4f(0, 0, 1, 0)
        );
        applyInterpolation(face, target, ticks);
    }

    // ========================= PHASE 3 – SHOW REWARDS =======================

    /**
     * Spawns ItemDisplay entities inside the open cavity.
     */
    private void showRewards() {
        if (cancelled.get()) return;

        World world = blockLocation.getWorld();
        if (world == null) { beginCleanup(); return; }

        SoundUtil.play(player, animation.getDismantleRewardSounds());

        int count = Math.max(1, animation.getDismantleRewardCount());
        List<Reward> pool = new ArrayList<>(crate.getRewards().values());
        if (pool.isEmpty()) { beginCleanup(); return; }

        // Build the display list: selected reward first, rest random
        List<Reward> displayRewards = new ArrayList<>();
        displayRewards.add(selectedReward);
        for (int i = 1; i < count; i++) {
            displayRewards.add(pool.get(random.nextInt(pool.size())));
        }

        // Position items in a grid inside the 1×1 cavity
        // We spread them across the interior, centred at block centre
        double cx = blockLocation.getBlockX() + 0.5;
        double cy = blockLocation.getBlockY() + 0.3; // slightly above floor
        double cz = blockLocation.getBlockZ() + 0.5;

        // Simple spiral / grid placement within [-0.3, 0.3] XZ range
        double[] offsets = computeOffsets(count);

        for (int i = 0; i < count; i++) {
            Reward reward = displayRewards.get(i);
            ItemStack displayItem = reward.getDisplay() != null
                    ? ItemBuilder.fromDisplay(reward.getDisplay())
                    : new ItemStack(Material.STONE);

            Location spawnLoc = new Location(world,
                    cx + offsets[i * 2],
                    cy + (i * 0.18),            // stack slightly in Y as well
                    cz + offsets[i * 2 + 1]);

            ItemDisplay itemDisplay = world.spawn(spawnLoc, ItemDisplay.class, e -> {
                e.setItemStack(displayItem);
                e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
                e.setBillboard(Display.Billboard.VERTICAL);
                e.setViewRange(48);
            });
            rewardDisplays.add(itemDisplay);
        }

        SoundUtil.play(player, animation.getDismantleSettleSounds());

        // Schedule cleanup
        int displayTicks = Math.max(1, animation.getDismantleDisplayDurationTicks());
        cleanupTask = type.getScheduler().regionSpecificScheduler(blockLocation)
                .runDelayed(this::finalCleanup, displayTicks);
    }

    // ========================= PHASE 4 – CLEANUP ============================

    private void beginCleanup() {
        int displayTicks = Math.max(1, animation.getDismantleDisplayDurationTicks());
        cleanupTask = type.getScheduler().regionSpecificScheduler(blockLocation)
                .runDelayed(this::finalCleanup, displayTicks);
    }

    private void finalCleanup() {
        cleanupAll();
        finished.set(true);
        type.getScheduler().entitySpecificScheduler(player)
                .run(onComplete::run, null);
    }

    // ========================= HELPERS ======================================

    /**
     * Spawns a single thin BlockDisplay face using the given {@link Transformation}.
     * The entity is positioned at the block origin (bottom-west-north corner) so that
     * the transformation matrix positions the face precisely within the block volume.
     */
    private BlockDisplay spawnFace(World world, BlockData blockData, Transformation transform) {
        // Spawn at block origin (lower-corner)
        Location origin = blockLocation.clone().add(0, 0, 0);
        return world.spawn(origin, BlockDisplay.class, e -> {
            e.setBlock(blockData);
            e.setTransformation(transform);
            e.setViewRange(64);
            e.setInterpolationDelay(-1);
        });
    }

    /**
     * Sets a new transformation on a BlockDisplay and lets the client interpolate
     * over the given number of ticks.
     */
    private static void applyInterpolation(BlockDisplay display, Transformation target, int durationTicks) {
        if (display == null || display.isDead()) return;
        display.setInterpolationDuration(durationTicks);
        display.setInterpolationDelay(0);
        display.setTransformation(target);
    }

    // ---- Face transformations at rest ----
    // All faces are 1 block wide/tall except in thickness dimension (FACE_THICKNESS).
    // The display entity origin is at the block's bottom-north-west corner.

    /** Bottom face – sits flush at Y=0 of the block. */
    private static Transformation bottomTransform() {
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(1f, FACE_THICKNESS, 1f),
                new AxisAngle4f(0, 0, 1, 0)
        );
    }

    /** Top face – sits at Y = 1 - FACE_THICKNESS of the block. */
    private static Transformation topTransform() {
        return new Transformation(
                new Vector3f(0f, 1f - FACE_THICKNESS, 0f),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(1f, FACE_THICKNESS, 1f),
                new AxisAngle4f(0, 0, 1, 0)
        );
    }

    /** North face – flush at Z=0, spans X and Y. */
    private static Transformation northTransform() {
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(1f, 1f, FACE_THICKNESS),
                new AxisAngle4f(0, 0, 1, 0)
        );
    }

    /** South face – at Z = 1 - FACE_THICKNESS. */
    private static Transformation southTransform() {
        return new Transformation(
                new Vector3f(0f, 0f, 1f - FACE_THICKNESS),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(1f, 1f, FACE_THICKNESS),
                new AxisAngle4f(0, 0, 1, 0)
        );
    }

    /** West face – flush at X=0. */
    private static Transformation westTransform() {
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(FACE_THICKNESS, 1f, 1f),
                new AxisAngle4f(0, 0, 1, 0)
        );
    }

    /** East face – at X = 1 - FACE_THICKNESS. */
    private static Transformation eastTransform() {
        return new Transformation(
                new Vector3f(1f - FACE_THICKNESS, 0f, 0f),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(FACE_THICKNESS, 1f, 1f),
                new AxisAngle4f(0, 0, 1, 0)
        );
    }

    /**
     * Computes XZ offsets for {@code count} items so they spread within [-0.3, 0.3].
     * Returns a flat array: [x0, z0, x1, z1, …]
     */
    private static double[] computeOffsets(int count) {
        double[] out = new double[count * 2];
        if (count == 1) return out; // centre
        double spread = 0.28;
        // Place items around a circle
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            out[i * 2]     = Math.cos(angle) * spread;
            out[i * 2 + 1] = Math.sin(angle) * spread;
        }
        return out;
    }

    /** Removes all spawned display entities and restores the crate block. */
    private void cleanupAll() {
        for (BlockDisplay d : faceDisplays) {
            if (d != null && !d.isDead()) d.remove();
        }
        faceDisplays.clear();

        for (ItemDisplay d : rewardDisplays) {
            if (d != null && !d.isDead()) d.remove();
        }
        rewardDisplays.clear();

        // Restore the block
        if (blockLocation != null && savedBlockData != null) {
            Block block = blockLocation.getBlock();
            block.setBlockData(savedBlockData, false);
            savedBlockData = null;
        }
    }

    private void cancelTasks() {
        if (dismantleTask != null) { dismantleTask.cancel(); dismantleTask = null; }
        if (topFallTask   != null) { topFallTask.cancel();   topFallTask   = null; }
        if (rewardTask    != null) { rewardTask.cancel();    rewardTask    = null; }
        if (cleanupTask   != null) { cleanupTask.cancel();   cleanupTask   = null; }
    }

    private void fallbackFinish() {
        if (selectedReward == null) selectedReward = RewardSelector.selectWeighted(crate);
        finished.set(true);
        type.getScheduler().entitySpecificScheduler(player)
                .runDelayed(onComplete::run, null, 1L);
    }
}




