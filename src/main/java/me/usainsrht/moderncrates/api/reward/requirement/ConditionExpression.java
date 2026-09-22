package me.usainsrht.moderncrates.api.reward.requirement;

import me.usainsrht.moderncrates.hook.PlaceholderAPIHook;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.function.BiFunction;

/**
 * Represents a parsed AST expression for reward requirements.
 */
public class ConditionExpression {

    private final String rawExpression;
    private final Node rootNode;

    public ConditionExpression(String rawExpression, Node rootNode) {
        this.rawExpression = rawExpression;
        this.rootNode = rootNode;
    }

    public String getRawExpression() {
        return rawExpression;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public boolean evaluate(Player player) {
        return evaluate(player, PlaceholderAPIHook::setPlaceholders);
    }

    public boolean evaluate(Player player, BiFunction<Player, String, String> placeholderResolver) {
        if (rootNode == null) {
            return true;
        }
        return rootNode.evaluate(player, placeholderResolver);
    }

    @Override
    public String toString() {
        return rootNode != null ? rootNode.toString() : rawExpression;
    }

    /**
     * Interface for AST nodes in a condition expression.
     */
    public interface Node {
        boolean evaluate(Player player, BiFunction<Player, String, String> resolver);
    }

    /**
     * Logical AND / OR binary operator node.
     */
    public static class BinaryLogicalNode implements Node {
        public enum LogicalOp { AND, OR }

        private final Node left;
        private final LogicalOp operator;
        private final Node right;

        public BinaryLogicalNode(Node left, LogicalOp operator, Node right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        public Node getLeft() {
            return left;
        }

        public LogicalOp getOperator() {
            return operator;
        }

        public Node getRight() {
            return right;
        }

        @Override
        public boolean evaluate(Player player, BiFunction<Player, String, String> resolver) {
            if (operator == LogicalOp.AND) {
                return left.evaluate(player, resolver) && right.evaluate(player, resolver);
            } else {
                return left.evaluate(player, resolver) || right.evaluate(player, resolver);
            }
        }

        @Override
        public String toString() {
            return "(" + left + " " + operator + " " + right + ")";
        }
    }

    /**
     * Unary NOT operator node.
     */
    public static class UnaryNotNode implements Node {
        private final Node child;

        public UnaryNotNode(Node child) {
            this.child = child;
        }

        public Node getChild() {
            return child;
        }

        @Override
        public boolean evaluate(Player player, BiFunction<Player, String, String> resolver) {
            return !child.evaluate(player, resolver);
        }

        @Override
        public String toString() {
            return "NOT (" + child + ")";
        }
    }

    /**
     * Comparison operator node (<, <=, ==, !=, >, >=).
     */
    public static class ComparisonNode implements Node {
        private final String leftOperand;
        private final ComparisonOperator operator;
        private final String rightOperand;

        public ComparisonNode(String leftOperand, ComparisonOperator operator, String rightOperand) {
            this.leftOperand = leftOperand;
            this.operator = operator;
            this.rightOperand = rightOperand;
        }

        public String getLeftOperand() {
            return leftOperand;
        }

        public ComparisonOperator getOperator() {
            return operator;
        }

        public String getRightOperand() {
            return rightOperand;
        }

        @Override
        public boolean evaluate(Player player, BiFunction<Player, String, String> resolver) {
            String leftResolved = resolveOperand(leftOperand, player, resolver);
            String rightResolved = resolveOperand(rightOperand, player, resolver);
            return operator.evaluate(leftResolved, rightResolved);
        }

        @Override
        public String toString() {
            return leftOperand + " " + operator.getPrimarySymbol() + " " + rightOperand;
        }
    }

    /**
     * Node representing a permission check (e.g. "permission moderncrates.vip").
     */
    public static class PermissionNode implements Node {
        private final String permission;

        public PermissionNode(String permission) {
            this.permission = permission;
        }

        public String getPermission() {
            return permission;
        }

        @Override
        public boolean evaluate(Player player, BiFunction<Player, String, String> resolver) {
            if (player == null) return true;
            String resolvedPerm = resolveOperand(permission, player, resolver);
            return player.hasPermission(resolvedPerm);
        }

        @Override
        public String toString() {
            return "permission " + permission;
        }
    }

    /**
     * Node representing a standalone boolean placeholder or literal.
     */
    public static class BooleanOperandNode implements Node {
        private final String operand;

        public BooleanOperandNode(String operand) {
            this.operand = operand;
        }

        public String getOperand() {
            return operand;
        }

        @Override
        public boolean evaluate(Player player, BiFunction<Player, String, String> resolver) {
            String resolved = resolveOperand(operand, player, resolver).trim().toLowerCase(Locale.ROOT);
            if ("true".equals(resolved) || "yes".equals(resolved) || "1".equals(resolved)) {
                return true;
            }
            if ("false".equals(resolved) || "no".equals(resolved) || "0".equals(resolved) || resolved.isEmpty()) {
                return false;
            }
            try {
                double val = Double.parseDouble(resolved);
                return val != 0;
            } catch (NumberFormatException ignored) {
                return true;
            }
        }

        @Override
        public String toString() {
            return operand;
        }
    }

    private static String resolveOperand(String operand, Player player, BiFunction<Player, String, String> resolver) {
        if (operand == null) return "";
        String trimmed = operand.trim();
        // If quoted with double or single quotes, strip them
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            if (trimmed.length() >= 2) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
        }
        if (resolver != null && trimmed.contains("%")) {
            trimmed = resolver.apply(player, trimmed);
        }
        // Strip quotes again if placeholder resolution resulted in quoted string
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            if (trimmed.length() >= 2) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }
}
