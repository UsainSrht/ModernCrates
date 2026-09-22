package me.usainsrht.moderncrates.api.reward.requirement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tokenizer and recursive-descent parser for reward requirement condition expressions.
 */
public class ConditionParser {

    public enum TokenType {
        LPAREN,
        RPAREN,
        AND,
        OR,
        NOT,
        COMPARISON_OP,
        PERMISSION,
        OPERAND,
        EOF
    }

    public static class Token {
        private final TokenType type;
        private final String value;
        private final ComparisonOperator compOp;

        public Token(TokenType type, String value) {
            this(type, value, null);
        }

        public Token(TokenType type, String value, ComparisonOperator compOp) {
            this.type = type;
            this.value = value;
            this.compOp = compOp;
        }

        public TokenType getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public ComparisonOperator getCompOp() {
            return compOp;
        }

        @Override
        public String toString() {
            return type + (value != null ? "(" + value + ")" : "");
        }
    }

    /**
     * Parses a condition string into a {@link ConditionExpression}.
     *
     * @param expression The raw expression string.
     * @return Parsed ConditionExpression.
     * @throws IllegalArgumentException If parsing fails due to syntax error.
     */
    public static ConditionExpression parse(String expression) {
        if (expression == null || expression.isBlank()) {
            return new ConditionExpression("", null);
        }

        List<Token> tokens = tokenize(expression);
        ParserInstance parser = new ParserInstance(expression, tokens);
        ConditionExpression.Node root = parser.parseExpression();

        if (parser.peek().getType() != TokenType.EOF) {
            throw new IllegalArgumentException("Unexpected token after expression: " + parser.peek().getValue() + " in: " + expression);
        }

        return new ConditionExpression(expression, root);
    }

    public static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int len = input.length();
        int i = 0;

