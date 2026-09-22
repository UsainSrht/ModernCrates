package me.usainsrht.moderncrates;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.reward.requirement.RewardRequirements;
import me.usainsrht.moderncrates.config.CrateConfigParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RewardRequirementsConfigTest {

    @Test
    public void testParseListRequirements(@TempDir Path tempDir) throws Exception {
        File crateFile = new File(tempDir.toFile(), "list_req.yml");
        String yamlContent = """
                name: "<gold>List Req Crate"
                animation: "instant"
                rewards:
                  claim_boost:
                    chance: 20.0
                    requirements:
                      - "%claim_limit% < 50"
                      - "%player_level% >= 10"
                  normal_reward:
                    chance: 80.0
                """;
        Files.writeString(crateFile.toPath(), yamlContent);

        CrateConfigParser parser = new CrateConfigParser(null);
        Crate crate = parser.parse("list_req", crateFile);

        assertNotNull(crate);
        Reward claimReward = crate.getRewards().get("claim_boost");
        assertNotNull(claimReward);
        assertTrue(claimReward.hasRequirements());

        RewardRequirements reqs = claimReward.getRequirements();
        assertEquals(RewardRequirements.LogicalMode.AND, reqs.getMode());
        assertEquals(2, reqs.getConditions().size());
        assertEquals(List.of("%claim_limit% < 50", "%player_level% >= 10"), reqs.getRawConditions());

        Reward normalReward = crate.getRewards().get("normal_reward");
        assertNotNull(normalReward);
        assertFalse(normalReward.hasRequirements());
    }

    @Test
    public void testParseSingleStringRequirements(@TempDir Path tempDir) throws Exception {
        File crateFile = new File(tempDir.toFile(), "single_req.yml");
        String yamlContent = """
                name: "<gold>Single Req Crate"
                animation: "instant"
                rewards:
                  claim_boost:
                    chance: 15.0
                    requirements: "%claim_limit% < 50"
                """;
        Files.writeString(crateFile.toPath(), yamlContent);

        CrateConfigParser parser = new CrateConfigParser(null);
        Crate crate = parser.parse("single_req", crateFile);

        assertNotNull(crate);
        Reward claimReward = crate.getRewards().get("claim_boost");
        assertNotNull(claimReward);
        assertTrue(claimReward.hasRequirements());
        assertEquals(1, claimReward.getRequirements().getConditions().size());
        assertEquals("%claim_limit% < 50", claimReward.getRequirements().getRawConditions().get(0));
    }

    @Test
    public void testParseStructuredSectionRequirements(@TempDir Path tempDir) throws Exception {
        File crateFile = new File(tempDir.toFile(), "section_req.yml");
        String yamlContent = """
                name: "<gold>Section Req Crate"
                animation: "instant"
                rewards:
                  claim_boost:
                    chance: 25.0
                    requirements:
                      mode: OR
                      permission: "moderncrates.vip"
                      conditions:
                        - "%claim_limit% < 50"
                        - "%player_level% >= 30"
                """;
        Files.writeString(crateFile.toPath(), yamlContent);

        CrateConfigParser parser = new CrateConfigParser(null);
        Crate crate = parser.parse("section_req", crateFile);

        assertNotNull(crate);
        Reward claimReward = crate.getRewards().get("claim_boost");
        assertNotNull(claimReward);
        assertTrue(claimReward.hasRequirements());

        RewardRequirements reqs = claimReward.getRequirements();
        assertEquals(RewardRequirements.LogicalMode.OR, reqs.getMode());
        assertEquals("moderncrates.vip", reqs.getPermission());
        assertEquals(2, reqs.getConditions().size());
    }

    @Test
    public void testSaveAndReloadRoundTrip(@TempDir Path tempDir) throws Exception {
        File cratesDir = tempDir.toFile();
        File crateFile = new File(cratesDir, "roundtrip.yml");
        String yamlContent = """
                name: "<gold>Roundtrip Crate"
                animation: "instant"
                rewards:
                  claim_boost:
                    chance: 20.0
                    requirements:
                      mode: OR
                      permission: "special.perm"
                      conditions:
                        - "%claim_limit% < 50"
                  simple_req:
                    chance: 30.0
                    requirements:
                      - "%player_level% >= 5"
                """;
        Files.writeString(crateFile.toPath(), yamlContent);

        CrateConfigParser parser = new CrateConfigParser(null);
        Crate crate = parser.parse("roundtrip", crateFile);
        assertNotNull(crate);

        // Save back
        parser.save(crate, cratesDir);

        // Reload
        Crate reloaded = parser.parse("roundtrip", crateFile);
        assertNotNull(reloaded);

        Reward claimReward = reloaded.getRewards().get("claim_boost");
        assertNotNull(claimReward);
        assertTrue(claimReward.hasRequirements());
        assertEquals(RewardRequirements.LogicalMode.OR, claimReward.getRequirements().getMode());
        assertEquals("special.perm", claimReward.getRequirements().getPermission());
        assertEquals(List.of("%claim_limit% < 50"), claimReward.getRequirements().getRawConditions());

        Reward simpleReward = reloaded.getRewards().get("simple_req");
        assertNotNull(simpleReward);
        assertTrue(simpleReward.hasRequirements());
        assertEquals(RewardRequirements.LogicalMode.AND, simpleReward.getRequirements().getMode());
        assertEquals(List.of("%player_level% >= 5"), simpleReward.getRequirements().getRawConditions());
    }

    @Test
    public void testRewardCanWinEvaluation() {
        Reward reward = new Reward("test_reward");
        reward.setChance(10.0);

        // No requirements, null player
        assertTrue(reward.canWin(null));

        // Required permission
        reward.setRequiredPermission("crates.admin");
        assertTrue(reward.hasRequiredPermission());
        assertFalse(reward.canWin(null)); // null player cannot have required permission

        // Passing reward requirement
        Reward rewardWithReq = new Reward("req_reward");
        RewardRequirements reqs = new RewardRequirements();
        reqs.addCondition("10 < 20");
        rewardWithReq.setRequirements(reqs);
        assertTrue(rewardWithReq.canWin(null));

        // Failing reward requirement
        Reward failingReward = new Reward("failing_reward");
        RewardRequirements failingReqs = new RewardRequirements();
        failingReqs.addCondition("20 < 10");
        failingReward.setRequirements(failingReqs);
        assertFalse(failingReward.canWin(null));
    }
}
