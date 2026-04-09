package me.usainsrht.moderncrates;

import me.usainsrht.moderncrates.animation.ClickAnimationType;
import me.usainsrht.moderncrates.animation.CsgoAnimationType;
import me.usainsrht.moderncrates.animation.ItemRiseAnimationType;
import me.usainsrht.moderncrates.animation.ScratchcardAnimationType;
import me.usainsrht.moderncrates.animation.SlotAnimationType;
import me.usainsrht.moderncrates.api.ModernCratesAPI;
import me.usainsrht.moderncrates.api.ModernCratesProvider;
import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.AnimationType;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.command.ModernCratesCommand;
import me.usainsrht.moderncrates.config.AnimationConfigParser;
import me.usainsrht.moderncrates.config.CrateConfigParser;
import me.usainsrht.moderncrates.config.PluginConfig;
import me.usainsrht.moderncrates.gui.ChatInputManager;
import me.usainsrht.moderncrates.gui.SignInputManager;
import me.usainsrht.moderncrates.listener.CrateInteractListener;
import me.usainsrht.moderncrates.listener.GuiListener;
import me.usainsrht.moderncrates.listener.PlayerListener;
import me.usainsrht.moderncrates.manager.AnimationManager;
import me.usainsrht.moderncrates.manager.HologramManager;
import me.usainsrht.moderncrates.manager.KeyManager;
import me.usainsrht.moderncrates.manager.VirtualKeyManager;
import me.usainsrht.moderncrates.hologram.HologramProvider;
import me.usainsrht.moderncrates.hologram.VanillaHologramProvider;
import me.usainsrht.moderncrates.util.SoundUtil;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.morepaperlib.MorePaperLib;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main plugin class for ModernCrates.
 */
public class ModernCratesPlugin extends JavaPlugin {

    private MorePaperLib morePaperLib;
    private GracefulScheduling scheduling;

    // Config
    private PluginConfig pluginConfig;
    private AnimationConfigParser animationConfigParser;
    private CrateConfigParser crateConfigParser;

    // Registries
    private final Map<String, AnimationType> animationTypeRegistry = new ConcurrentHashMap<>();
    private final Map<String, Animation> animationRegistry = new ConcurrentHashMap<>();
    private final Map<String, Crate> crateRegistry = new ConcurrentHashMap<>();

    // Managers
    private AnimationManager animationManager;
    private KeyManager keyManager;
    private VirtualKeyManager virtualKeyManager;
    private HologramManager hologramManager;
    private ChatInputManager chatInputManager;
    private SignInputManager signInputManager;

    // API
    private ModernCratesAPIImpl apiImpl;

    @Override
    public void onEnable() {
        // Initialize MorePaperLib
        morePaperLib = new MorePaperLib(this);
        scheduling = morePaperLib.scheduling();

        // Managers
        animationManager = new AnimationManager();
        keyManager = new KeyManager();
        virtualKeyManager = new VirtualKeyManager(getDataFolder(), getLogger());
        hologramManager = new HologramManager();
        chatInputManager = new ChatInputManager();
        signInputManager = new SignInputManager();

        // Config parsers
        animationConfigParser = new AnimationConfigParser(getLogger());
        crateConfigParser = new CrateConfigParser(getLogger());

        // Load config
        pluginConfig = new PluginConfig(getDataFolder());
        try {
            pluginConfig.createDefaults();
        } catch (IOException e) {
            getLogger().warning("Failed to create default config: " + e.getMessage());
        }
        pluginConfig.load();

        // Resolve hologram provider from config
        hologramManager.setProvider(resolveHologramProvider());

        // Create directories and default files
        File cratesDir = new File(getDataFolder(), "crates");
        File animationsDir = new File(getDataFolder(), "animations");
        if (!cratesDir.exists()) {
            cratesDir.mkdirs();
            saveResource("crates/example.yml", false);
            saveResource("crates/casino.yml", false);
            saveResource("crates/instant.yml", false);
            saveResource("crates/mystery_box.yml", false);
            saveResource("crates/roulette.yml", false);
            saveResource("crates/scratchcard_crate.yml", false);
            saveResource("crates/slot_machine.yml", false);
            saveResource("crates/treasure_chest.yml", false);
        }
        if (!animationsDir.exists()) {
            animationsDir.mkdirs();
            saveResource("animations/casino_roulette.yml", false);
            saveResource("animations/click.yml", false);
            saveResource("animations/csgo.yml", false);
            saveResource("animations/instant_click.yml", false);
            saveResource("animations/item_rise.yml", false);
            saveResource("animations/roulette.yml", false);
            saveResource("animations/scratchcard.yml", false);
            saveResource("animations/slot.yml", false);
        }

        // Register built-in animation types
        registerBuiltinAnimationTypes();

        // Load animations and crates
        loadAnimations();
        loadCrates();

        // Load virtual keys
        virtualKeyManager.load();

        // Register commands (Brigadier)
        new ModernCratesCommand(this).register();

        // Register listeners
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new CrateInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Set up API
        apiImpl = new ModernCratesAPIImpl();
        ModernCratesProvider.set(apiImpl);

        // Create holograms
        scheduling.globalRegionalScheduler().runDelayed(() -> {
            hologramManager.refreshAll(crateRegistry);
        }, 20L); // Delay to ensure worlds are loaded

        getLogger().info("ModernCrates enabled! Loaded " + crateRegistry.size() + " crates and "
                + animationRegistry.size() + " animations.");
    }

