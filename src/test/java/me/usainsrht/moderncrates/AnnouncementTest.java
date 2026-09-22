package me.usainsrht.moderncrates;

import me.usainsrht.moderncrates.api.crate.AnnounceConfig;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.util.PlaceholderUtil;
import me.usainsrht.yamlmessage.YamlMessage;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AnnouncementTest {

    @Test
    public void testAnnouncementDelivery() {
        List<Component> messages = new ArrayList<>();
        Audience testAudience = new Audience() {
            @Override
            public void sendMessage(Component message) {
                messages.add(message);
                System.out.println("Received message: " + message);
            }
        };

        Crate crate = new Crate("example");
        crate.setName("<gold>Example Crate");

        AnnounceConfig annConfig = new AnnounceConfig();
        annConfig.setToEveryone(true);
        annConfig.setSingle("<gold><player> <gray>won <gold><reward_name> <gray>from the <gold>Example Crate<gray>!");
        crate.setAnnounceConfig(annConfig);

        Reward reward = new Reward("diamond_sword");
        reward.setAnnouncementMessage("<gold><player> <gray>won a <aqua><bold>Diamond Sword <gray>from the <gold>Example Crate<gray>!");

        Component rewardDisplayName = Component.text("Diamond Sword");
        TagResolver[] resolvers = PlaceholderUtil.rewardResolvers(null, crate, reward, rewardDisplayName);

        YamlMessage message = annConfig.getSingleMessage();
        assertFalse(message.isEmpty());

        PlaceholderUtil.send(message, testAudience, "<gold>MC <gray>> ", resolvers);

        assertEquals(1, messages.size());
    }

    @Test
    public void testMuteValues() {
        assertTrue(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("true"));
        assertTrue(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("TRUE"));
        assertTrue(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("True"));
        assertTrue(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue(" true "));

        assertTrue(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("1"));
        assertTrue(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue(" 1 "));

        assertTrue(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("yes"));
        assertTrue(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("YES"));
        assertTrue(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("Yes"));
        assertTrue(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue(" yes "));

        assertFalse(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("false"));
        assertFalse(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("FALSE"));
        assertFalse(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("0"));
        assertFalse(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("no"));
        assertFalse(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("NO"));
        assertFalse(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue(null));
        assertFalse(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue(""));
        assertFalse(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("   "));
        assertFalse(me.usainsrht.moderncrates.hook.LuckPermsHook.isMuteValue("random"));
    }

    @Test
    public void testAnnouncementParsedForOpenerOnly() {
        List<Component> alexReceived = new ArrayList<>();
        Audience alexAudience = new Audience() {
            @Override
            public void sendMessage(Component message) {
                alexReceived.add(message);
            }
        };

        Crate crate = new Crate("example");
        crate.setName("<gold>Example Crate");

        Reward reward = new Reward("diamond_sword");
        Component rewardDisplayName = Component.text("Diamond Sword");

        // Resolver constructed for opener "Steve"
        TagResolver steveResolver = PlaceholderUtil.parsed("player", "Steve");
        TagResolver[] resolvers = new TagResolver[]{
                steveResolver,
                PlaceholderUtil.component("reward_name", rewardDisplayName),
                PlaceholderUtil.parsed("crate", "Example Crate")
        };

        YamlMessage message = YamlMessage.chat("<gold><player> <gray>opened <gold><crate> <gray>and won <gold><reward_name><gray>!");

        // Parse once with opener resolvers
        List<Component> parsedChat = new ArrayList<>();
        for (String line : message.chat()) {
            parsedChat.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(line, resolvers));
        }

        // Send the pre-parsed components to Alex
        PlaceholderUtil.sendParsed(alexAudience, parsedChat, null, null, List.of());

        assertEquals(1, alexReceived.size());
        String plain = me.usainsrht.moderncrates.util.TextUtil.plain(alexReceived.get(0));
        assertTrue(plain.contains("Steve"), "Announcement sent to Alex must contain opener's name Steve");
        assertFalse(plain.contains("Alex"), "Announcement sent to Alex must NOT contain Alex");
        assertTrue(plain.contains("Diamond Sword"));
    }

    @Test
    public void testRewardExemptFromAnnouncement() {
        Crate crate = new Crate("example");
        AnnounceConfig annConfig = new AnnounceConfig();
        annConfig.setDefaultAnnounce(true);
        crate.setAnnounceConfig(annConfig);

        Reward junkReward = new Reward("cobblestone");
        junkReward.setAnnounce(false);

        // announce: false overrides crate default true
        assertFalse(junkReward.shouldAnnounce(crate));
        assertEquals(Boolean.FALSE, junkReward.getAnnounce());
        assertEquals(Boolean.FALSE, junkReward.isAnnounce());

        // Even with an announcementMessage set, announce: false must exempt the reward
        junkReward.setAnnouncementMessage("<gray>Junk announcement");
        assertFalse(junkReward.shouldAnnounce(crate));
    }

    @Test
    public void testRewardUniqueAnnouncementMessage() {
        Crate crate = new Crate("example");
        AnnounceConfig annConfig = new AnnounceConfig();
        annConfig.setDefaultAnnounce(true);
        crate.setAnnounceConfig(annConfig);

        Reward rareReward = new Reward("ancient_sword");
        rareReward.setAnnouncementMessage("<light_purple>★ <player> found the ANCIENT SWORD! ★");

        assertTrue(rareReward.shouldAnnounce(crate));
        assertNotNull(rareReward.getAnnouncementMessage());
        assertFalse(rareReward.getAnnouncementMessage().isEmpty());
        assertTrue(rareReward.getAnnouncementMessage().chat().get(0).contains("ANCIENT SWORD"));
    }

    @Test
    public void testRewardForcedAnnounceWhenCrateDisabled() {
        Crate crate = new Crate("example");
        AnnounceConfig annConfig = new AnnounceConfig();
        annConfig.setDefaultAnnounce(false);
        crate.setAnnounceConfig(annConfig);

        Reward forcedReward = new Reward("jackpot");
        forcedReward.setAnnounce(true);

        // announce: true overrides crate default false
        assertTrue(forcedReward.shouldAnnounce(crate));
        assertEquals(Boolean.TRUE, forcedReward.getAnnounce());
        assertEquals(Boolean.TRUE, forcedReward.isAnnounce());
    }

    @Test
    public void testRewardFallbackToCrateDefault() {
        Crate crateDefaultTrue = new Crate("crate1");
        AnnounceConfig ann1 = new AnnounceConfig();
        ann1.setDefaultAnnounce(true);
        crateDefaultTrue.setAnnounceConfig(ann1);

        Crate crateDefaultFalse = new Crate("crate2");
        AnnounceConfig ann2 = new AnnounceConfig();
        ann2.setDefaultAnnounce(false);
        crateDefaultFalse.setAnnounceConfig(ann2);

        Reward normalReward = new Reward("iron_ingot");

        assertNull(normalReward.getAnnounce());
        assertNull(normalReward.getAnnouncementMessage());

        // Falls back to crate default
        assertTrue(normalReward.shouldAnnounce(crateDefaultTrue));
        assertFalse(normalReward.shouldAnnounce(crateDefaultFalse));
        assertFalse(normalReward.shouldAnnounce(null));
    }

    @Test
    public void testConfigParserRewardAnnounceAndMessage() throws java.io.IOException {
        java.io.File tempDir = java.nio.file.Files.createTempDirectory("moderncrates_test").toFile();
        try {
            java.io.File crateFile = new java.io.File(tempDir, "test_crate.yml");
            String yamlContent = """
                    name: "<gold>Test Crate"
                    animation: "instant"
                    announce:
                      to_everyone: true
                      default: false
                      single: "<gold><player> won <reward_name>!"
                    rewards:
                      junk:
                        chance: 50.0
                        announce: false
                      forced:
                        chance: 20.0
                        announce: true
                      rare:
                        chance: 5.0
                        announcement-message: "<light_purple><player> won Rare!"
                      legacy:
                        chance: 10.0
                        announce: "<aqua><player> won Legacy!"
                      default_item:
                        chance: 15.0
                    """;
            java.nio.file.Files.writeString(crateFile.toPath(), yamlContent);

            me.usainsrht.moderncrates.config.CrateConfigParser parser =
                    new me.usainsrht.moderncrates.config.CrateConfigParser(null);
            Crate parsedCrate = parser.parse("test_crate", crateFile);

            assertNotNull(parsedCrate);
            assertNotNull(parsedCrate.getAnnounceConfig());
            assertFalse(parsedCrate.getAnnounceConfig().isDefaultAnnounce(), "Crate default announce should be false");

            Reward junk = parsedCrate.getRewards().get("junk");
            assertNotNull(junk);
            assertEquals(Boolean.FALSE, junk.getAnnounce());
            assertFalse(junk.shouldAnnounce(parsedCrate));

            Reward forced = parsedCrate.getRewards().get("forced");
            assertNotNull(forced);
            assertEquals(Boolean.TRUE, forced.getAnnounce());
            assertTrue(forced.shouldAnnounce(parsedCrate));

            Reward rare = parsedCrate.getRewards().get("rare");
            assertNotNull(rare);
            assertNotNull(rare.getAnnouncementMessage());
            assertTrue(rare.shouldAnnounce(parsedCrate));

            Reward legacy = parsedCrate.getRewards().get("legacy");
            assertNotNull(legacy);
            assertNotNull(legacy.getAnnouncementMessage());
            assertTrue(legacy.shouldAnnounce(parsedCrate));

            Reward defaultItem = parsedCrate.getRewards().get("default_item");
            assertNotNull(defaultItem);
            assertNull(defaultItem.getAnnounce());
            assertNull(defaultItem.getAnnouncementMessage());
            assertFalse(defaultItem.shouldAnnounce(parsedCrate)); // crate default is false

            // Test saving and re-parsing
            java.io.File saveDir = java.nio.file.Files.createTempDirectory("moderncrates_save_test").toFile();
            try {
                parser.save(parsedCrate, saveDir);
                java.io.File savedFile = new java.io.File(saveDir, parsedCrate.getId() + ".yml");
                assertTrue(savedFile.exists());

                Crate reloadedCrate = parser.parse(parsedCrate.getId(), savedFile);
                assertNotNull(reloadedCrate);
                assertFalse(reloadedCrate.getAnnounceConfig().isDefaultAnnounce());

                Reward reloadedJunk = reloadedCrate.getRewards().get("junk");
                assertEquals(Boolean.FALSE, reloadedJunk.getAnnounce());

                Reward reloadedForced = reloadedCrate.getRewards().get("forced");
                assertEquals(Boolean.TRUE, reloadedForced.getAnnounce());

                Reward reloadedRare = reloadedCrate.getRewards().get("rare");
                assertNotNull(reloadedRare.getAnnouncementMessage());

                Reward reloadedLegacy = reloadedCrate.getRewards().get("legacy");
                assertNotNull(reloadedLegacy.getAnnouncementMessage());
            } finally {
                for (java.io.File f : saveDir.listFiles()) f.delete();
                saveDir.delete();
            }
        } finally {
            for (java.io.File f : tempDir.listFiles()) f.delete();
            tempDir.delete();
        }
    }

    @Test
    public void testMultiRewardFiltering() {
        Crate crate = new Crate("multi_test");
        AnnounceConfig ann = new AnnounceConfig();
        ann.setDefaultAnnounce(true);
        crate.setAnnounceConfig(ann);

        Reward junk1 = new Reward("junk1");
        junk1.setAnnounce(false);

        Reward junk2 = new Reward("junk2");
        junk2.setAnnounce(false);

        Reward rare = new Reward("rare");
        rare.setAnnouncementMessage("<gold><player> won RARE!");

        Reward normal = new Reward("normal"); // fallback to crate default (true)

        List<Reward> allRewards = List.of(junk1, junk2, rare, normal);
        List<Reward> announced = allRewards.stream()
                .filter(r -> r.shouldAnnounce(crate))
                .toList();

        assertEquals(2, announced.size());
        assertTrue(announced.contains(rare));
        assertTrue(announced.contains(normal));
        assertFalse(announced.contains(junk1));
        assertFalse(announced.contains(junk2));

        // All junk case
        List<Reward> allJunk = List.of(junk1, junk2);
        List<Reward> announcedJunk = allJunk.stream()
                .filter(r -> r.shouldAnnounce(crate))
                .toList();
        assertTrue(announcedJunk.isEmpty());
    }
}
