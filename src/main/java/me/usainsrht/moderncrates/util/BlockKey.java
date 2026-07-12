package me.usainsrht.moderncrates.util;

import org.bukkit.Location;

import java.util.Objects;

/**
 * Immutable block coordinate key for session locking.
 */
public final class BlockKey {

    private final String world;
    private final int x;
    private final int y;
    private final int z;

    private BlockKey(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static BlockKey from(Location location) {
        return new BlockKey(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockKey blockKey)) return false;
        return x == blockKey.x && y == blockKey.y && z == blockKey.z && world.equals(blockKey.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }
}
