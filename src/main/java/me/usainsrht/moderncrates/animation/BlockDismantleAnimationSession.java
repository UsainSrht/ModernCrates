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
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Animation session for the BlockDismantle animation.
 *
 * <p>Phases:
 * <ol>
 *   <li>SPAWN  – hide the real crate block (save BlockData, set to AIR), spawn 6 thin
 *       BlockDisplay entities that together form the visual shell of the block.</li>
 *   <li>DISMANTLE – side faces rotate outward on a bottom-edge hinge (like doors falling
 *       open); top face interpolates upward in a random horizontal direction then falls
 *       back; bottom face stays; configured particles burst at the block centre.</li>
 *   <li>REWARD  – one ItemDisplay for the selected (won) reward appears inside the
 *       open cavity. No random pool items are shown.</li>
 *   <li>CLEANUP – after {@code dismantle_display_duration_ticks}, all display entities are
 *       removed and the original block is restored.</li>
 * </ol>
 *
 * <h3>Side-face hinge maths</h3>
 * The entity is spawned at the block's lower-northwest corner. Each side face's bottom
 * outer edge is positioned so that a single Translation + LeftRotation Transformation
 * places it lying flat on the ground after a 90° rotation.
 *
 * <pre>
 * Face    Axis  Angle   Final T            Flat region (entity-relative)
 * ------  ----  ------  -----------------  --------------------------------
 * North   X     −90°    (0, 0, 0)          X=[0,1]  Y=[0,FT]  Z=[−1,0]
 * South   X     +90°    (0, FT, 1)         X=[0,1]  Y=[0,FT]  Z=[1,2]
 * West    Z     +90°    (0, 0, 0)          X=[−1,0] Y=[0,FT]  Z=[0,1]
 * East    Z     −90°    (1, FT, 0)         X=[1,2]  Y=[0,FT]  Z=[0,1]
 * </pre>
 */
public class BlockDismantleAnimationSession implements AnimationSession {

    private static final float FACE_THICKNESS = 0.05f;

    private final Player player;
    private final Crate crate;
    private final Animation animation;
    private final BlockDismantleAnimationType type;
    private final Runnable onComplete;

    private final AtomicBoolean finished  = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Random random = new Random();

    private Location  blockLocation;
    private BlockData savedBlockData;
    private Reward    selectedReward;

    /** 0=bottom, 1=top, 2=north, 3=south, 4=west, 5=east */
    private final List<BlockDisplay> faceDisplays   = new ArrayList<>();
    private final List<ItemDisplay>  rewardDisplays = new ArrayList<>();

    private ScheduledTask dismantleTask;
    private ScheduledTask topFallTask;
    private ScheduledTask rewardTask;
    private ScheduledTask cleanupTask;

    public BlockDismantleAnimationSession(Player player, Crate crate, Animation animation,
                                           BlockDismantleAnimationType type, Runnable onComplete) {
        this.player    = player;
        this.crate     = crate;
        this.animation = animation;
        this.type      = type;
        this.onComplete = onComplete;
    }

    // ============================= LIFECYCLE ================================

    @Override
    public void start() {
        CrateLocation crateLoc = crate.getCrateLocation();
        if (crateLoc == null) { fallbackFinish(); return; }
        Location loc = crateLoc.toBukkit();
        if (loc == null)      { fallbackFinish(); return; }
        blockLocation = loc.getBlock().getLocation();

        selectedReward = RewardSelector.selectWeighted(crate);
        if (selectedReward == null) { fallbackFinish(); return; }

        type.getScheduler().regionSpecificScheduler(blockLocation).run(this::spawnShell);
    }

    @Override
    public void cancel() {
        cancelled.set(true);
        cancelTasks();
        if (blockLocation != null) {
            type.getScheduler().regionSpecificScheduler(blockLocation).run(this::cleanupAll);
        } else {
            cleanupAll();
        }
        if (selectedReward == null) selectedReward = RewardSelector.selectWeighted(crate);
        finished.set(true);
    }

    @Override public boolean isFinished()        { return finished.get(); }
    @Override public Reward  getSelectedReward() { return selectedReward; }

    // ========================= PHASE 1 – SPAWN SHELL ========================

