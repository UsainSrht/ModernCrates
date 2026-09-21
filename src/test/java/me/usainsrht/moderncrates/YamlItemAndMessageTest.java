package me.usainsrht.moderncrates;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateKeyConfig;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.util.PlaceholderUtil;
import me.usainsrht.yamlmessage.YamlMessage;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class YamlItemAndMessageTest {

    @Test
    public void testYamlMessageParsingAndDelivery() {
        Map<String, Object> msgMap = Map.of(
                "chat", List.of("<green>Test chat message"),
                "sound", "entity.player.levelup"
        );
        YamlMessage yamlMessage = YamlMessage.parse(msgMap);
        assertNotNull(yamlMessage);
        assertFalse(yamlMessage.isEmpty());
        assertFalse(yamlMessage.chat().isEmpty());
        assertFalse(yamlMessage.sounds().isEmpty());

        List<Component> receivedChat = new ArrayList<>();
        List<net.kyori.adventure.sound.Sound> receivedSounds = new ArrayList<>();

        Audience audience = new Audience() {
            @Override
            public void sendMessage(Component message) {
                receivedChat.add(message);
            }

            @Override
            public void playSound(net.kyori.adventure.sound.Sound sound) {
                receivedSounds.add(sound);
            }
        };

        yamlMessage.send(audience);

        assertEquals(1, receivedChat.size());
        assertEquals(1, receivedSounds.size());
    }

    @Test
    public void testYamlMessageMultiSoundsAndPlaceholders() {
        Map<String, Object> msgMap = Map.of(
                "chat", List.of("<gold>Winner: <player> won <reward_name>"),
                "sounds", List.of("entity.player.levelup", "block.chest.open")
        );
        YamlMessage yamlMessage = YamlMessage.parse(msgMap);
        assertNotNull(yamlMessage);
        assertEquals(2, yamlMessage.sounds().size());

        List<Component> receivedChat = new ArrayList<>();
        List<net.kyori.adventure.sound.Sound> receivedSounds = new ArrayList<>();

        Audience audience = new Audience() {
            @Override
            public void sendMessage(Component message) {
                receivedChat.add(message);
            }

            @Override
            public void playSound(net.kyori.adventure.sound.Sound sound) {
                receivedSounds.add(sound);
            }
        };

        Crate crate = new Crate("test_crate");
        crate.setName("Test Crate");
        Reward reward = new Reward("test_reward");

        TagResolver[] resolvers = PlaceholderUtil.rewardResolvers(null, crate, reward, Component.text("Excalibur"));
        PlaceholderUtil.send(yamlMessage, audience, "<gray>[<gold>MC<gray>] ", resolvers);

        assertEquals(1, receivedChat.size());
        assertEquals(2, receivedSounds.size());
    }

    @Test
    public void testCrateKeyConfigNoFallback() {
        CrateKeyConfig config = new CrateKeyConfig();
        // Without itemStack being set, getItemStack() returns null (no fallback legacy builder)
        assertNull(config.getItemStack());
    }
}
