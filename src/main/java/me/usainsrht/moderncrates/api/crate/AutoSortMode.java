package me.usainsrht.moderncrates.api.crate;

public enum AutoSortMode {
    DISABLED,
    ASCENDING,
    DESCENDING;

    public static AutoSortMode fromString(String val) {
        if (val == null) return DISABLED;
        return switch (val.trim().toUpperCase()) {
            case "ASCENDING", "ASC", "LOW_TO_HIGH" -> ASCENDING;
            case "DESCENDING", "DESC", "HIGH_TO_LOW" -> DESCENDING;
            case "TRUE" -> ASCENDING;
            default -> DISABLED;
        };
    }

    public boolean isEnabled() {
        return this != DISABLED;
    }
}
