package me.usainsrht.moderncrates.manager;

import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateKeyConfig;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.stream.Collectors;

/**
 * Manages physical key creation and checking.
 */
public class KeyManager {

    /**
     * Creates a physical key ItemStack for a crate.
     */
    @SuppressWarnings("deprecation")
    public ItemStack createKey(Crate crate, int amount) {
        CrateKeyConfig config = crate.getKeyConfig();
        if (config == null) return null;

        Material mat = Material.matchMaterial(config.getMaterial().toUpperCase());
        if (mat == null) mat = Material.TRIPWIRE_HOOK;

        ItemStack key = new ItemStack(mat, amount);
        ItemMeta meta = key.getItemMeta();
        if (meta == null) return key;

        if (config.getName() != null) {
            meta.displayName(TextUtil.parse(config.getName()));
        }
        if (config.getLore() != null) {
            meta.lore(config.getLore().stream()
                    .map(TextUtil::parse)
                    .collect(Collectors.toList()));
        }
        if (config.getEnchantments() != null) {
            for (var entry : config.getEnchantments().entrySet()) {
                Enchantment ench = Enchantment.getByName(entry.getKey().toUpperCase());
                if (ench != null) {
                    meta.addEnchant(ench, entry.getValue(), true);
                }
            }
        }
        if (config.getItemFlags() != null) {
            for (String flag : config.getItemFlags()) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Store crate ID in persistent data for identification
        meta.getPersistentDataContainer().set(
                org.bukkit.NamespacedKey.fromString("moderncrates:crate_key"),
                org.bukkit.persistence.PersistentDataType.STRING,
                crate.getId()
        );

        key.setItemMeta(meta);
        return key;
    }

    /**
     * Creates a crate item for giving to players.
     */
    public ItemStack createCrateItem(Crate crate, int amount) {
        var config = crate.getItemConfig();
        if (config == null) return null;

        Material mat = Material.matchMaterial(config.getMaterial().toUpperCase());
        if (mat == null) mat = Material.CHEST;

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (config.getName() != null) {
            meta.displayName(TextUtil.parse(config.getName()));
        }
        if (config.getLore() != null) {
            meta.lore(config.getLore().stream()
                    .map(TextUtil::parse)
                    .collect(Collectors.toList()));
        }

        // Store crate ID
        meta.getPersistentDataContainer().set(
                org.bukkit.NamespacedKey.fromString("moderncrates:crate_item"),
                org.bukkit.persistence.PersistentDataType.STRING,
                crate.getId()
        );

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Checks if a player has a physical key for the crate.
     */
    public boolean hasKey(Player player, Crate crate) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isKey(item, crate)) return true;
        }
        return false;
    }

    /**
     * Removes one physical key from the player's inventory.
     * Returns true if successful.
     */
    public boolean removeKey(Player player, Crate crate) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isKey(item, crate)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItem(i, null);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an item matches a crate's key.
     */
    public boolean isKey(ItemStack item, Crate crate) {
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        var key = org.bukkit.NamespacedKey.fromString("moderncrates:crate_key");
        if (!pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) return false;
        String crateId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
        return crate.getId().equals(crateId);
    }

    /**
     * Gets the crate ID from a crate item.
     */
    public String getCrateIdFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        var key = org.bukkit.NamespacedKey.fromString("moderncrates:crate_item");
        if (!pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) return null;
        return pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
    }
}