        while (i < len) {
            char c = input.charAt(i);

            // Skip whitespace
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Parentheses
            if (c == '(') {
                tokens.add(new Token(TokenType.LPAREN, "("));
                i++;
                continue;
            }
            if (c == ')') {
                tokens.add(new Token(TokenType.RPAREN, ")"));
                i++;
                continue;
            }

            // Logical symbolic operators: &&, ||
            if (c == '&') {
                if (i + 1 < len && input.charAt(i + 1) == '&') {
                    tokens.add(new Token(TokenType.AND, "&&"));
                    i += 2;
                    continue;
                }
            }
            if (c == '|') {
                if (i + 1 < len && input.charAt(i + 1) == '|') {
                    tokens.add(new Token(TokenType.OR, "||"));
                    i += 2;
                    continue;
                }
            }

            // Two-character comparisons: <=, >=, ==, !=
            if (c == '<' && i + 1 < len && input.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.COMPARISON_OP, "<=", ComparisonOperator.LESS_THAN_OR_EQUAL));
                i += 2;
                continue;
            }
            if (c == '>' && i + 1 < len && input.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.COMPARISON_OP, ">=", ComparisonOperator.GREATER_THAN_OR_EQUAL));
                i += 2;
                continue;
            }
            if (c == '=' && i + 1 < len && input.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.COMPARISON_OP, "==", ComparisonOperator.EQUALS));
                i += 2;
                continue;
            }
            if (c == '!' && i + 1 < len && input.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.COMPARISON_OP, "!=", ComparisonOperator.NOT_EQUALS));
                i += 2;
                continue;
            }

            // Single-character comparison: <, >, =
            if (c == '<') {
                tokens.add(new Token(TokenType.COMPARISON_OP, "<", ComparisonOperator.LESS_THAN));
                i++;
                continue;
            }
            if (c == '>') {
                tokens.add(new Token(TokenType.COMPARISON_OP, ">", ComparisonOperator.GREATER_THAN));
                i++;
                continue;
            }
            if (c == '=') {
                tokens.add(new Token(TokenType.COMPARISON_OP, "=", ComparisonOperator.EQUALS));
                i++;
                continue;
            }

            // Unary NOT symbol: !
            if (c == '!') {
                tokens.add(new Token(TokenType.NOT, "!"));
                i++;
                continue;
            }

            // Quoted strings ("..." or '...')
            if (c == '"' || c == '\'') {
                char quoteChar = c;
                int start = i + 1;
                i++;
                while (i < len && input.charAt(i) != quoteChar) {
                    i++;
                }
                String content = input.substring(start, i);
                tokens.add(new Token(TokenType.OPERAND, "\"" + content + "\""));
                if (i < len && input.charAt(i) == quoteChar) {
                    i++;
                }
                continue;
            }

            // Placeholders (%...%)
            if (c == '%') {
                int start = i;
                i++;
                while (i < len && input.charAt(i) != '%') {
                    i++;
                }
                if (i < len && input.charAt(i) == '%') {
                    i++; // Include closing %
                }
                String placeholder = input.substring(start, i);
                tokens.add(new Token(TokenType.OPERAND, placeholder));
                continue;
            }

            // Word / Identifier / Number
            int start = i;
            while (i < len) {
                char ch = input.charAt(i);
                if (Character.isWhitespace(ch) || ch == '(' || ch == ')'
                        || ch == '<' || ch == '>' || ch == '=' || ch == '!'
                        || ch == '&' || ch == '|' || ch == '"' || ch == '\'') {
                    break;
                }
                i++;
            }
            String word = input.substring(start, i);
            String upper = word.toUpperCase(Locale.ROOT);

            if ("AND".equals(upper)) {
                tokens.add(new Token(TokenType.AND, word));
            } else if ("OR".equals(upper)) {
                tokens.add(new Token(TokenType.OR, word));
            } else if ("NOT".equals(upper)) {
                tokens.add(new Token(TokenType.NOT, word));
            } else if ("PERMISSION".equals(upper)) {
                tokens.add(new Token(TokenType.PERMISSION, word));
            } else {
                tokens.add(new Token(TokenType.OPERAND, word));
            }
        }

        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    private static class ParserInstance {
        private final String raw;
        private final List<Token> tokens;
        private int pos = 0;

        public ParserInstance(String raw, List<Token> tokens) {
            this.raw = raw;
            this.tokens = tokens;
        }

        public Token peek() {
            if (pos < tokens.size()) {
                return tokens.get(pos);
            }
            return new Token(TokenType.EOF, "");
        }

        public Token advance() {
            Token t = peek();
            pos++;
            return t;
        }

        public Token match(TokenType type) {
            Token t = peek();
            if (t.getType() != type) {
                throw new IllegalArgumentException("Expected " + type + " but found " + t.getType() + " ('" + t.getValue() + "') in: " + raw);
            }
            return advance();
        }

        public ConditionExpression.Node parseExpression() {
            return parseOr();
        }

        private ConditionExpression.Node parseOr() {
            ConditionExpression.Node left = parseAnd();
            while (peek().getType() == TokenType.OR) {
                advance();
                ConditionExpression.Node right = parseAnd();
                left = new ConditionExpression.BinaryLogicalNode(left, ConditionExpression.BinaryLogicalNode.LogicalOp.OR, right);
            }
            return left;
        }

        private ConditionExpression.Node parseAnd() {
            ConditionExpression.Node left = parseUnary();
            while (peek().getType() == TokenType.AND) {
                advance();
                ConditionExpression.Node right = parseUnary();
                left = new ConditionExpression.BinaryLogicalNode(left, ConditionExpression.BinaryLogicalNode.LogicalOp.AND, right);
            }
            return left;
        }

        private ConditionExpression.Node parseUnary() {
            if (peek().getType() == TokenType.NOT) {
                advance();
                ConditionExpression.Node child = parseUnary();
                return new ConditionExpression.UnaryNotNode(child);
            }
            return parsePrimary();
        }

        private ConditionExpression.Node parsePrimary() {
            Token token = peek();

            if (token.getType() == TokenType.LPAREN) {
                advance();
                ConditionExpression.Node expr = parseOr();
                match(TokenType.RPAREN);
                return expr;
            }

            if (token.getType() == TokenType.PERMISSION) {
                advance();
                Token permToken = advance();
                if (permToken.getType() != TokenType.OPERAND) {
                    throw new IllegalArgumentException("Expected permission node after 'permission' keyword in: " + raw);
                }
                return new ConditionExpression.PermissionNode(permToken.getValue());
            }

            if (token.getType() == TokenType.OPERAND) {
                advance();
                // Check if next token is a comparison operator
                if (peek().getType() == TokenType.COMPARISON_OP) {
                    Token opToken = advance();
                    Token rightToken = advance();
                    if (rightToken.getType() != TokenType.OPERAND) {
                        throw new IllegalArgumentException("Expected right-hand operand after " + opToken.getValue() + " in: " + raw);
                    }
                    return new ConditionExpression.ComparisonNode(token.getValue(), opToken.getCompOp(), rightToken.getValue());
                }
                // Standalone boolean operand
                return new ConditionExpression.BooleanOperandNode(token.getValue());
            }

            throw new IllegalArgumentException("Unexpected token " + token.getType() + " ('" + token.getValue() + "') in: " + raw);
        }
    }
}
