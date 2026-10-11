package me.usainsrht.moderncrates;

import me.usainsrht.moderncrates.manager.VirtualKeyManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class VirtualKeyManagerTest {

    @TempDir
    Path tempDir;

    private VirtualKeyManager manager;
    private File dataFolder;

    @BeforeEach
    public void setUp() {
        dataFolder = tempDir.toFile();
        manager = new VirtualKeyManager(dataFolder, Logger.getLogger("VirtualKeyManagerTest"));
        manager.load();
    }

    @AfterEach
    public void tearDown() {
        manager.shutdown();
    }

    @Test
    public void testCleanupZeroValuesAndEmptyUuidsOnSave() throws IOException {
        UUID playerWithKeys = UUID.randomUUID();
        UUID playerWithZeroKeys = UUID.randomUUID();
        UUID playerMixed = UUID.randomUUID();

        // 1. Manually write file with 0s and empty UUIDs to simulate existing dirty database
        File file = new File(dataFolder, "virtual_keys.yml");
        YamlConfiguration initialYaml = new YamlConfiguration();
        initialYaml.set(playerWithKeys + ".common", 5);
        initialYaml.set(playerWithZeroKeys + ".common", 0);
        initialYaml.set(playerMixed + ".crateA", 3);
        initialYaml.set(playerMixed + ".crateB", 0);
        initialYaml.save(file);

        // Reload manager from disk
        manager.load();

        // Verify in-memory state ignores zeros
        assertEquals(5, manager.getKeys(playerWithKeys, "common"));
        assertEquals(0, manager.getKeys(playerWithZeroKeys, "common"));
        assertEquals(3, manager.getKeys(playerMixed, "crateA"));
        assertEquals(0, manager.getKeys(playerMixed, "crateB"));

        // Save
        manager.saveSync();

        // Read file back and verify cleanup
        YamlConfiguration cleanYaml = YamlConfiguration.loadConfiguration(file);
        assertEquals(5, cleanYaml.getInt(playerWithKeys + ".common"));
        assertEquals(3, cleanYaml.getInt(playerMixed + ".crateA"));

        // Check that zeros and empty UUID sections are completely removed from file
        assertFalse(cleanYaml.contains(playerWithZeroKeys.toString()), "UUID with only 0 keys should not exist in YAML");
        assertFalse(cleanYaml.contains(playerMixed + ".crateB"), "Crate with 0 keys should not exist in YAML");
    }

    @Test
    public void testPreserveUnloadedCratesWithPositiveKeys() throws IOException {
        UUID player = UUID.randomUUID();
        String loadedCrate = "roulette";
        String unloadedCrate = "unloaded_holiday_crate_2025";

        manager.setKeys(player, loadedCrate, 2);
        manager.setKeys(player, unloadedCrate, 10);
        manager.saveSync();

        File file = new File(dataFolder, "virtual_keys.yml");
        YamlConfiguration savedYaml = YamlConfiguration.loadConfiguration(file);

        // Unloaded crate ID must NOT be deleted or lost
        assertEquals(2, savedYaml.getInt(player + "." + loadedCrate));
        assertEquals(10, savedYaml.getInt(player + "." + unloadedCrate));

        // Reload and verify
        VirtualKeyManager freshManager = new VirtualKeyManager(dataFolder, Logger.getLogger("Test"));
        freshManager.load();
        assertEquals(10, freshManager.getKeys(player, unloadedCrate));
        freshManager.shutdown();
    }

    @Test
    public void testRemoveKeysClampingAndRemoval() {
        UUID player = UUID.randomUUID();
        manager.setKeys(player, "mystery", 3);
        assertEquals(3, manager.getKeys(player, "mystery"));

        // Remove 1
        assertTrue(manager.removeKeys(player, "mystery", 1));
        assertEquals(2, manager.getKeys(player, "mystery"));

        // Remove 5 (more than 2) -> clamped to 0
        assertTrue(manager.removeKeys(player, "mystery", 5));
        assertEquals(0, manager.getKeys(player, "mystery"));

        // Remove again when already 0 -> returns false
        assertFalse(manager.removeKeys(player, "mystery", 1));

        manager.saveSync();
        File file = new File(dataFolder, "virtual_keys.yml");
        YamlConfiguration savedYaml = YamlConfiguration.loadConfiguration(file);
        assertFalse(savedYaml.contains(player.toString()), "UUID should be removed when all keys reach 0");
    }

    @Test
    public void testGetPlayerKeys() {
        UUID player = UUID.randomUUID();
        manager.setKeys(player, "crate1", 5);
        manager.setKeys(player, "crate2", 0);
        manager.setKeys(player, "crate3", 2);

        Map<String, Integer> playerKeys = manager.getPlayerKeys(player);
        assertEquals(2, playerKeys.size());
        assertEquals(5, playerKeys.get("crate1"));
        assertEquals(2, playerKeys.get("crate3"));
        assertNull(playerKeys.get("crate2"));
    }

    @Test
    public void testAsyncSaveCompletes() throws Exception {
        UUID player = UUID.randomUUID();
        manager.setKeys(player, "async_crate", 7);
        var future = manager.saveAsync();
        future.get(); // Await async completion

        File file = new File(dataFolder, "virtual_keys.yml");
        YamlConfiguration savedYaml = YamlConfiguration.loadConfiguration(file);
        assertEquals(7, savedYaml.getInt(player + ".async_crate"));
    }
}
