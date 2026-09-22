package me.usainsrht.moderncrates.api.reward.requirement;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Encapsulates the requirement settings and condition expressions for an individual reward.
 */
public class RewardRequirements {

    public enum LogicalMode {
        AND,
        OR;

        public static LogicalMode fromString(String str) {
            if (str == null) return AND;
            String s = str.trim().toUpperCase(Locale.ROOT);
            if ("OR".equals(s) || "ANY".equals(s)) return OR;
            return AND;
        }
    }

    private LogicalMode mode = LogicalMode.AND;
    private String permission;
    private final List<ConditionExpression> conditions = new ArrayList<>();
    private final List<String> rawConditions = new ArrayList<>();

    public RewardRequirements() {}

    public RewardRequirements(List<String> rawConditionList) {
        if (rawConditionList != null) {
            for (String raw : rawConditionList) {
                addCondition(raw);
            }
        }
    }

    public LogicalMode getMode() {
        return mode;
    }

    public void setMode(LogicalMode mode) {
        this.mode = mode != null ? mode : LogicalMode.AND;
    }

    public @Nullable String getPermission() {
        return permission;
    }

    public void setPermission(@Nullable String permission) {
        this.permission = permission;
    }

    public boolean hasPermission() {
        return permission != null && !permission.isBlank();
    }

    public List<ConditionExpression> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public List<String> getRawConditions() {
        return Collections.unmodifiableList(rawConditions);
    }

    public void addCondition(String rawCondition) {
        if (rawCondition == null || rawCondition.isBlank()) return;
        ConditionExpression expr = ConditionParser.parse(rawCondition);
        conditions.add(expr);
        rawConditions.add(rawCondition);
    }

    public void removeCondition(int index) {
        if (index >= 0 && index < conditions.size()) {
            conditions.remove(index);
            rawConditions.remove(index);
        }
    }

    public void clear() {
        conditions.clear();
        rawConditions.clear();
        permission = null;
    }

    public boolean isEmpty() {
        return !hasPermission() && conditions.isEmpty();
    }

    /**
     * Evaluates whether the given player meets all configured requirements.
     *
     * @param player The player to test against.
     * @return {@code true} if requirements are met, {@code false} if ineligible.
     */
    public boolean evaluate(Player player) {
        // Shorthand permission check
        if (hasPermission() && (player == null || !player.hasPermission(permission))) {
            return false;
        }

        if (conditions.isEmpty()) {
            return true;
        }

        if (mode == LogicalMode.AND) {
            for (ConditionExpression condition : conditions) {
                if (!condition.evaluate(player)) {
                    return false;
                }
            }
            return true;
        } else {
            for (ConditionExpression condition : conditions) {
                if (condition.evaluate(player)) {
                    return true;
                }
            }
            return false;
        }
    }
}
