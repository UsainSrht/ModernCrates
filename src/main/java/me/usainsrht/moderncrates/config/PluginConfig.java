package me.usainsrht.moderncrates.config;

import me.usainsrht.yamlmessage.YamlMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main plugin configuration (config.yml).
 */
public class PluginConfig {

    private final File file;
    private YamlConfiguration yaml;

    private String prefix;
    private String hologramSystem;
    private boolean giveFullInventoryDrop;
    private boolean allowShiftLeftClickRemove;
    private String announcementMuteMetaKey;
    private final Map<String, YamlMessage> messages = new LinkedHashMap<>();
    private final Map<String, YamlMessage> sounds = new LinkedHashMap<>();
    private final Map<String, Object> rawMessages = new LinkedHashMap<>();
    private final Map<String, Object> rawSounds = new LinkedHashMap<>();
    private CommandConfig commandConfig;

    public PluginConfig(File dataFolder) {
        this.file = new File(dataFolder, "config.yml");
    }

    public void load() {
        yaml = YamlConfiguration.loadConfiguration(file);

        prefix = yaml.getString("prefix", "<gold>MC <gray>> ");
        hologramSystem = yaml.getString("hologram-system", "FANCY_HOLOGRAMS");
        giveFullInventoryDrop = yaml.getBoolean("give-full-inventory-drop", true);
        allowShiftLeftClickRemove = yaml.getBoolean("allow-shift-left-click-remove", true);
        announcementMuteMetaKey = yaml.getString("announcement-mute-meta-key", "mute-crate-announcements");

        messages.clear();
        rawMessages.clear();
        var msgSection = yaml.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                Object raw = msgSection.get(key);
                rawMessages.put(key, raw);
                messages.put(key, YamlMessage.parse(raw));
            }
        }

        sounds.clear();
        rawSounds.clear();
        var soundSection = yaml.getConfigurationSection("sounds");
        if (soundSection != null) {
            for (String key : soundSection.getKeys(false)) {
                Object raw = soundSection.get(key);
                rawSounds.put(key, raw);
                sounds.put(key, parseSoundMessage(raw));
            }
        }

        var cmdSection = yaml.getConfigurationSection("command");
        commandConfig = new CommandConfig();
        if (cmdSection != null) {
            commandConfig.setName(cmdSection.getString("name", "moderncrates"));
            commandConfig.setDescription(cmdSection.getString("description", "ModernCrates command"));
            commandConfig.setUsage(cmdSection.getString("usage", "/moderncrates"));
            commandConfig.setPermission(cmdSection.getString("permission", "moderncrates.use"));
            commandConfig.setAliases(cmdSection.getStringList("aliases"));
        }
    }

    public static @NotNull YamlMessage parseSoundMessage(Object raw) {
        if (raw == null) return YamlMessage.empty();
        if (raw instanceof ConfigurationSection section) {
            if (section.contains("sound") || section.contains("sounds")) {
                return YamlMessage.parse(section);
            }
            return YamlMessage.parse(Map.of("sound", section.getValues(false)));
        }
        if (raw instanceof Map<?, ?> map) {
            if (map.containsKey("sound") || map.containsKey("sounds")) {
                return YamlMessage.parse(map);
            }
            return YamlMessage.parse(Map.of("sound", map));
        }
        return YamlMessage.parse(Map.of("sounds", raw));
    }

    public void save() throws IOException {
        yaml.set("prefix", prefix);
        yaml.set("hologram-system", hologramSystem);
        yaml.set("give-full-inventory-drop", giveFullInventoryDrop);
        yaml.set("allow-shift-left-click-remove", allowShiftLeftClickRemove);
        yaml.set("announcement-mute-meta-key", announcementMuteMetaKey);

        for (var entry : rawMessages.entrySet()) {
            yaml.set("messages." + entry.getKey(), entry.getValue());
        }
        for (var entry : rawSounds.entrySet()) {
            yaml.set("sounds." + entry.getKey(), entry.getValue());
        }

        yaml.set("command.name", commandConfig.getName());
        yaml.set("command.description", commandConfig.getDescription());
        yaml.set("command.usage", commandConfig.getUsage());
        yaml.set("command.permission", commandConfig.getPermission());
        yaml.set("command.aliases", commandConfig.getAliases());

        yaml.save(file);
    }



    public String getPrefix() { return prefix; }
    public String getHologramSystem() { return hologramSystem; }
    public boolean isGiveFullInventoryDrop() { return giveFullInventoryDrop; }
    public boolean isAllowShiftLeftClickRemove() { return allowShiftLeftClickRemove; }
    public String getAnnouncementMuteMetaKey() { return announcementMuteMetaKey; }
    public void setAnnouncementMuteMetaKey(String key) { this.announcementMuteMetaKey = key; }

    public @NotNull YamlMessage getMessage(String key) {
        return messages.getOrDefault(key, YamlMessage.empty());
    }

    public @NotNull YamlMessage getSound(String key) {
        return sounds.getOrDefault(key, YamlMessage.empty());
    }

    public String getRawMessageString(String key) {
        Object raw = rawMessages.get(key);
        return raw != null ? String.valueOf(raw) : "";
    }

    public String getRawSoundString(String key) {
        Object raw = rawSounds.get(key);
        return raw != null ? String.valueOf(raw) : "";
    }

    public CommandConfig getCommandConfig() { return commandConfig; }
    public YamlConfiguration getYaml() { return yaml; }

    public static class CommandConfig {
        private String name = "moderncrates";
        private String description = "ModernCrates command";
        private String usage = "/moderncrates";
        private String permission = "moderncrates.use";
        private List<String> aliases = List.of("mc", "crate", "crates");

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getUsage() { return usage; }
        public void setUsage(String usage) { this.usage = usage; }
        public String getPermission() { return permission; }
        public void setPermission(String permission) { this.permission = permission; }
        public List<String> getAliases() { return aliases; }
        public void setAliases(List<String> aliases) { this.aliases = aliases; }
    }
}

