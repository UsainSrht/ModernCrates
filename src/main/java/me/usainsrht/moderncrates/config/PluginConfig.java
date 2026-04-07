package me.usainsrht.moderncrates.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Main plugin configuration (config.yml).
 */
public class PluginConfig {

    private final File file;
    private YamlConfiguration yaml;

    private String prefix;
    private Map<String, String> messages;
    private Map<String, String> sounds;
    private CommandConfig commandConfig;

    public PluginConfig(File dataFolder) {
        this.file = new File(dataFolder, "config.yml");
    }

    public void load() {
        yaml = YamlConfiguration.loadConfiguration(file);

        prefix = yaml.getString("prefix", "<gold>MC <gray>> ");

        messages = new java.util.HashMap<>();
        var msgSection = yaml.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                messages.put(key, msgSection.getString(key, ""));
            }
        }

        sounds = new java.util.HashMap<>();
        var soundSection = yaml.getConfigurationSection("sounds");
        if (soundSection != null) {
            for (String key : soundSection.getKeys(false)) {
                sounds.put(key, soundSection.getString(key, ""));
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

    public void save() throws IOException {
        yaml.set("prefix", prefix);

        for (var entry : messages.entrySet()) {
            yaml.set("messages." + entry.getKey(), entry.getValue());
        }
        for (var entry : sounds.entrySet()) {
            yaml.set("sounds." + entry.getKey(), entry.getValue());
        }

        yaml.set("command.name", commandConfig.getName());
        yaml.set("command.description", commandConfig.getDescription());
        yaml.set("command.usage", commandConfig.getUsage());
        yaml.set("command.permission", commandConfig.getPermission());
        yaml.set("command.aliases", commandConfig.getAliases());

        yaml.save(file);
    }

    public void createDefaults() throws IOException {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            yaml = new YamlConfiguration();
            prefix = "<gold>MC <dark_gray>> ";
            messages = Map.of(
                    "reload", "<green>reloaded!",
                    "no_key", "<red>You don't have a <dark_red><crate> <red>key to open this crate!",
                    "no_crate", "<red>No crate named <dark_red><crate><red>!",
                    "no_player", "<red>No player named <dark_red><player><red>!",
                    "crate_given", "<green>Crate <dark_green><crate> <green>given to <dark_green><player><green>!",
                    "key_given", "<green>Crate <dark_green><crate> <green>key given to <dark_green><player><green>!"
            );
            sounds = Map.of(
                    "reload", "ui.button.click",
                    "no_key", "entity.villager.no",
                    "no_crate", "entity.villager.no",
                    "no_player", "entity.villager.no",
                    "crate_given", "entity.villager.yes",
                    "key_given", "entity.villager.yes"
            );
            commandConfig = new CommandConfig();
            commandConfig.setName("moderncrates");
            commandConfig.setDescription("ModernCrates command");
            commandConfig.setUsage("/moderncrates");
            commandConfig.setPermission("moderncrates.use");
            commandConfig.setAliases(List.of("mc", "crate", "crates"));
            save();
        }
    }

    public String getPrefix() { return prefix; }
    public String getMessage(String key) { return messages.getOrDefault(key, ""); }
    public String getSound(String key) { return sounds.getOrDefault(key, ""); }
    public CommandConfig getCommandConfig() { return commandConfig; }
    public YamlConfiguration getYaml() { return yaml; }

    public static class CommandConfig {
        private String name;
        private String description;
        private String usage;
        private String permission;
        private List<String> aliases;

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