    private void spawnShell() {
        if (cancelled.get()) return;
        World world = blockLocation.getWorld();
        if (world == null) { fallbackFinish(); return; }

        Block block = blockLocation.getBlock();
        savedBlockData = block.getBlockData().clone();

        Material faceMaterial = Material.matchMaterial(animation.getDismantleBlockType().toUpperCase());
        if (faceMaterial == null || !faceMaterial.isBlock()) faceMaterial = Material.BARREL;

        // Force facing=UP for directional blocks (e.g. BARREL) so the top texture faces +Y,
        // the bottom texture faces -Y, and side textures face outward on all 4 side displays.
        // Blocks that don't support UP facing are left at their default state.
        BlockData faceBlockData = faceMaterial.createBlockData();
        if (faceBlockData instanceof Directional directional
                && directional.getFaces().contains(BlockFace.UP)) {
            directional.setFacing(BlockFace.UP);
        }

        block.setType(Material.AIR, false);

        faceDisplays.add(spawnFace(world, faceBlockData, bottomTransform()));
        faceDisplays.add(spawnFace(world, faceBlockData, topTransform()));
        faceDisplays.add(spawnFace(world, faceBlockData, northTransform()));
        faceDisplays.add(spawnFace(world, faceBlockData, southTransform()));
        faceDisplays.add(spawnFace(world, faceBlockData, westTransform()));
        faceDisplays.add(spawnFace(world, faceBlockData, eastTransform()));

        SoundUtil.play(player, animation.getDismantleOpenSounds());

        dismantleTask = type.getScheduler().regionSpecificScheduler(blockLocation)
                .runDelayed(this::dismantle, 5);
    }

    // ========================= PHASE 2 – DISMANTLE ==========================

    /**
     * Fires all face animations simultaneously.
     * The Minecraft client interpolates each transformation smoothly at its own
     * render frame-rate — no server-side per-tick tasks are needed.
     */
    private void dismantle() {
        if (cancelled.get()) return;

        int fallTicks = Math.max(1, animation.getDismantleFallDurationTicks());
        int riseTicks = Math.max(1, animation.getDismantleTopRiseDurationTicks());
        float height  = (float) animation.getDismantleTopLaunchHeight();
        float hRange  = (float) animation.getDismantleTopHorizontalRange();

        // ---- Top face (index 1): rise then fall ----
        if (faceDisplays.size() > 1) {
            BlockDisplay topFace = faceDisplays.get(1);
            float offX = (random.nextFloat() * 2 - 1) * hRange;
            float offZ = (random.nextFloat() * 2 - 1) * hRange;

            // Rise to peak
            applyInterpolation(topFace, new Transformation(
                    new Vector3f(offX, 1f - FACE_THICKNESS + height, offZ),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1f, FACE_THICKNESS, 1f),
                    new AxisAngle4f(0, 0, 1, 0)
            ), riseTicks);

            // After rise: fall off-screen
            topFallTask = type.getScheduler().regionSpecificScheduler(blockLocation)
                    .runDelayed(() -> {
                        if (cancelled.get() || topFace.isDead()) return;
                        applyInterpolation(topFace, new Transformation(
                                new Vector3f(offX, -2f, offZ),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(1f, FACE_THICKNESS, 1f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ), fallTicks);
                    }, riseTicks);
        }

        // ---- Side faces: hinge-drop using the exact fallen transformations ----
        // Each call issues one setTransformation; the client interpolates client-side.
        //
        // North (2): Rx(−90°), pivot at entity origin
        hingeDropSide(2, fallTicks,
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f((float) -Math.PI / 2, 1f, 0f, 0f),
                new Vector3f(1f, 1f, FACE_THICKNESS));

        // South (3): Rx(+90°), pivot at Z=1,Y=0 → T=(0, FT, 1)
        hingeDropSide(3, fallTicks,
                new Vector3f(0f, FACE_THICKNESS, 1f),
                new AxisAngle4f((float) Math.PI / 2, 1f, 0f, 0f),
                new Vector3f(1f, 1f, FACE_THICKNESS));

        // West (4): Rz(+90°), pivot at entity origin
        hingeDropSide(4, fallTicks,
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f((float) Math.PI / 2, 0f, 0f, 1f),
                new Vector3f(FACE_THICKNESS, 1f, 1f));

        // East (5): Rz(−90°), pivot at X=1,Y=0 → T=(1, FT, 0)
        hingeDropSide(5, fallTicks,
                new Vector3f(1f, FACE_THICKNESS, 0f),
                new AxisAngle4f((float) -Math.PI / 2, 0f, 0f, 1f),
                new Vector3f(FACE_THICKNESS, 1f, 1f));

        // ---- Particles ----
        spawnDismantleParticles(blockLocation.getWorld());

        // ---- Schedule reward reveal ----
        int rewardDelay = fallTicks + (int) (fallTicks * 0.2) + 1;
        rewardTask = type.getScheduler().regionSpecificScheduler(blockLocation)
                .runDelayed(this::showRewards, rewardDelay);
    }

    /**
     * Sends a single interpolated transformation to a side face.
     * The Minecraft client handles all intermediate frames at render frame-rate.
     */
    private void hingeDropSide(int faceIndex, int ticks,
                                Vector3f translation, AxisAngle4f rotation, Vector3f scale) {
        if (faceDisplays.size() <= faceIndex) return;
        BlockDisplay face = faceDisplays.get(faceIndex);
        if (face == null || face.isDead()) return;
        applyInterpolation(face, new Transformation(
                translation, rotation, scale, new AxisAngle4f(0, 0, 1, 0)
        ), ticks);
    }

