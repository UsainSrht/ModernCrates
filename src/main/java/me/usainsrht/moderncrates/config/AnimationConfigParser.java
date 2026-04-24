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
        anim.setRewardAmount(yaml.getInt("reward_amount", 1));
        anim.setShowRevealedItemsFor(yaml.getInt("show_revealed_items_for", 20));

        // GUI
        anim.setGuiTitle(yaml.getString("gui_title", "<red><bold><crate>"));
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

        // Pointers - support flat (down_pointer:) and nested (pointers.down:)
        PointerConfig downPointer = parsePointerConfig(yaml.getConfigurationSection("down_pointer"));
        if (downPointer == null) {
            downPointer = parsePointerConfig(yaml.getConfigurationSection("pointers.down"));
        }
        anim.setDownPointer(downPointer);

        PointerConfig upPointer = parsePointerConfig(yaml.getConfigurationSection("up_pointer"));
        if (upPointer == null) {
            upPointer = parsePointerConfig(yaml.getConfigurationSection("pointers.up"));
        }
        anim.setUpPointer(upPointer);

        // Sounds - support flat (tick_sound:) and nested (sounds.tick:), string or list
        anim.setTickSounds(parseSoundList(yaml, "tick_sound", "sounds.tick"));
        anim.setRewardSounds(parseSoundList(yaml, "reward_sound", "sounds.reward"));

        // Click-type sounds - also support nested
        anim.setShuffleSounds(parseSoundList(yaml, "shuffle_sound", "sounds.shuffle"));
        anim.setHideSounds(parseSoundList(yaml, "hide_sound", "sounds.hide"));
        anim.setRevealSounds(parseSoundList(yaml, "reveal_sound", "sounds.reveal"));

        // Shuffling title - support both key names
        if (anim.getGuiTitleShuffling() == null) {
            anim.setGuiTitleShuffling(yaml.getString("shuffling_title"));
        }

        // Scratchcard-type fields
        anim.setMatchRequired(yaml.getInt("match_required", anim.getRewardAmount()));

        // Slot-type fields
        anim.setSlotColumns(parseSlotColumns(yaml.getConfigurationSection("slot_columns")));
        anim.setRewardWinnerColumns(yaml.getStringList("reward_winner_columns"));
        anim.setRewardWinnerIndex(yaml.getInt("reward_winner_index", 1));
        anim.setColumnStopDelayTicks(yaml.getInt("column_stop_delay_ticks", 15));
        anim.setMatchChance(yaml.getDouble("match_chance", 25.0));

        // Shared win/lose fields
        anim.setWinTitle(yaml.getString("win_title"));
        anim.setLoseTitle(yaml.getString("lose_title"));
        anim.setWinSounds(parseSoundList(yaml, "win_sound", "sounds.win"));
        anim.setLoseSounds(parseSoundList(yaml, "lose_sound", "sounds.lose"));

        // ItemRise-type fields
        anim.setRiseHeight(yaml.getDouble("rise_height", 2.5));
        anim.setRiseTicks(yaml.getInt("rise_ticks", 100));
        anim.setCycleTicks(yaml.getInt("cycle_ticks", 5));
        anim.setParticleType(yaml.getString("particle_type", "FLAME"));
        anim.setParticleCount(yaml.getInt("particle_count", 3));
        anim.setParticleSpiralRadius(yaml.getDouble("particle_spiral_radius", 0.5));
        anim.setParticleSpiralSpeed(yaml.getDouble("particle_spiral_speed", 0.3));
        anim.setBlockOpenDelayTicks(yaml.getInt("block_open_delay_ticks", 5));
        anim.setRiseStartDelayTicks(yaml.getInt("rise_start_delay_ticks", 10));
        anim.setSettleDisplayTicks(yaml.getInt("settle_display_ticks", 60));
        anim.setRiseSounds(parseSoundList(yaml, "rise_sound", "sounds.rise"));
        anim.setSettleSounds(parseSoundList(yaml, "settle_sound", "sounds.settle"));

        // BlockDismantle-type fields
        anim.setDismantleBlockType(yaml.getString("dismantle_block_type", "BARREL"));
        anim.setDismantleFallDurationTicks(yaml.getInt("dismantle_fall_duration_ticks", 30));
        anim.setDismantleTopRiseDurationTicks(yaml.getInt("dismantle_top_rise_duration_ticks", 20));
        anim.setDismantleTopLaunchHeight(yaml.getDouble("dismantle_top_launch_height", 3.0));
        anim.setDismantleTopHorizontalRange(yaml.getDouble("dismantle_top_horizontal_range", 1.5));
        anim.setDismantleDisplayDurationTicks(yaml.getInt("dismantle_display_duration_ticks", 80));
        anim.setDismantleRewardCount(yaml.getInt("dismantle_reward_count", 3));
        anim.setDismantleOpenSounds(parseSoundList(yaml, "dismantle_open_sound", "sounds.dismantle_open"));
        anim.setDismantleRewardSounds(parseSoundList(yaml, "dismantle_reward_sound", "sounds.dismantle_reward"));
        anim.setDismantleSettleSounds(parseSoundList(yaml, "dismantle_settle_sound", "sounds.dismantle_settle"));

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

    /**
     * Reads a sound config that might be a single string or a list,
     * from a primary key or a fallback nested key.
     */
    private List<String> parseSoundList(YamlConfiguration yaml, String primaryKey, String nestedKey) {
        // Try primary key first (e.g. "tick_sound")
        if (yaml.isList(primaryKey)) {
            return yaml.getStringList(primaryKey);
        }
        if (yaml.isString(primaryKey)) {
            String val = yaml.getString(primaryKey);
            return val != null && !val.isEmpty() ? List.of(val) : List.of();
        }
        // Try nested key (e.g. "sounds.tick")
        if (nestedKey != null) {
            if (yaml.isList(nestedKey)) {
                return yaml.getStringList(nestedKey);
            }
            if (yaml.isString(nestedKey)) {
                String val = yaml.getString(nestedKey);
                return val != null && !val.isEmpty() ? List.of(val) : List.of();
            }
        }
        return List.of();
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

    private Map<String, List<Integer>> parseSlotColumns(ConfigurationSection section) {
        if (section == null) return null;
        Map<String, List<Integer>> columns = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            columns.put(key, section.getIntegerList(key));
        }
        return columns;
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
