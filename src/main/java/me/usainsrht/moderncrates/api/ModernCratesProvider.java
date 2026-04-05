package me.usainsrht.moderncrates.api;

/**
 * Static provider for the ModernCratesAPI singleton instance.
 */
public final class ModernCratesProvider {

    private static ModernCratesAPI instance;

    private ModernCratesProvider() {}

    public static ModernCratesAPI get() {
        if (instance == null) {
            throw new IllegalStateException("ModernCrates API has not been initialized yet.");
        }
        return instance;
    }

    public static void set(ModernCratesAPI api) {
        if (instance != null) {
            throw new IllegalStateException("ModernCrates API has already been initialized.");
        }
        instance = api;
    }

    public static void unset() {
        instance = null;
    }
}