    // ========================= PHASE 3 – SHOW REWARDS =======================

    /**
     * Spawns exactly one ItemDisplay for the selected (won) reward.
     * No random pool items are shown so the player only sees what they actually won.
     */
    private void showRewards() {
        if (cancelled.get()) return;
        World world = blockLocation.getWorld();
        if (world == null) { beginCleanup(); return; }

        SoundUtil.play(player, animation.getDismantleRewardSounds());

        ItemStack displayItem = selectedReward.getDisplay() != null
                ? ItemBuilder.fromDisplay(selectedReward.getDisplay())
                : new ItemStack(Material.STONE);

        double cx = blockLocation.getBlockX() + 0.5;
        double cy = blockLocation.getBlockY() + 0.35;
        double cz = blockLocation.getBlockZ() + 0.5;

        rewardDisplays.add(world.spawn(new Location(world, cx, cy, cz), ItemDisplay.class, e -> {
            e.setItemStack(displayItem);
            e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            e.setBillboard(Display.Billboard.VERTICAL);
            e.setViewRange(48);
        }));

        SoundUtil.play(player, animation.getDismantleSettleSounds());
        beginCleanup();
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
        type.getScheduler().entitySpecificScheduler(player).run(onComplete::run, null);
    }

    // ========================= HELPERS ======================================

    /**
     * Spawns a burst of every particle listed in {@code dismantle_particles}
     * as a ring + centre cluster around the block at dismantle time.
     */
    private void spawnDismantleParticles(World world) {
        if (world == null) return;
        List<String> particleNames = animation.getDismantleParticles();
        if (particleNames == null || particleNames.isEmpty()) return;

        double cx = blockLocation.getBlockX() + 0.5;
        double cy = blockLocation.getBlockY() + 0.5;
        double cz = blockLocation.getBlockZ() + 0.5;

        for (String name : particleNames) {
            Particle particle;
            try {
                particle = Particle.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            // Ring around the block
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                world.spawnParticle(particle,
                        cx + 0.7 * Math.cos(angle), cy, cz + 0.7 * Math.sin(angle),
                        1, 0, 0, 0, 0);
            }
            // Central burst
            world.spawnParticle(particle, cx, cy, cz, 6, 0.3, 0.3, 0.3, 0);
        }
    }
    private BlockDisplay spawnFace(World world, BlockData blockData, Transformation transform) {
        return world.spawn(blockLocation.clone(), BlockDisplay.class, e -> {
            e.setBlock(blockData);
            e.setTransformation(transform);
            e.setViewRange(64);
            e.setInterpolationDelay(-1);
        });
    }

    private static void applyInterpolation(BlockDisplay display, Transformation target, int durationTicks) {
        if (display == null || display.isDead()) return;
        display.setInterpolationDuration(durationTicks);
        display.setInterpolationDelay(0);
        display.setTransformation(target);
    }

    // ---- Resting face transformations ----
    private static Transformation bottomTransform() {
        return new Transformation(new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0, 0, 1, 0), new Vector3f(1f, FACE_THICKNESS, 1f),
                new AxisAngle4f(0, 0, 1, 0));
    }

    private static Transformation topTransform() {
        return new Transformation(new Vector3f(0f, 1f - FACE_THICKNESS, 0f),
                new AxisAngle4f(0, 0, 1, 0), new Vector3f(1f, FACE_THICKNESS, 1f),
                new AxisAngle4f(0, 0, 1, 0));
    }

    private static Transformation northTransform() {
        return new Transformation(new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0, 0, 1, 0), new Vector3f(1f, 1f, FACE_THICKNESS),
                new AxisAngle4f(0, 0, 1, 0));
    }

    private static Transformation southTransform() {
        return new Transformation(new Vector3f(0f, 0f, 1f - FACE_THICKNESS),
                new AxisAngle4f(0, 0, 1, 0), new Vector3f(1f, 1f, FACE_THICKNESS),
                new AxisAngle4f(0, 0, 1, 0));
    }

    private static Transformation westTransform() {
        return new Transformation(new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0, 0, 1, 0), new Vector3f(FACE_THICKNESS, 1f, 1f),
                new AxisAngle4f(0, 0, 1, 0));
    }

    private static Transformation eastTransform() {
        return new Transformation(new Vector3f(1f - FACE_THICKNESS, 0f, 0f),
                new AxisAngle4f(0, 0, 1, 0), new Vector3f(FACE_THICKNESS, 1f, 1f),
                new AxisAngle4f(0, 0, 1, 0));
    }

    private void cleanupAll() {
        for (BlockDisplay d : faceDisplays)  { if (d != null && !d.isDead()) d.remove(); }
        faceDisplays.clear();
        for (ItemDisplay d : rewardDisplays) { if (d != null && !d.isDead()) d.remove(); }
        rewardDisplays.clear();
        if (blockLocation != null && savedBlockData != null) {
            blockLocation.getBlock().setBlockData(savedBlockData, false);
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