    @Override
    public void onDisable() {
        // Cancel all animations
        animationManager.cancelAll();

        // Save virtual keys
        virtualKeyManager.save();

        // Remove holograms
        hologramManager.removeAll();

        // Clean up scheduling
        if (scheduling != null) {
            scheduling.cancelGlobalTasks();
        }

        // Unset API
        ModernCratesProvider.unset();

        chatInputManager.clear();
        signInputManager.clear();
        getLogger().info("ModernCrates disabled.");
    }

    private HologramProvider resolveHologramProvider() {
        String system = pluginConfig.getHologramSystem().toUpperCase();
        switch (system) {
            case "DECENT_HOLOGRAMS", "DECENTHOLOGRAMS" -> {
                if (getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
                    getLogger().info("Using DecentHolograms for holograms.");
                    return new me.usainsrht.moderncrates.hologram.DecentHologramsProvider();
                }
                getLogger().warning("DecentHolograms not found! Falling back to VANILLA holograms.");
            }
            case "HOLOGRAPHIC_DISPLAYS", "HOLOGRAPHICDISPLAYS" -> {
                if (getServer().getPluginManager().getPlugin("HolographicDisplays") != null) {
                    getLogger().info("Using HolographicDisplays for holograms.");
                    return new me.usainsrht.moderncrates.hologram.HolographicDisplaysProvider(this);
                }
                getLogger().warning("HolographicDisplays not found! Falling back to VANILLA holograms.");
            }
            case "FANCY_HOLOGRAMS", "FANCYHOLOGRAMS" -> {
                if (getServer().getPluginManager().getPlugin("FancyHolograms") != null) {
                    getLogger().info("Using FancyHolograms for holograms.");
                    return new me.usainsrht.moderncrates.hologram.FancyHologramsProvider();
                }
                getLogger().warning("FancyHolograms not found! Falling back to VANILLA holograms.");
            }
            default -> {
                if (!"VANILLA".equals(system)) {
                    getLogger().warning("Unknown hologram system '" + pluginConfig.getHologramSystem()
                            + "'! Using VANILLA. Valid: VANILLA, DECENT_HOLOGRAMS, HOLOGRAPHIC_DISPLAYS, FANCY_HOLOGRAMS");
                }
            }
        }
        getLogger().info("Using vanilla TextDisplay holograms.");
        return new VanillaHologramProvider();
    }

    private void registerBuiltinAnimationTypes() {
        animationTypeRegistry.put("csgo", new CsgoAnimationType(scheduling));
        animationTypeRegistry.put("click", new ClickAnimationType(scheduling));
        animationTypeRegistry.put("scratchcard", new ScratchcardAnimationType(scheduling));
        animationTypeRegistry.put("slot", new SlotAnimationType(scheduling));
        animationTypeRegistry.put("item_rise", new ItemRiseAnimationType(scheduling, this));
    }

    private void loadAnimations() {
        animationRegistry.clear();
        File animDir = new File(getDataFolder(), "animations");
        animationRegistry.putAll(animationConfigParser.loadAll(animDir));
    }

    private void loadCrates() {
        crateRegistry.clear();
        File cratesDir = new File(getDataFolder(), "crates");
        crateRegistry.putAll(crateConfigParser.loadAll(cratesDir));
    }

    public void reloadPlugin() {
        // Cancel ongoing sessions
        animationManager.cancelAll();
        hologramManager.removeAll();

        // Reload config
        pluginConfig.load();

        // Resolve hologram provider (may have changed)
        hologramManager.setProvider(resolveHologramProvider());

        // Reload animations and crates
        loadAnimations();
        loadCrates();

        // Reload virtual keys
        virtualKeyManager.save();
        virtualKeyManager.load();

        // Refresh holograms
        hologramManager.refreshAll(crateRegistry);
    }

