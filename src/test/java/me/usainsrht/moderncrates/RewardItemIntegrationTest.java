package me.usainsrht.moderncrates;

import me.usainsrht.moderncrates.api.reward.RewardDisplay;
import me.usainsrht.moderncrates.api.reward.RewardItem;
import me.usainsrht.moderncrates.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RewardItemIntegrationTest {

    @Test
    public void testRewardItemDynamicGettersFromItemStack() {
        ItemStack mockStack = mock(ItemStack.class);
        ItemMeta mockMeta = mock(ItemMeta.class);

        when(mockStack.getType()).thenReturn(Material.DIAMOND);
        when(mockStack.getAmount()).thenReturn(5);
        when(mockStack.hasItemMeta()).thenReturn(true);
        when(mockStack.getItemMeta()).thenReturn(mockMeta);
        when(mockMeta.hasDisplayName()).thenReturn(true);
        when(mockMeta.displayName()).thenReturn(Component.text("Special Diamond"));
        when(mockMeta.lore()).thenReturn(List.of(Component.text("Rare gem"), Component.text("Tier 1")));
        when(mockMeta.isHideTooltip()).thenReturn(true);
        when(mockStack.clone()).thenReturn(mockStack);

        RewardItem item = new RewardItem();
        item.setItemStack(mockStack);

        // Verify dynamic getters resolve from itemStack when fields are null
        assertEquals("DIAMOND", item.getMaterial());
        assertEquals(5, item.getAmount());
        assertEquals("Special Diamond", item.getName());
        assertEquals(List.of("Rare gem", "Tier 1"), item.getLore());
        assertTrue(item.isHideTooltip());
        assertNotNull(item.getItemStack());

        // Verify ItemBuilder.fromRewardItem returns the cloned ItemStack
        ItemStack fromReward = ItemBuilder.fromRewardItem(item);
        assertNotNull(fromReward);
        assertEquals(Material.DIAMOND, fromReward.getType());
        assertEquals(5, fromReward.getAmount());
    }

    @Test
    public void testToRewardItemPreservesItemStackWithoutWiping() {
        ItemStack mockStack = mock(ItemStack.class);
        ItemMeta mockMeta = mock(ItemMeta.class);

        when(mockStack.getType()).thenReturn(Material.GOLDEN_APPLE);
        when(mockStack.getAmount()).thenReturn(3);
        when(mockStack.hasItemMeta()).thenReturn(true);
        when(mockStack.getItemMeta()).thenReturn(mockMeta);
        when(mockMeta.hasDisplayName()).thenReturn(true);
        when(mockMeta.displayName()).thenReturn(Component.text("Notch Apple"));
        when(mockStack.clone()).thenReturn(mockStack);

        RewardItem rewardItem = ItemBuilder.toRewardItem(mockStack);

        // Verify itemStack was NOT wiped to null by setters
        assertNotNull(rewardItem.getItemStack());
        assertEquals(Material.GOLDEN_APPLE, rewardItem.getItemStack().getType());
        assertEquals(3, rewardItem.getItemStack().getAmount());
        assertEquals("GOLDEN_APPLE", rewardItem.getMaterial());
        assertEquals(3, rewardItem.getAmount());

        ItemStack convertedBack = ItemBuilder.fromRewardItem(rewardItem);
        assertNotNull(convertedBack);
        assertEquals(Material.GOLDEN_APPLE, convertedBack.getType());
        assertEquals(3, convertedBack.getAmount());
    }

    @Test
    public void testRewardDisplayDynamicGettersAndToRewardDisplay() {
        ItemStack mockSword = mock(ItemStack.class);
        ItemMeta mockMeta = mock(ItemMeta.class);

        when(mockSword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(mockSword.getAmount()).thenReturn(1);
        when(mockSword.hasItemMeta()).thenReturn(true);
        when(mockSword.getItemMeta()).thenReturn(mockMeta);
        when(mockMeta.hasDisplayName()).thenReturn(true);
        when(mockMeta.displayName()).thenReturn(Component.text("Excalibur"));
        when(mockSword.clone()).thenReturn(mockSword);

        RewardDisplay display = ItemBuilder.toRewardDisplay(mockSword);

        assertNotNull(display.getItemStack());
        assertEquals("DIAMOND_SWORD", display.getMaterial());
        assertEquals(1, display.getAmount());
        assertEquals("Excalibur", display.getName());

        ItemStack fromDisplay = ItemBuilder.fromDisplay(display, 10.0);
        assertNotNull(fromDisplay);
        assertEquals(Material.DIAMOND_SWORD, fromDisplay.getType());
    }

    @Test
    public void testRewardItemManualProperties() {
        RewardItem manual = new RewardItem();
        manual.setMaterial("EMERALD");
        manual.setAmount(10);
        manual.setName("<green>Emeralds");
        manual.setLore(List.of("<gray>Currency"));
        manual.setHideTooltip(true);

        assertEquals("EMERALD", manual.getMaterial());
        assertEquals(10, manual.getAmount());
        assertEquals("<green>Emeralds", manual.getName());
        assertEquals(List.of("<gray>Currency"), manual.getLore());
        assertTrue(manual.isHideTooltip());
    }
}
