package me.usainsrht.moderncrates.api.reward.requirement;

import java.util.Locale;

/**
 * Comparison operators for reward requirement conditions.
 */
public enum ComparisonOperator {

    LESS_THAN("<") {
        @Override
        public boolean evaluate(String left, String right) {
            Double leftNum = parseDouble(left);
            Double rightNum = parseDouble(right);
            if (leftNum != null && rightNum != null) {
                return leftNum < rightNum;
            }
            return left.compareToIgnoreCase(right) < 0;
        }
    },
    LESS_THAN_OR_EQUAL("<=") {
        @Override
        public boolean evaluate(String left, String right) {
            Double leftNum = parseDouble(left);
            Double rightNum = parseDouble(right);
            if (leftNum != null && rightNum != null) {
                return leftNum <= rightNum;
            }
            return left.compareToIgnoreCase(right) <= 0;
        }
    },
    EQUALS("==", "=") {
        @Override
        public boolean evaluate(String left, String right) {
            Double leftNum = parseDouble(left);
            Double rightNum = parseDouble(right);
            if (leftNum != null && rightNum != null) {
                return Math.abs(leftNum - rightNum) < 1e-9;
            }
            Boolean leftBool = parseBoolean(left);
            Boolean rightBool = parseBoolean(right);
            if (leftBool != null && rightBool != null) {
                return leftBool.equals(rightBool);
            }
            return left.equalsIgnoreCase(right);
        }
    },
    NOT_EQUALS("!=") {
        @Override
        public boolean evaluate(String left, String right) {
            return !EQUALS.evaluate(left, right);
        }
    },
    GREATER_THAN(">") {
        @Override
        public boolean evaluate(String left, String right) {
            Double leftNum = parseDouble(left);
            Double rightNum = parseDouble(right);
            if (leftNum != null && rightNum != null) {
                return leftNum > rightNum;
            }
            return left.compareToIgnoreCase(right) > 0;
        }
    },
    GREATER_THAN_OR_EQUAL(">=") {
        @Override
        public boolean evaluate(String left, String right) {
            Double leftNum = parseDouble(left);
            Double rightNum = parseDouble(right);
            if (leftNum != null && rightNum != null) {
                return leftNum >= rightNum;
            }
            return left.compareToIgnoreCase(right) >= 0;
        }
    };

    private final String[] symbols;

    ComparisonOperator(String... symbols) {
        this.symbols = symbols;
    }

    public String[] getSymbols() {
        return symbols;
    }

    public String getPrimarySymbol() {
        return symbols[0];
    }

    public abstract boolean evaluate(String left, String right);

    private static Double parseDouble(String str) {
        if (str == null) return null;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean parseBoolean(String str) {
        if (str == null) return null;
        String trimmed = str.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(trimmed) || "yes".equals(trimmed) || "1".equals(trimmed)) {
            return Boolean.TRUE;
        }
        if ("false".equals(trimmed) || "no".equals(trimmed) || "0".equals(trimmed)) {
            return Boolean.FALSE;
        }
        return null;
    }

    public static ComparisonOperator fromSymbol(String symbol) {
        if (symbol == null) return null;
        String s = symbol.trim();
        for (ComparisonOperator op : values()) {
            for (String sym : op.symbols) {
                if (sym.equalsIgnoreCase(s)) {
                    return op;
                }
            }
        }
        return null;
    }
}
