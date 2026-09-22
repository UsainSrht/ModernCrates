package me.usainsrht.moderncrates;

import me.usainsrht.moderncrates.api.crate.AutoSortMode;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.config.CrateConfigParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AutoSortChanceTest {

    @Test
    public void testAutoSortOmittedDefaultsToDisabled(@TempDir Path tempDir) throws Exception {
        File crateFile = new File(tempDir.toFile(), "omitted.yml");
        String yamlContent = """
                name: "<gold>Omitted Crate"
                animation: "instant"
                rewards:
                  second:
                    chance: 50.0
                  first:
                    chance: 5.0
                  third:
                    chance: 20.0
                """;
        Files.writeString(crateFile.toPath(), yamlContent);

        CrateConfigParser parser = new CrateConfigParser(null);
        Crate crate = parser.parse("omitted", crateFile);

        assertNotNull(crate);
        assertEquals(AutoSortMode.DISABLED, crate.getAutoSortOnChance());
        assertFalse(crate.isAutoSortOnChance());

        List<String> rewardKeys = new ArrayList<>(crate.getRewards().keySet());
        assertEquals(List.of("second", "first", "third"), rewardKeys,
                "Rewards should retain original insertion order when auto sort is omitted");
    }

    @Test
    public void testAutoSortAscending(@TempDir Path tempDir) throws Exception {
        File crateFile = new File(tempDir.toFile(), "ascending.yml");
        String yamlContent = """
                name: "<gold>Ascending Crate"
                animation: "instant"
                auto_sort_on_chance: "ascending"
                rewards:
                  high:
                    chance: 50.0
                  low:
                    chance: 5.0
                  medium:
                    chance: 20.0
                  low_b:
                    chance: 5.0
                """;
        Files.writeString(crateFile.toPath(), yamlContent);

        CrateConfigParser parser = new CrateConfigParser(null);
        Crate crate = parser.parse("ascending", crateFile);

        assertNotNull(crate);
        assertEquals(AutoSortMode.ASCENDING, crate.getAutoSortOnChance());
        assertTrue(crate.isAutoSortOnChance());

        List<String> rewardKeys = new ArrayList<>(crate.getRewards().keySet());
        assertEquals(List.of("low", "low_b", "medium", "high"), rewardKeys,
                "Rewards should be sorted ascending by chance, with tie-breaker by key");
    }

    @Test
    public void testAutoSortDescending(@TempDir Path tempDir) throws Exception {
        File crateFile = new File(tempDir.toFile(), "descending.yml");
        String yamlContent = """
                name: "<gold>Descending Crate"
                animation: "instant"
                auto_sort_on_chance: "descending"
                rewards:
                  low:
                    chance: 5.0
                  high:
                    chance: 50.0
                  medium:
                    chance: 20.0
                  high_b:
                    chance: 50.0
                """;
        Files.writeString(crateFile.toPath(), yamlContent);

        CrateConfigParser parser = new CrateConfigParser(null);
        Crate crate = parser.parse("descending", crateFile);

        assertNotNull(crate);
        assertEquals(AutoSortMode.DESCENDING, crate.getAutoSortOnChance());
        assertTrue(crate.isAutoSortOnChance());

        List<String> rewardKeys = new ArrayList<>(crate.getRewards().keySet());
        assertEquals(List.of("high", "high_b", "medium", "low"), rewardKeys,
                "Rewards should be sorted descending by chance, with tie-breaker by key");
    }

    @Test
    public void testAutoSortDisabledExplicit(@TempDir Path tempDir) throws Exception {
        File crateFile = new File(tempDir.toFile(), "disabled.yml");
        String yamlContent = """
                name: "<gold>Disabled Crate"
                animation: "instant"
                auto_sort_on_chance: "disabled"
                rewards:
                  z_item:
                    chance: 100.0
                  a_item:
                    chance: 1.0
                """;
        Files.writeString(crateFile.toPath(), yamlContent);

        CrateConfigParser parser = new CrateConfigParser(null);
        Crate crate = parser.parse("disabled", crateFile);

        assertNotNull(crate);
        assertEquals(AutoSortMode.DISABLED, crate.getAutoSortOnChance());
        assertFalse(crate.isAutoSortOnChance());

        List<String> rewardKeys = new ArrayList<>(crate.getRewards().keySet());
        assertEquals(List.of("z_item", "a_item"), rewardKeys);
    }

    @Test
    public void testSaveAndReload(@TempDir Path tempDir) throws Exception {
        File crateFile = new File(tempDir.toFile(), "test_save.yml");
        String yamlContent = """
                name: "<gold>Save Test"
                animation: "instant"
                auto_sort_on_chance: "ascending"
                rewards:
                  c:
                    chance: 30.0
                  a:
                    chance: 10.0
                  b:
                    chance: 20.0
                """;
        Files.writeString(crateFile.toPath(), yamlContent);

        CrateConfigParser parser = new CrateConfigParser(null);
        Crate crate = parser.parse("test_save", crateFile);

        parser.save(crate, tempDir.toFile());

        File savedFile = new File(tempDir.toFile(), "test_save.yml");
        Crate reloaded = parser.parse("test_save", savedFile);
        assertNotNull(reloaded);
        assertEquals(AutoSortMode.ASCENDING, reloaded.getAutoSortOnChance());

        List<String> keys = new ArrayList<>(reloaded.getRewards().keySet());
        assertEquals(List.of("a", "b", "c"), keys);
    }

    @Test
    public void testDynamicSorting() {
        Crate crate = new Crate("dynamic");
        Map<String, Reward> rewards = new LinkedHashMap<>();

        Reward r1 = new Reward("r1");
        r1.setChance(30.0);
        rewards.put("r1", r1);

        Reward r2 = new Reward("r2");
        r2.setChance(10.0);
        rewards.put("r2", r2);

        Reward r3 = new Reward("r3");
        r3.setChance(20.0);
        rewards.put("r3", r3);

        crate.setRewards(rewards);
        assertEquals(List.of("r1", "r2", "r3"), new ArrayList<>(crate.getRewards().keySet()));

        // Enable ascending
        crate.setAutoSortOnChance(AutoSortMode.ASCENDING);
        assertEquals(List.of("r2", "r3", "r1"), new ArrayList<>(crate.getRewards().keySet()));

        // Add a new reward
        Reward r0 = new Reward("r0");
        r0.setChance(5.0);
        crate.getRewards().put("r0", r0);
        crate.sortRewards();
        assertEquals(List.of("r0", "r2", "r3", "r1"), new ArrayList<>(crate.getRewards().keySet()));

        // Switch to descending
        crate.setAutoSortOnChance(AutoSortMode.DESCENDING);
        assertEquals(List.of("r1", "r3", "r2", "r0"), new ArrayList<>(crate.getRewards().keySet()));
    }
}
