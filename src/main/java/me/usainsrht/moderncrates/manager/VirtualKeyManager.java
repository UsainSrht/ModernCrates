package me.usainsrht.moderncrates.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Manages virtual keys stored in a YAML-based flatfile database.
 */
public class VirtualKeyManager {

    private final File file;
    private final Logger logger;
    private final ConcurrentHashMap<String, Integer> cache = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "ModernCrates-VirtualKeys-IO");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean modified = new AtomicBoolean(false);
    private final Object saveLock = new Object();

    public VirtualKeyManager(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "virtual_keys.yml");
        this.logger = logger;
    }

    public void load() {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                logger.warning("Failed to create virtual_keys.yml: " + e.getMessage());
            }
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        cache.clear();
        boolean foundZeros = false;
        for (String key : yaml.getKeys(true)) {
            if (yaml.isInt(key)) {
                int amount = yaml.getInt(key);
                if (amount > 0) {
                    cache.put(key, amount);
                } else {
                    foundZeros = true;
                }
            }
        }
        modified.set(false);
        if (foundZeros) {
            saveAsync();
        }
    }

    /**
     * Synchronously saves virtual keys to disk, cleaning up 0-amount entries
     * and removing empty UUID sections, while preserving unloaded crates with > 0 keys.
     */
    public void save() {
        saveSync();
    }

    /**
     * Synchronously saves virtual keys to disk.
     */
    public void saveSync() {
        synchronized (saveLock) {
            writeSnapshot(getPositiveSnapshot());
        }
    }

    private CompletableFuture<Void> nextFuture = null;

    /**
     * Asynchronously saves virtual keys to disk without hanging the main thread.
     * Coalesces concurrent save requests so callers receive a future that completes
     * when their modifications have been written to disk.
     */
    public synchronized CompletableFuture<Void> saveAsync() {
        if (nextFuture != null) {
            return nextFuture;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        nextFuture = future;
        ioExecutor.submit(() -> {
            synchronized (VirtualKeyManager.this) {
                nextFuture = null;
            }
            try {
                saveSync();
                future.complete(null);
            } catch (Throwable t) {
                logger.warning("Error during async virtual keys save: " + t.getMessage());
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Shuts down background IO executor and ensures pending data is written.
     */
    public void shutdown() {
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        saveSync();
    }

    private Map<String, Integer> getPositiveSnapshot() {
        Map<String, Integer> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : cache.entrySet()) {
            Integer amount = entry.getValue();
            if (amount != null && amount > 0) {
                snapshot.put(entry.getKey(), amount);
            }
        }
        return snapshot;
    }

    private void writeSnapshot(Map<String, Integer> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                yaml.set(entry.getKey(), entry.getValue());
            }
        }
        try {
            yaml.save(file);
            modified.set(false);
        } catch (IOException e) {
            logger.warning("Failed to save virtual_keys.yml: " + e.getMessage());
        }
    }

    public boolean isModified() {
        return modified.get();
    }

    public int getKeys(Player player, String crateId) {
        return getKeys(player.getUniqueId(), crateId);
    }

    public int getKeys(UUID uuid, String crateId) {
        String path = uuid.toString() + "." + crateId;
        return cache.getOrDefault(path, 0);
    }

    public Map<String, Integer> getPlayerKeys(UUID uuid) {
        Map<String, Integer> result = new LinkedHashMap<>();
        String prefix = uuid.toString() + ".";
        for (Map.Entry<String, Integer> entry : cache.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                Integer amount = entry.getValue();
                if (amount != null && amount > 0) {
                    result.put(entry.getKey().substring(prefix.length()), amount);
                }
            }
        }
        return result;
    }

    public void setKeys(Player player, String crateId, int amount) {
        setKeys(player.getUniqueId(), crateId, amount);
    }

    public void setKeys(UUID uuid, String crateId, int amount) {
        String path = uuid.toString() + "." + crateId;
        if (amount <= 0) {
            cache.remove(path);
        } else {
            cache.put(path, amount);
        }
        modified.set(true);
        saveAsync();
    }

    public void addKeys(Player player, String crateId, int amount) {
        addKeys(player.getUniqueId(), crateId, amount);
    }

    public void addKeys(UUID uuid, String crateId, int amount) {
        if (amount <= 0) return;
        int current = getKeys(uuid, crateId);
        setKeys(uuid, crateId, current + amount);
    }

    public boolean removeKey(Player player, String crateId) {
        return removeKeys(player.getUniqueId(), crateId, 1);
    }

    public boolean removeKey(UUID uuid, String crateId) {
        return removeKeys(uuid, crateId, 1);
    }

    public boolean removeKeys(Player player, String crateId, int amount) {
        return removeKeys(player.getUniqueId(), crateId, amount);
    }

    public boolean removeKeys(UUID uuid, String crateId, int amount) {
        int current = getKeys(uuid, crateId);
        if (current <= 0) return false;
        int newAmount = Math.max(0, current - amount);
        setKeys(uuid, crateId, newAmount);
        return true;
    }
}
