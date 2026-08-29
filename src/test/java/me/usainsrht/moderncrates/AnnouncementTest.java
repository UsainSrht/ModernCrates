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
        reward.setAnnounce("<gold><player> <gray>won a <aqua><bold>Diamond Sword <gray>from the <gold>Example Crate<gray>!");

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
}
