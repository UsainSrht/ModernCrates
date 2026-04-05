package me.usainsrht.moderncrates.config;

import me.usainsrht.moderncrates.api.animation.Animation;
import me.usainsrht.moderncrates.api.animation.GuiItemConfig;
import me.usainsrht.moderncrates.api.animation.PointerConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Parser for animation YAML configuration files.
 */
public class AnimationConfigParser {

    private final Logger logger;

    public AnimationConfigParser(Logger logger) {
        this.logger = logger;
    }

    public Map<String, Animation> loadAll(File animationsDir) {
        Map<String, Animation> animations = new LinkedHashMap<>();
        if (!animationsDir.exists() || !animationsDir.isDirectory()) {
            return animations;
        }

        File[] files = animationsDir.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) return animations;

        for (File file : files) {
            try {
                String id = file.getName().replace(".yml", "").replace(".yaml", "");
                Animation anim = parse(id, file);
                if (anim != null) {
                    animations.put(id, anim);
                    logger.info("Loaded animation: " + id);
                }
            } catch (Exception e) {
                logger.warning("Failed to load animation file: " + file.getName() + " - " + e.getMessage());
            }
        }
        return animations;
    }

    public Animation parse(String id, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Animation anim = new Animation(id);

        anim.setTypeId(yaml.getString("type", "csgo").toLowerCase(Locale.ROOT));

        // CSGO-type fields
        anim.setTotalTicks(yaml.getInt("total_ticks", 45));
        anim.setStayOpenAfterRewardTicks(yaml.getInt("stay_open_after_reward_ticks", 50));
        anim.setStartTickRate(yaml.getInt("start_tick_rate", 1));
        anim.setTickRateModifier(yaml.getInt("tick_rate_modifier", 20));

        // Click-type fields
        anim.setShuffleAmount(yaml.getInt("shuffle_amount", 5));
        anim.setShuffleTicks(yaml.getInt("shuffle_ticks", 10));
        anim.setShuffleSounds(yaml.getStringList("shuffle_sound"));
        anim.setRewardAmount(yaml.getInt("reward_amount", 1));
        anim.setShowRevealedItemsFor(yaml.getInt("show_revealed_items_for", 20));
        anim.setHideSounds(yaml.getStringList("hide_sound"));
        anim.setRevealSounds(yaml.getStringList("reveal_sound"));

        // GUI
        anim.setGuiTitle(yaml.getString("gui_title", "&c&l<crate>"));
        anim.setGuiTitleShuffling(yaml.getString("gui_title_shuffling"));
        anim.setGuiRows(yaml.getInt("gui_rows", 3));
        anim.setGuiType(yaml.getString("gui_type"));
        anim.setGuiFill(parseGuiItemConfig(yaml.getConfigurationSection("gui_fill")));

        // Reward hide item
        anim.setRewardHideItem(parseGuiItemConfig(yaml.getConfigurationSection("reward_hide_item")));

        // Reward slots
        anim.setRewardIndex(yaml.getInt("reward_index", 5));
        anim.setRewardSlots(yaml.getIntegerList("reward_slots"));

        // Filler
        anim.setFillerSlots(yaml.getIntegerList("filler_slots"));
        anim.setFillerItems(parseFillerItems(yaml.getConfigurationSection("filler_items")));

        // End of animation
        anim.setEndOfAnimationItem(parseGuiItemConfig(yaml.getConfigurationSection("end_of_the_animation_item")));
        anim.setEndOfAnimationSlots(yaml.getIntegerList("end_of_the_animation_slots"));

        // Pointers
        anim.setDownPointer(parsePointerConfig(yaml.getConfigurationSection("down_pointer")));
        anim.setUpPointer(parsePointerConfig(yaml.getConfigurationSection("up_pointer")));

        // Sounds
        anim.setTickSounds(yaml.getStringList("tick_sound"));
        anim.setRewardSounds(yaml.getStringList("reward_sound"));

        return anim;
    }

    private GuiItemConfig parseGuiItemConfig(ConfigurationSection section) {
        if (section == null) return null;
        GuiItemConfig config = new GuiItemConfig();
        config.setMaterial(section.getString("material"));
        config.setName(section.getString("name"));
        config.setLore(section.getStringList("lore"));
        if (section.contains("nbt")) {
            config.setNbt(sectionToMap(section.getConfigurationSection("nbt")));
        }
        return config;
    }

    private PointerConfig parsePointerConfig(ConfigurationSection section) {
        if (section == null) return null;
        PointerConfig config = new PointerConfig();
        config.setSlot(section.getInt("slot"));
        config.setMaterial(section.getString("material"));
        config.setName(section.getString("name"));
        config.setLore(section.getStringList("lore"));
        if (section.contains("nbt")) {
            config.setNbt(sectionToMap(section.getConfigurationSection("nbt")));
        }
        return config;
    }

    private Map<String, GuiItemConfig> parseFillerItems(ConfigurationSection section) {
        if (section == null) return null;
        Map<String, GuiItemConfig> items = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            items.put(key, parseGuiItemConfig(section.getConfigurationSection(key)));
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sectionToMap(ConfigurationSection section) {
        if (section == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object val = section.get(key);
            if (val instanceof ConfigurationSection cs) {
                map.put(key, sectionToMap(cs));
            } else {
                map.put(key, val);
            }
        }
        return map;
    }

    public void save(Animation animation, File animationsDir) throws IOException {
        File file = new File(animationsDir, animation.getId() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("type", animation.getTypeId());
        yaml.set("total_ticks", animation.getTotalTicks());
        yaml.set("stay_open_after_reward_ticks", animation.getStayOpenAfterRewardTicks());
        yaml.set("start_tick_rate", animation.getStartTickRate());
        yaml.set("tick_rate_modifier", animation.getTickRateModifier());
        yaml.set("gui_title", animation.getGuiTitle());
        yaml.set("gui_rows", animation.getGuiRows());

        if (animation.getGuiFill() != null) {
            yaml.set("gui_fill.material", animation.getGuiFill().getMaterial());
            yaml.set("gui_fill.name", animation.getGuiFill().getName());
        }

        yaml.set("reward_index", animation.getRewardIndex());
        yaml.set("reward_slots", animation.getRewardSlots());
        yaml.set("filler_slots", animation.getFillerSlots());

        if (animation.getTickSounds() != null) yaml.set("tick_sound", animation.getTickSounds());
        if (animation.getRewardSounds() != null) yaml.set("reward_sound", animation.getRewardSounds());

        yaml.save(file);
    }
}
