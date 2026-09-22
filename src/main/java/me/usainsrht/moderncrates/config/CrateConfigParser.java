package me.usainsrht.moderncrates.config;

import me.usainsrht.moderncrates.api.crate.*;
import me.usainsrht.moderncrates.api.reward.Reward;
import me.usainsrht.moderncrates.api.reward.RewardDisplay;
import me.usainsrht.moderncrates.api.reward.RewardItem;
import me.usainsrht.itemapi.yamlitem.YamlItem;
import me.usainsrht.yamlmessage.YamlMessage;
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
            keyConfig.setItemStack(YamlItem.parse(keySection));
            crate.setKeyConfig(keyConfig);
        }

        // Item config
        ConfigurationSection itemSection = yaml.getConfigurationSection("item");
        if (itemSection != null) {
            CrateItemConfig itemConfig = new CrateItemConfig();
            itemConfig.setItemStack(YamlItem.parse(itemSection));
            crate.setItemConfig(itemConfig);
        }

        // Locations (list) – with backward compat for old single 'location' section
        List<Map<?, ?>> locationsList = yaml.getMapList("locations");
        if (!locationsList.isEmpty()) {
            List<CrateLocation> locs = new ArrayList<>();
            for (Map<?, ?> map : locationsList) {
                if (map == null) continue;
                CrateLocation loc = new CrateLocation();
                loc.setWorldName(String.valueOf(map.get("world")));
                Object xv = map.get("x"), yv = map.get("y"), zv = map.get("z");
                loc.setX(xv instanceof Number ? ((Number) xv).doubleValue() : 0.0);
                loc.setY(yv instanceof Number ? ((Number) yv).doubleValue() : 0.0);
                loc.setZ(zv instanceof Number ? ((Number) zv).doubleValue() : 0.0);
                locs.add(loc);
            }
            crate.setCrateLocations(locs);
        } else {
            // Backward compat: single 'location' section
            ConfigurationSection locSection = yaml.getConfigurationSection("location");
            if (locSection != null) {
                CrateLocation loc = new CrateLocation();
                loc.setWorldName(locSection.getString("world", "world"));
                loc.setX(locSection.getDouble("x"));
                loc.setY(locSection.getDouble("y"));
                loc.setZ(locSection.getDouble("z"));
                crate.addCrateLocation(loc);
            }
        }

        crate.setBounceBack(yaml.getBoolean("bounce_back", false));

        crate.setAutoShowChanceOnLore(yaml.getBoolean("auto_show_chance_on_lore",
                yaml.getBoolean("auto-show-chance-on-lore", false)));
        List<String> chanceLoreTemplate = yaml.getStringList("chance-lore-template");
        if (chanceLoreTemplate.isEmpty()) {
            chanceLoreTemplate = yaml.getStringList("chance_lore_template");
        }
        if (chanceLoreTemplate.isEmpty()) {
            chanceLoreTemplate = defaultChanceLoreTemplate();
        }
        crate.setChanceLoreTemplate(chanceLoreTemplate);

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
            holoConfig.setScale((float) holoSection.getDouble("scale", 1.0));
            holoConfig.setBillboard(holoSection.getString("billboard", "CENTER"));
            holoConfig.setSeeThrough(holoSection.getBoolean("see_through", true));
            holoConfig.setShadowed(holoSection.getBoolean("shadowed", true));
            holoConfig.setBackgroundColor(holoSection.getInt("background_color", -1));
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
            if (annSection.contains("default")) {
                ann.setDefaultAnnounce(annSection.getBoolean("default", true));
            } else if (annSection.contains("enabled")) {
                ann.setDefaultAnnounce(annSection.getBoolean("enabled", true));
            }
            if (annSection.contains("single")) {
                ann.setSingleMessage(YamlMessage.parse(annSection.get("single")));
            }
            if (annSection.contains("multiple")) {
                ann.setMultipleMessage(YamlMessage.parse(annSection.get("multiple")));
            }
            if (annSection.contains("multiple_item")) {
                ann.setMultipleItemMessage(YamlMessage.parse(annSection.get("multiple_item")));
            }
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
            fill.setItemStack(YamlItem.parse(fillSection));
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
                        item.setItemStack(YamlItem.parse(slotSection));
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
        item.setItemStack(YamlItem.parse(section));
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
                display.setItemStack(YamlItem.parse(displaySection));
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
                        rewardItem.setItemStack(YamlItem.parse(itemSection));
                        items.put(itemKey, rewardItem);
                    }
                }
                reward.setItems(items);
            }

            // Commands
            reward.setCommands(rewardSection.getStringList("commands"));

            // Per-reward announce
            if (rewardSection.contains("announce")) {
                if (rewardSection.isBoolean("announce")) {
                    reward.setAnnounce(rewardSection.getBoolean("announce"));
                } else {
                    Object val = rewardSection.get("announce");
                    if (val instanceof String str && (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false"))) {
                        reward.setAnnounce(Boolean.parseBoolean(str));
                    } else {
                        // Legacy per-reward announce message support
                        reward.setAnnouncementMessage(YamlMessage.parse(val));
                    }
                }
            }

            if (rewardSection.contains("announcement-message")) {
                reward.setAnnouncementMessage(YamlMessage.parse(rewardSection.get("announcement-message")));
            } else if (rewardSection.contains("announcement_message")) {
                reward.setAnnouncementMessage(YamlMessage.parse(rewardSection.get("announcement_message")));
            }

            String requiredPermission = rewardSection.getString("required-permission");
            if (requiredPermission == null) {
                requiredPermission = rewardSection.getString("required_permission");
            }
            reward.setRequiredPermission(requiredPermission);

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
            if (key.getStoredEnchantments() != null) {
                for (var e : key.getStoredEnchantments().entrySet()) {
                    yaml.set("key.stored_enchantments." + e.getKey(), e.getValue());
                }
            }
            if (key.getItemFlags() != null) yaml.set("key.item_flags", key.getItemFlags());
            if (key.getName() != null) yaml.set("key.name", key.getName());
            if (key.getLore() != null) yaml.set("key.lore", key.getLore());
            yaml.set("key.hide-tooltip", key.isHideTooltip());
        }

        // Item
        CrateItemConfig item = crate.getItemConfig();
        if (item != null) {
            yaml.set("item.material", item.getMaterial());
            if (item.getName() != null) yaml.set("item.name", item.getName());
            if (item.getLore() != null) yaml.set("item.lore", item.getLore());
            yaml.set("item.hide-tooltip", item.isHideTooltip());
        }

        // Locations
        List<CrateLocation> locs = crate.getCrateLocations();
        if (!locs.isEmpty()) {
            List<Map<String, Object>> locMapList = new ArrayList<>();
            for (CrateLocation loc : locs) {
                Map<String, Object> locMap = new LinkedHashMap<>();
                locMap.put("world", loc.getWorldName());
                locMap.put("x", loc.getX());
                locMap.put("y", loc.getY());
                locMap.put("z", loc.getZ());
                locMapList.add(locMap);
            }
            yaml.set("locations", locMapList);
        }

        yaml.set("bounce_back", crate.isBounceBack());
        yaml.set("auto_show_chance_on_lore", crate.isAutoShowChanceOnLore());
        if (crate.getChanceLoreTemplate() != null && !crate.getChanceLoreTemplate().isEmpty()) {
            yaml.set("chance-lore-template", crate.getChanceLoreTemplate());
        }

        // Hologram
        HologramConfig holo = crate.getHologramConfig();
        if (holo != null) {
            yaml.set("hologram.lines", holo.getLines());
            yaml.set("hologram.offset", List.of(holo.getOffsetX(), holo.getOffsetY(), holo.getOffsetZ()));
            yaml.set("hologram.scale", (double) holo.getScale());
            yaml.set("hologram.billboard", holo.getBillboard());
            yaml.set("hologram.see_through", holo.isSeeThrough());
            yaml.set("hologram.shadowed", holo.isShadowed());
            yaml.set("hologram.background_color", holo.getBackgroundColor());
        }

        // Preview
        PreviewConfig prev = crate.getPreviewConfig();
        if (prev != null) {
            yaml.set("preview.title", prev.getTitle());
            yaml.set("preview.rows", prev.getRows());
            if (prev.getFill() != null) {
                yaml.set("preview.fill.material", prev.getFill().getMaterial());
                yaml.set("preview.fill.name", prev.getFill().getName());
                yaml.set("preview.fill.hide-tooltip", prev.getFill().isHideTooltip());
            }
            if (prev.getCloseButton() != null) {
                yaml.set("preview.close.slot", prev.getCloseButton().getSlot());
                yaml.set("preview.close.material", prev.getCloseButton().getMaterial());
                yaml.set("preview.close.name", prev.getCloseButton().getName());
                yaml.set("preview.close.hide-tooltip", prev.getCloseButton().isHideTooltip());
            }
            if (prev.getNextButton() != null) {
                yaml.set("preview.next.slot", prev.getNextButton().getSlot());
                yaml.set("preview.next.material", prev.getNextButton().getMaterial());
                yaml.set("preview.next.name", prev.getNextButton().getName());
                yaml.set("preview.next.hide-tooltip", prev.getNextButton().isHideTooltip());
            }
            if (prev.getPreviousButton() != null) {
                yaml.set("preview.previous.slot", prev.getPreviousButton().getSlot());
                yaml.set("preview.previous.material", prev.getPreviousButton().getMaterial());
                yaml.set("preview.previous.name", prev.getPreviousButton().getName());
                yaml.set("preview.previous.hide-tooltip", prev.getPreviousButton().isHideTooltip());
            }
            if (prev.getSounds() != null) yaml.set("preview.sound", prev.getSounds());
        }

        // Announce
        AnnounceConfig ann = crate.getAnnounceConfig();
        if (ann != null) {
            yaml.set("announce.to_everyone", ann.isToEveryone());
            if (!ann.isDefaultAnnounce()) {
                yaml.set("announce.default", false);
            }
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
                yaml.set(rewardKey + ".display.hide-tooltip", d.isHideTooltip());
                yaml.set(rewardKey + ".display.hide-enchantments", d.isHideEnchantments());
                if (d.getEnchantments() != null) {
                    for (var e : d.getEnchantments().entrySet()) {
                        yaml.set(rewardKey + ".display.enchantments." + e.getKey(), e.getValue());
                    }
                }
                if (d.getStoredEnchantments() != null) {
                    for (var e : d.getStoredEnchantments().entrySet()) {
                        yaml.set(rewardKey + ".display.stored_enchantments." + e.getKey(), e.getValue());
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
                    yaml.set(itemKey + ".hide-tooltip", ri.isHideTooltip());
                    yaml.set(itemKey + ".hide-enchantments", ri.isHideEnchantments());
                    if (ri.getEnchantments() != null) {
                        for (var e : ri.getEnchantments().entrySet()) {
                            yaml.set(itemKey + ".enchantments." + e.getKey(), e.getValue());
                        }
                    }
                    if (ri.getStoredEnchantments() != null) {
                        for (var e : ri.getStoredEnchantments().entrySet()) {
                            yaml.set(itemKey + ".stored_enchantments." + e.getKey(), e.getValue());
                        }
                    }
                    if (ri.getItemFlags() != null && !ri.getItemFlags().isEmpty()) {
                        yaml.set(itemKey + ".item_flags", ri.getItemFlags());
                    }
                }
            }

            if (reward.getCommands() != null) yaml.set(rewardKey + ".commands", reward.getCommands());
            if (reward.getAnnounce() != null) {
                yaml.set(rewardKey + ".announce", reward.getAnnounce());
            }
            if (reward.getAnnouncementMessage() != null && !reward.getAnnouncementMessage().isEmpty()) {
                if (reward.getAnnouncementMessageRaw() != null) {
                    yaml.set(rewardKey + ".announcement-message", reward.getAnnouncementMessageRaw());
                } else if (reward.getAnnouncementMessage().chat() != null && !reward.getAnnouncementMessage().chat().isEmpty()) {
                    yaml.set(rewardKey + ".announcement-message", String.join("\n", reward.getAnnouncementMessage().chat()));
                }
            }
            if (reward.getRequiredPermission() != null) {
                yaml.set(rewardKey + ".required-permission", reward.getRequiredPermission());
            }
        }

        yaml.save(file);
    }

    private static List<String> defaultChanceLoreTemplate() {
        return List.of("", " <yellow>%<chance> ", "");
    }
}
