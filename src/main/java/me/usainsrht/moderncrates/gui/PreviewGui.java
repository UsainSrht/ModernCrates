package me.usainsrht.moderncrates.gui;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.PreviewConfig;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.SoundUtil;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Paginated preview GUI for viewing crate rewards.
 */
public class PreviewGui implements ModernCratesGui {

    private final Player player;
    private final Crate crate;
    private int currentPage = 0;
    private Inventory inventory;

    private final List<Reward> rewards;
    private final int itemsPerPage;
    private final Set<Integer> reservedSlots = new HashSet<>();

    public PreviewGui(Player player, Crate crate) {
        this.player = player;
        this.crate = crate;
        this.rewards = new ArrayList<>(crate.getRewards().values());

        PreviewConfig config = crate.getPreviewConfig();
        int totalSlots = config != null ? config.getRows() * 9 : 54;

        if (config != null) {
            if (config.getCloseButton() != null) reservedSlots.add(config.getCloseButton().getSlot());
            if (config.getNextButton() != null) reservedSlots.add(config.getNextButton().getSlot());
            if (config.getPreviousButton() != null) reservedSlots.add(config.getPreviousButton().getSlot());
            if (config.getCustomSlots() != null) reservedSlots.addAll(config.getCustomSlots().keySet());
        }

        itemsPerPage = totalSlots - reservedSlots.size();
    }

    public void open() {
        render();
        player.openInventory(inventory);

        PreviewConfig config = crate.getPreviewConfig();
        if (config != null && config.getSounds() != null) {
            SoundUtil.play(player, config.getSounds());
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        PreviewConfig config = crate.getPreviewConfig();
        if (config != null) {
            if (config.getNextButton() != null && config.getNextButton().getSlot() == slot) {
                nextPage();
            } else if (config.getPreviousButton() != null && config.getPreviousButton().getSlot() == slot) {
                previousPage();
            } else if (config.getCloseButton() != null && config.getCloseButton().getSlot() == slot) {
                player.closeInventory();
            }
        }
    }

    private void nextPage() {
        if (currentPage < getMaxPage()) {
            currentPage++;
            render();
            player.openInventory(inventory);
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            render();
            player.openInventory(inventory);
        }
    }

    private void render() {
        PreviewConfig config = crate.getPreviewConfig();
        String title = config != null ? config.getTitle() : "&6Preview";
        int rows = config != null ? config.getRows() : 6;

        inventory = Bukkit.createInventory(this, rows * 9, TextUtil.parse(title));

        // Fill background
        if (config != null && config.getFill() != null) {
            ItemStack fill = ItemBuilder.create(config.getFill().getMaterial(), config.getFill().getName(), null);
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, fill);
            }
        }

        // Place custom slots
        if (config != null && config.getCustomSlots() != null) {
            for (var entry : config.getCustomSlots().entrySet()) {
                int slot = entry.getKey();
                var item = entry.getValue();
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, ItemBuilder.create(item.getMaterial(), item.getName(), item.getLore()));
                }
            }
        }

        // Place navigation
        if (config != null) {
            if (config.getCloseButton() != null) placeNavItem(config.getCloseButton());
            if (config.getNextButton() != null && currentPage < getMaxPage()) placeNavItem(config.getNextButton());
            if (config.getPreviousButton() != null && currentPage > 0) placeNavItem(config.getPreviousButton());
        }

        // Place rewards
        int startIndex = currentPage * itemsPerPage;
        int rewardIdx = 0;
        for (int slot = 0; slot < inventory.getSize() && startIndex + rewardIdx < rewards.size(); slot++) {
            if (reservedSlots.contains(slot)) continue;
            Reward reward = rewards.get(startIndex + rewardIdx);
            if (reward.getDisplay() != null) {
                inventory.setItem(slot, ItemBuilder.fromDisplay(reward.getDisplay()));
            }
            rewardIdx++;
        }
    }

    private void placeNavItem(PreviewConfig.SlotItem item) {
        if (item.getSlot() >= 0 && item.getSlot() < inventory.getSize()) {
            inventory.setItem(item.getSlot(), ItemBuilder.create(item.getMaterial(), item.getName(), item.getLore()));
        }
    }

    private int getMaxPage() {
        if (itemsPerPage <= 0) return 0;
        return Math.max(0, (rewards.size() - 1) / itemsPerPage);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Crate getCrate() {
        return crate;
    }
}
