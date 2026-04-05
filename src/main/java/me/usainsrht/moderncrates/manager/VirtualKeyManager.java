package me.usainsrht.moderncrates.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages virtual keys stored in a YAML-based flatfile database.
 */
public class VirtualKeyManager {

    private final File file;
    private final Logger logger;
    private YamlConfiguration yaml;
    private final ConcurrentHashMap<String, Integer> cache = new ConcurrentHashMap<>();

    public VirtualKeyManager(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "virtual_keys.yml");
        this.logger = logger;
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                logger.warning("Failed to create virtual_keys.yml: " + e.getMessage());
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);

        cache.clear();
        for (String key : yaml.getKeys(true)) {
            if (yaml.isInt(key)) {
                cache.put(key, yaml.getInt(key));
            }
        }
    }

    public void save() {
        try {
            for (var entry : cache.entrySet()) {
                yaml.set(entry.getKey(), entry.getValue());
            }
            yaml.save(file);
        } catch (IOException e) {
            logger.warning("Failed to save virtual_keys.yml: " + e.getMessage());
        }
    }

    public int getKeys(Player player, String crateId) {
        return getKeys(player.getUniqueId(), crateId);
    }

    public int getKeys(UUID uuid, String crateId) {
        String path = uuid.toString() + "." + crateId;
        return cache.getOrDefault(path, 0);
    }

    public void setKeys(Player player, String crateId, int amount) {
        setKeys(player.getUniqueId(), crateId, amount);
    }

    public void setKeys(UUID uuid, String crateId, int amount) {
        String path = uuid.toString() + "." + crateId;
        cache.put(path, Math.max(0, amount));
    }

    public void addKeys(Player player, String crateId, int amount) {
        int current = getKeys(player, crateId);
        setKeys(player, crateId, current + amount);
    }

    public boolean removeKey(Player player, String crateId) {
        int current = getKeys(player, crateId);
        if (current <= 0) return false;
        setKeys(player, crateId, current - 1);
        return true;
    }
}