    /**
     * Tries to open a crate for a player.
     *
     * @return true when an animation session is started, false otherwise.
     */
    public boolean tryOpenCrate(Player player, Crate crate) {
        // Check if player already has an active session
        if (animationManager.hasActiveSession(player)) {
            player.sendMessage(TextUtil.parse("<red>You already have a crate open!"));
            return false;
        }

        // Check for key requirement
        if (crate.requiresKey()) {
            // Try physical key first
            if (keyManager.hasKey(player, crate)) {
                keyManager.removeKey(player, crate);
            } else if (virtualKeyManager.getKeys(player, crate.getId()) > 0) {
                virtualKeyManager.removeKey(player, crate.getId());
            } else {
                String msg = pluginConfig.getPrefix()
                        + pluginConfig.getMessage("no_key").replace("<crate>", crate.getName());
                player.sendMessage(TextUtil.parse(msg));
                SoundUtil.play(player, pluginConfig.getSound("no_key"));
                return false;
            }
        }

        // Find animation
        Animation animation = animationRegistry.get(crate.getAnimationId());
        if (animation == null) {
            player.sendMessage(TextUtil.parse("<red>Animation not found: " + crate.getAnimationId()));
            return false;
        }

        // Find animation type
        AnimationType type = animationTypeRegistry.get(animation.getTypeId());
        if (type == null) {
            player.sendMessage(TextUtil.parse("<red>Animation type not found: " + animation.getTypeId()));
            return false;
        }

        // Start animation
        animationManager.startSession(player, crate, type, animation);
        return true;
    }

    /**
     * Opens a crate for a player, handling key checks and animation startup.
     */
    public void openCrate(Player player, Crate crate) {
        tryOpenCrate(player, crate);
    }

    // --- Getters ---

    public GracefulScheduling getScheduling() { return scheduling; }
    public PluginConfig getPluginConfig() { return pluginConfig; }
    public AnimationConfigParser getAnimationConfigParser() { return animationConfigParser; }
    public CrateConfigParser getCrateConfigParser() { return crateConfigParser; }
    public Map<String, AnimationType> getAnimationTypeRegistry() { return animationTypeRegistry; }
    public Map<String, Animation> getAnimationRegistry() { return animationRegistry; }
    public Map<String, Crate> getCrateRegistry() { return crateRegistry; }
    public AnimationManager getAnimationManager() { return animationManager; }
    public KeyManager getKeyManager() { return keyManager; }
    public VirtualKeyManager getVirtualKeyManager() { return virtualKeyManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public ChatInputManager getChatInputManager() { return chatInputManager; }
    public SignInputManager getSignInputManager() { return signInputManager; }

    // --- API Implementation ---

    private class ModernCratesAPIImpl implements ModernCratesAPI {

        @Override
        public void registerAnimationType(String id, AnimationType type) {
            animationTypeRegistry.put(id, type);
        }

        @Override
        public void unregisterAnimationType(String id) {
            animationTypeRegistry.remove(id);
        }

        @Override
        public Optional<AnimationType> getAnimationType(String id) {
            return Optional.ofNullable(animationTypeRegistry.get(id));
        }

        @Override
        public Collection<String> getRegisteredAnimationTypeIds() {
            return Collections.unmodifiableCollection(animationTypeRegistry.keySet());
        }

        @Override
        public void registerAnimation(String id, Animation animation) {
            animationRegistry.put(id, animation);
        }

        @Override
        public void unregisterAnimation(String id) {
            animationRegistry.remove(id);
        }

        @Override
        public Optional<Animation> getAnimation(String id) {
            return Optional.ofNullable(animationRegistry.get(id));
        }

        @Override
        public Collection<String> getRegisteredAnimationIds() {
            return Collections.unmodifiableCollection(animationRegistry.keySet());
        }

        @Override
        public void registerCrate(Crate crate) {
            crateRegistry.put(crate.getId(), crate);
        }

        @Override
        public void unregisterCrate(String id) {
            crateRegistry.remove(id);
        }

        @Override
        public Optional<Crate> getCrate(String id) {
            return Optional.ofNullable(crateRegistry.get(id));
        }

        @Override
        public Collection<Crate> getCrates() {
            return Collections.unmodifiableCollection(crateRegistry.values());
        }

        @Override
        public Collection<String> getCrateIds() {
            return Collections.unmodifiableCollection(crateRegistry.keySet());
        }

        @Override
        public void openCrate(Player player, Crate crate) {
            ModernCratesPlugin.this.openCrate(player, crate);
        }

        @Override
        public void giveCrateKey(Player player, String crateId, int amount) {
            Crate crate = crateRegistry.get(crateId);
            if (crate == null) return;
            var key = keyManager.createKey(crate, amount);
            if (key != null) player.getInventory().addItem(key);
        }

        @Override
        public void giveCrateItem(Player player, String crateId, int amount) {
            Crate crate = crateRegistry.get(crateId);
            if (crate == null) return;
            var item = keyManager.createCrateItem(crate, amount);
            if (item != null) player.getInventory().addItem(item);
        }

        @Override
        public int getVirtualKeys(Player player, String crateId) {
            return virtualKeyManager.getKeys(player, crateId);
        }

        @Override
        public void setVirtualKeys(Player player, String crateId, int amount) {
            virtualKeyManager.setKeys(player, crateId, amount);
        }
    }
}
