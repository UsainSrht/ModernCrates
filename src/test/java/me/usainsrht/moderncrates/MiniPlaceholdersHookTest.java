package me.usainsrht.moderncrates;

import me.usainsrht.itemapi.itemtext.ItemTextOptions;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateItemConfig;
import me.usainsrht.moderncrates.api.crate.CrateKeyConfig;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.reward.RewardDisplay;
import me.usainsrht.moderncrates.hook.MiniPlaceholdersHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class MiniPlaceholdersHookTest {

    private ModernCratesPlugin plugin;
    private MiniPlaceholdersHook hook;
    private final Map<String, Crate> crates = new HashMap<>();

    @BeforeEach
    public void setUp() {
        plugin = Mockito.mock(ModernCratesPlugin.class);
        crates.clear();
        when(plugin.getCrateRegistry()).thenReturn(crates);
        hook = new MiniPlaceholdersHook(plugin);
    }

    private ItemStack createMockItem(Material material, int amount, String name) {
        ItemStack item = Mockito.mock(ItemStack.class);
        org.bukkit.inventory.meta.ItemMeta meta = Mockito.mock(org.bukkit.inventory.meta.ItemMeta.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(amount);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.hasDisplayName()).thenReturn(name != null);
        if (name != null) {
            when(meta.displayName()).thenReturn(Component.text(name));
        }
        when(meta.lore()).thenReturn(java.util.Collections.emptyList());
        when(item.clone()).thenReturn(item);
        return item;
    }

    @Test
    public void testParseOptionsCombinations() {
        ItemStack itemSingle = createMockItem(Material.DIAMOND, 1, "Diamond");
        ItemStack itemStack = createMockItem(Material.DIAMOND, 16, "Diamond");

        // 1. Default options
        ItemTextOptions def = MiniPlaceholdersHook.parseOptions(itemStack, null);
        assertNotNull(def);
        assertEquals(ItemTextOptions.DEFAULT_PATTERN, def.pattern());

        // Helper to create mock ArgumentQueue
        ArgumentQueue noSpriteQueue = mockQueue("no_sprite");
        ItemTextOptions noSpriteOpt = MiniPlaceholdersHook.parseOptions(itemStack, noSpriteQueue);
        assertEquals("<subscript_number> <item_displayname>", noSpriteOpt.pattern());

        ArgumentQueue noSpriteSingleQueue = mockQueue("no-sprite");
        ItemTextOptions noSpriteSingleOpt = MiniPlaceholdersHook.parseOptions(itemSingle, noSpriteSingleQueue);
        assertEquals("<item_displayname>", noSpriteSingleOpt.pattern());

        ArgumentQueue noAmountQueue = mockQueue("no_amount");
        ItemTextOptions noAmountOpt = MiniPlaceholdersHook.parseOptions(itemStack, noAmountQueue);
        assertEquals("<item_sprite> <item_displayname>", noAmountOpt.pattern());

        ArgumentQueue nameOnlyQueue = mockQueue("name_only");
        ItemTextOptions nameOnlyOpt = MiniPlaceholdersHook.parseOptions(itemStack, nameOnlyQueue);
        assertEquals("<item_displayname>", nameOnlyOpt.pattern());

        ArgumentQueue spriteOnlyQueue = mockQueue("sprite_only");
        ItemTextOptions spriteOnlyOpt = MiniPlaceholdersHook.parseOptions(itemStack, spriteOnlyQueue);
        assertEquals("<item_sprite>", spriteOnlyOpt.pattern());

        ArgumentQueue bracketsQueue = mockQueue("brackets", "no_hover");
        ItemTextOptions bracketsOpt = MiniPlaceholdersHook.parseOptions(itemStack, bracketsQueue);
        assertTrue(bracketsOpt.displayBrackets());
        assertFalse(bracketsOpt.hoverEnabled());
    }

    @Test
    public void testCrateAndKeyTagResolution() {
        Crate crate = new Crate("money");
        crate.setName("Money Crate");

        CrateItemConfig itemConfig = new CrateItemConfig();
        itemConfig.setItemStack(createMockItem(Material.CHEST, 1, "Money Crate"));
        crate.setItemConfig(itemConfig);

        CrateKeyConfig keyConfig = new CrateKeyConfig();
        keyConfig.setItemStack(createMockItem(Material.TRIPWIRE_HOOK, 1, "Money Key"));
        crate.setKeyConfig(keyConfig);

        crates.put("money", crate);

        TagResolver resolver = hook.createTagResolver();
        MiniMessage mm = MiniMessage.builder().tags(resolver).build();

        // Test <moderncrates_crate:money>
        Component crateComp = mm.deserialize("<moderncrates_crate:money>");
        assertNotNull(crateComp);
        String crateText = PlainTextComponentSerializer.plainText().serialize(crateComp);
        assertTrue(crateText.contains("Money Crate"));

        // Test <moderncrates_crate_money>
        Component crateUnderscoreComp = mm.deserialize("<moderncrates_crate_money>");
        assertNotNull(crateUnderscoreComp);

        // Test <moderncrates_key:money>
        Component keyComp = mm.deserialize("<moderncrates_key:money>");
        assertNotNull(keyComp);

        // Test <moderncrates_key_money>
        Component keyUnderscoreComp = mm.deserialize("<moderncrates_key_money>");
        assertNotNull(keyUnderscoreComp);

        // Test with option flags: <moderncrates_crate:money:no_sprite>
        Component noSpriteComp = mm.deserialize("<moderncrates_crate:money:no_sprite>");
        assertNotNull(noSpriteComp);

        // Test with option flags: <moderncrates_crate_money:name_only>
        Component nameOnlyComp = mm.deserialize("<moderncrates_crate_money:name_only>");
        assertNotNull(nameOnlyComp);
    }

    @Test
    public void testRewardTagResolution() {
        Crate crate = new Crate("money");
        crate.setName("Money Crate");

        Reward reward = new Reward("reward1458");
        RewardDisplay display = new RewardDisplay();
        display.setItemStack(createMockItem(Material.DIAMOND, 5, "Diamond"));
        reward.setDisplay(display);

        Map<String, Reward> rewardMap = new HashMap<>();
        rewardMap.put("reward1458", reward);
        crate.setRewards(rewardMap);

        crates.put("money", crate);

        TagResolver resolver = hook.createTagResolver();
        MiniMessage mm = MiniMessage.builder().tags(resolver).build();

        // Test <moderncrates_reward:money:reward1458>
        Component rewardComp = mm.deserialize("<moderncrates_reward:money:reward1458>");
        assertNotNull(rewardComp);

        // Test <moderncrates_reward_money:reward1458>
        Component rewardUnderscoreComp = mm.deserialize("<moderncrates_reward_money:reward1458>");
        assertNotNull(rewardUnderscoreComp);

        // Test with options: <moderncrates_reward_money:reward1458:no_amount>
        Component noAmountComp = mm.deserialize("<moderncrates_reward_money:reward1458:no_amount>");
        assertNotNull(noAmountComp);

        // Test with options: <moderncrates_reward:money:reward1458:name_only>
        Component nameOnlyComp = mm.deserialize("<moderncrates_reward:money:reward1458:name_only>");
        assertNotNull(nameOnlyComp);
    }

    private ArgumentQueue mockQueue(String... args) {
        return new ArgumentQueue() {
            private int index = 0;

            @Override
            public Tag.Argument pop() {
                if (!hasNext()) return null;
                String val = args[index++];
                return () -> val;
            }

            @Override
            public Tag.Argument popOr(String message) {
                Tag.Argument arg = pop();
                if (arg == null) throw new IllegalArgumentException(message);
                return arg;
            }

            @Override
            public Tag.Argument popOr(java.util.function.Supplier<String> messageSupplier) {
                Tag.Argument arg = pop();
                if (arg == null) throw new IllegalArgumentException(messageSupplier.get());
                return arg;
            }

            @Override
            public boolean hasNext() {
                return index < args.length;
            }

            @Override
            public Tag.Argument peek() {
                if (!hasNext()) return null;
                String val = args[index];
                return () -> val;
            }

            @Override
            public void reset() {
                index = 0;
            }
        };
    }
}
