package me.usainsrht.moderncrates.config;

import me.usainsrht.moderncrates.api.crate.*;
import me.usainsrht.moderncrates.api.crate.*;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.reward.RewardDisplay;
import me.usainsrht.moderncrates.api.reward.RewardItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Parser for crate YAML configuration files.
 */
public class CrateConfigParser {

    private final Logger logger;

    public CrateConfigParser(Logger logger) {
        this.logger = logger;
    }

    public Map<String, Crate> loadAll(File cratesDir) {
        Map<String, Crate> crates = new LinkedHashMap<>();
        if (!cratesDir.exists() || !cratesDir.isDirectory()) {
            return crates;
        }

        File[] files = cratesDir.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) return crates;

        for (File file : files) {
            try {
                String id = file.getName().replace(".yml", "").replace(".yaml", "");
                Crate crate = parse(id, file);
                if (crate != null) {
                    crates.put(id, crate);
                    logger.info("Loaded crate: " + id);
                }
            } catch (Exception e) {
                logger.warning("Failed to load crate file: " + file.getName() + " - " + e.getMessage());
            }
        }
        return crates;
    }

    public Crate parse(String id, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Crate crate = new Crate(id);

        crate.setName(yaml.getString("name", id));
        crate.setAnimationId(yaml.getString("animation", "csgo"));

        // Key config
        ConfigurationSection keySection = yaml.getConfigurationSection("key");
        if (keySection != null) {
            CrateKeyConfig keyConfig = new CrateKeyConfig();
            keyConfig.setRequired(keySection.getBoolean("required", false));
            keyConfig.setMaterial(keySection.getString("material", "TRIPWIRE_HOOK"));
            keyConfig.setCount(keySection.getInt("count", 1));
            keyConfig.setName(keySection.getString("name"));
            keyConfig.setLore(keySection.getStringList("lore"));

            // Enchantments
            ConfigurationSection enchSection = keySection.getConfigurationSection("enchantments");
            if (enchSection != null) {
                Map<String, Integer> enchants = new LinkedHashMap<>();
                for (String key : enchSection.getKeys(false)) {
                    enchants.put(key, enchSection.getInt(key));
                }
                keyConfig.setEnchantments(enchants);
            }

            keyConfig.setItemFlags(keySection.getStringList("item_flags"));
            crate.setKeyConfig(keyConfig);
        }

        // Item config
        ConfigurationSection itemSection = yaml.getConfigurationSection("item");
        if (itemSection != null) {
            CrateItemConfig itemConfig = new CrateItemConfig();
            itemConfig.setMaterial(itemSection.getString("material", "CHEST"));
            itemConfig.setName(itemSection.getString("name"));
            itemConfig.setLore(itemSection.getStringList("lore"));
            crate.setItemConfig(itemConfig);
        }

        // Location
        ConfigurationSection locSection = yaml.getConfigurationSection("location");
        if (locSection != null) {
            CrateLocation loc = new CrateLocation();
            loc.setWorldName(locSection.getString("world", "world"));
            loc.setX(locSection.getDouble("x"));
            loc.setY(locSection.getDouble("y"));
            loc.setZ(locSection.getDouble("z"));
            crate.setCrateLocation(loc);
        }

        crate.setBounceBack(yaml.getBoolean("bounce_back", false));

        // Hologram
        ConfigurationSection holoSection = yaml.getConfigurationSection("hologram");
        if (holoSection != null) {
            HologramConfig holoConfig = new HologramConfig();
            holoConfig.setLines(holoSection.getStringList("lines"));
            List<Double> offset = holoSection.getDoubleList("offset");
            if (offset.size() >= 3) {
                holoConfig.setOffsetX(offset.get(0));
                holoConfig.setOffsetY(offset.get(1));
                holoConfig.setOffsetZ(offset.get(2));
            }
            crate.setHologramConfig(holoConfig);
        }

        // Preview
        ConfigurationSection prevSection = yaml.getConfigurationSection("preview");
        if (prevSection != null) {
            crate.setPreviewConfig(parsePreviewConfig(prevSection));
        }

        // Announce
        ConfigurationSection annSection = yaml.getConfigurationSection("announce");
        if (annSection != null) {
            AnnounceConfig ann = new AnnounceConfig();
            ann.setToEveryone(annSection.getBoolean("to_everyone", true));
            ann.setSingle(annSection.getString("single", ""));
            ann.setMultiple(annSection.getString("multiple", ""));
            ann.setMultipleItem(annSection.getString("multiple_item", ""));
            crate.setAnnounceConfig(ann);
        }

        // Rewards
        ConfigurationSection rewardsSection = yaml.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            crate.setRewards(parseRewards(rewardsSection));
        } else {
            crate.setRewards(new LinkedHashMap<>());
        }

        return crate;
    }

    private PreviewConfig parsePreviewConfig(ConfigurationSection section) {
        PreviewConfig config = new PreviewConfig();
        config.setTitle(section.getString("title", "<gold>Preview"));
        config.setRows(section.getInt("rows", 6));

        ConfigurationSection fillSection = section.getConfigurationSection("fill");
        if (fillSection != null) {
            PreviewConfig.GuiItem fill = new PreviewConfig.GuiItem();
            fill.setMaterial(fillSection.getString("material", "BLACK_STAINED_GLASS_PANE"));
            fill.setName(fillSection.getString("name", " "));
            config.setFill(fill);
        }

        config.setCloseButton(parseSlotItem(section.getConfigurationSection("close")));
        config.setNextButton(parseSlotItem(section.getConfigurationSection("next")));
        config.setPreviousButton(parseSlotItem(section.getConfigurationSection("previous")));
        config.setSounds(section.getStringList("sound"));

        // Custom slots
        ConfigurationSection customSection = section.getConfigurationSection("custom_slots");
        if (customSection != null) {
            Map<Integer, PreviewConfig.GuiItem> customSlots = new LinkedHashMap<>();
            for (String key : customSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ConfigurationSection slotSection = customSection.getConfigurationSection(key);
                    if (slotSection != null) {
                        PreviewConfig.GuiItem item = new PreviewConfig.GuiItem();
                        item.setMaterial(slotSection.getString("material"));
                        item.setName(slotSection.getString("name"));
                        item.setLore(slotSection.getStringList("lore"));
                        customSlots.put(slot, item);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            config.setCustomSlots(customSlots);
        }

        return config;
    }

    private PreviewConfig.SlotItem parseSlotItem(ConfigurationSection section) {
        if (section == null) return null;
        PreviewConfig.SlotItem item = new PreviewConfig.SlotItem();
        item.setSlot(section.getInt("slot"));
        item.setMaterial(section.getString("material"));
        item.setName(section.getString("name"));
        item.setLore(section.getStringList("lore"));
        return item;
    }

    private Map<String, Reward> parseRewards(ConfigurationSection section) {
        Map<String, Reward> rewards = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection rewardSection = section.getConfigurationSection(key);
            if (rewardSection == null) continue;

            Reward reward = new Reward(key);
            reward.setChance(rewardSection.getDouble("chance", 1.0));

            // Display
            ConfigurationSection displaySection = rewardSection.getConfigurationSection("display");
            if (displaySection != null) {
                RewardDisplay display = new RewardDisplay();
                display.setMaterial(displaySection.getString("material"));
                display.setName(displaySection.getString("name"));
                display.setLore(displaySection.getStringList("lore"));
                display.setAmount(displaySection.getInt("amount", 1));

                ConfigurationSection enchSection = displaySection.getConfigurationSection("enchantments");
                if (enchSection != null) {
                    Map<String, Integer> enchants = new LinkedHashMap<>();
                    for (String ek : enchSection.getKeys(false)) {
                        enchants.put(ek, enchSection.getInt(ek));
                    }
                    display.setEnchantments(enchants);
                }
                display.setItemFlags(displaySection.getStringList("item_flags"));
                reward.setDisplay(display);
            }

            // Items
            ConfigurationSection itemsSection = rewardSection.getConfigurationSection("items");
            if (itemsSection != null) {
                Map<String, RewardItem> items = new LinkedHashMap<>();
                for (String itemKey : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                    if (itemSection != null) {
                        RewardItem rewardItem = new RewardItem();
                        rewardItem.setMaterial(itemSection.getString("material"));
                        rewardItem.setAmount(itemSection.getInt("amount", 1));
                        rewardItem.setName(itemSection.getString("name"));
                        rewardItem.setLore(itemSection.getStringList("lore"));

                        ConfigurationSection riEnch = itemSection.getConfigurationSection("enchantments");
                        if (riEnch != null) {
                            Map<String, Integer> enchants = new LinkedHashMap<>();
                            for (String ek : riEnch.getKeys(false)) {
                                enchants.put(ek, riEnch.getInt(ek));
                            }
                            rewardItem.setEnchantments(enchants);
                        }
                        rewardItem.setItemFlags(itemSection.getStringList("item_flags"));
                        items.put(itemKey, rewardItem);
                    }
                }
                reward.setItems(items);
            }

            // Commands
            reward.setCommands(rewardSection.getStringList("commands"));

            // Per-reward announce
            reward.setAnnounce(rewardSection.getString("announce"));

            rewards.put(key, reward);
        }
        return rewards;
    }

    public void save(Crate crate, File cratesDir) throws IOException {
        File file = new File(cratesDir, crate.getId() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("name", crate.getName());
        yaml.set("animation", crate.getAnimationId());

        // Key
        CrateKeyConfig key = crate.getKeyConfig();
        if (key != null) {
            yaml.set("key.required", key.isRequired());
            yaml.set("key.material", key.getMaterial());
            yaml.set("key.count", key.getCount());
            if (key.getEnchantments() != null) {
                for (var e : key.getEnchantments().entrySet()) {
                    yaml.set("key.enchantments." + e.getKey(), e.getValue());
                }
            }
            if (key.getItemFlags() != null) yaml.set("key.item_flags", key.getItemFlags());
            if (key.getName() != null) yaml.set("key.name", key.getName());
            if (key.getLore() != null) yaml.set("key.lore", key.getLore());
        }

        // Item
        CrateItemConfig item = crate.getItemConfig();
        if (item != null) {
            yaml.set("item.material", item.getMaterial());
            if (item.getName() != null) yaml.set("item.name", item.getName());
            if (item.getLore() != null) yaml.set("item.lore", item.getLore());
        }

        // Location
        CrateLocation loc = crate.getCrateLocation();
        if (loc != null) {
            yaml.set("location.world", loc.getWorldName());
            yaml.set("location.x", loc.getX());
            yaml.set("location.y", loc.getY());
            yaml.set("location.z", loc.getZ());
        }

        yaml.set("bounce_back", crate.isBounceBack());

        // Hologram
        HologramConfig holo = crate.getHologramConfig();
        if (holo != null) {
            yaml.set("hologram.lines", holo.getLines());
            yaml.set("hologram.offset", List.of(holo.getOffsetX(), holo.getOffsetY(), holo.getOffsetZ()));
        }

        // Preview
        PreviewConfig prev = crate.getPreviewConfig();
        if (prev != null) {
            yaml.set("preview.title", prev.getTitle());
            yaml.set("preview.rows", prev.getRows());
            if (prev.getFill() != null) {
                yaml.set("preview.fill.material", prev.getFill().getMaterial());
                yaml.set("preview.fill.name", prev.getFill().getName());
            }
            if (prev.getCloseButton() != null) {
                yaml.set("preview.close.slot", prev.getCloseButton().getSlot());
                yaml.set("preview.close.material", prev.getCloseButton().getMaterial());
                yaml.set("preview.close.name", prev.getCloseButton().getName());
            }
            if (prev.getNextButton() != null) {
                yaml.set("preview.next.slot", prev.getNextButton().getSlot());
                yaml.set("preview.next.material", prev.getNextButton().getMaterial());
                yaml.set("preview.next.name", prev.getNextButton().getName());
            }
            if (prev.getPreviousButton() != null) {
                yaml.set("preview.previous.slot", prev.getPreviousButton().getSlot());
                yaml.set("preview.previous.material", prev.getPreviousButton().getMaterial());
                yaml.set("preview.previous.name", prev.getPreviousButton().getName());
            }
            if (prev.getSounds() != null) yaml.set("preview.sound", prev.getSounds());
        }

        // Announce
        AnnounceConfig ann = crate.getAnnounceConfig();
        if (ann != null) {
            yaml.set("announce.to_everyone", ann.isToEveryone());
            yaml.set("announce.single", ann.getSingle());
            yaml.set("announce.multiple", ann.getMultiple());
            yaml.set("announce.multiple_item", ann.getMultipleItem());
        }

        // Rewards
        for (var entry : crate.getRewards().entrySet()) {
            String rewardKey = "rewards." + entry.getKey();
            Reward reward = entry.getValue();
            yaml.set(rewardKey + ".chance", reward.getChance());

            if (reward.getDisplay() != null) {
                RewardDisplay d = reward.getDisplay();
                yaml.set(rewardKey + ".display.material", d.getMaterial());
                if (d.getName() != null) yaml.set(rewardKey + ".display.name", d.getName());
                if (d.getLore() != null && !d.getLore().isEmpty()) yaml.set(rewardKey + ".display.lore", d.getLore());
                if (d.getEnchantments() != null) {
                    for (var e : d.getEnchantments().entrySet()) {
                        yaml.set(rewardKey + ".display.enchantments." + e.getKey(), e.getValue());
                    }
                }
            }

            if (reward.getItems() != null) {
                for (var ie : reward.getItems().entrySet()) {
                    String itemKey = rewardKey + ".items." + ie.getKey();
                    RewardItem ri = ie.getValue();
                    if (ri.getMaterial() != null) yaml.set(itemKey + ".material", ri.getMaterial());
                    if (ri.getAmount() > 1) yaml.set(itemKey + ".amount", ri.getAmount());
                    if (ri.getName() != null) yaml.set(itemKey + ".name", ri.getName());
                    if (ri.getLore() != null) yaml.set(itemKey + ".lore", ri.getLore());
                }
            }

            if (reward.getCommands() != null) yaml.set(rewardKey + ".commands", reward.getCommands());
            if (reward.getAnnounce() != null) yaml.set(rewardKey + ".announce", reward.getAnnounce());
        }

        yaml.save(file);
    }
}
