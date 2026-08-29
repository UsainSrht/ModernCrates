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
}
