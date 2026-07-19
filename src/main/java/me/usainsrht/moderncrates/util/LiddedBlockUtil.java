package me.usainsrht.moderncrates.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lidded;

/**
 * Helpers for opening and closing lidded container blocks.
 */
public final class LiddedBlockUtil {

    private LiddedBlockUtil() {}

    public static boolean isLidded(Location location) {
        if (location == null || location.getWorld() == null) return false;
        BlockState state = location.getBlock().getState();
        return state instanceof Lidded;
    }

    public static void open(Location location) {
        Lidded lidded = getLidded(location);
        if (lidded != null) {
            lidded.open();
        }
    }

    public static void close(Location location) {
        Lidded lidded = getLidded(location);
        if (lidded != null) {
            lidded.close();
        }
    }

    private static Lidded getLidded(Location location) {
        if (location == null || location.getWorld() == null) return null;
        Block block = location.getBlock();
        BlockState state = block.getState();
        if (state instanceof Lidded lidded) {
            return lidded;
        }
        return null;
    }
}
