package me.usainsrht.moderncrates;

import me.usainsrht.moderncrates.api.reward.requirement.ConditionExpression;
import me.usainsrht.moderncrates.api.reward.requirement.ConditionParser;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

public class ConditionParserTest {

    private BiFunction<org.bukkit.entity.Player, String, String> createMockResolver(Map<String, String> placeholderMap) {
        return (player, text) -> {
            String result = text;
            for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
            return result;
        };
    }

    @Test
    public void testNumericComparisons() {
        ConditionExpression lt = ConditionParser.parse("5 < 10");
        assertTrue(lt.evaluate(null, (p, s) -> s));

        ConditionExpression lte = ConditionParser.parse("10 <= 10");
        assertTrue(lte.evaluate(null, (p, s) -> s));

        ConditionExpression eq = ConditionParser.parse("25 == 25.0");
        assertTrue(eq.evaluate(null, (p, s) -> s));

        ConditionExpression ne = ConditionParser.parse("15 != 20");
        assertTrue(ne.evaluate(null, (p, s) -> s));

        ConditionExpression gt = ConditionParser.parse("30 > 20");
        assertTrue(gt.evaluate(null, (p, s) -> s));

        ConditionExpression gte = ConditionParser.parse("50 >= 50");
        assertTrue(gte.evaluate(null, (p, s) -> s));

        ConditionExpression falseLt = ConditionParser.parse("10 < 5");
        assertFalse(falseLt.evaluate(null, (p, s) -> s));

        ConditionExpression falseEq = ConditionParser.parse("10 == 20");
        assertFalse(falseEq.evaluate(null, (p, s) -> s));
    }

    @Test
    public void testStringComparisons() {
        ConditionExpression eq1 = ConditionParser.parse("\"vip\" == \"vip\"");
        assertTrue(eq1.evaluate(null, (p, s) -> s));

        ConditionExpression eqCase = ConditionParser.parse("\"VIP\" == \"vip\"");
        assertTrue(eqCase.evaluate(null, (p, s) -> s));

        ConditionExpression ne = ConditionParser.parse("\"vip\" != \"admin\"");
        assertTrue(ne.evaluate(null, (p, s) -> s));

        ConditionExpression unquoted = ConditionParser.parse("vip == vip");
        assertTrue(unquoted.evaluate(null, (p, s) -> s));

        // Boolean equivalence
        ConditionExpression bool1 = ConditionParser.parse("\"true\" == \"yes\"");
        assertTrue(bool1.evaluate(null, (p, s) -> s));

        ConditionExpression bool2 = ConditionParser.parse("\"1\" == \"true\"");
        assertTrue(bool2.evaluate(null, (p, s) -> s));

        ConditionExpression bool3 = ConditionParser.parse("\"0\" == \"false\"");
        assertTrue(bool3.evaluate(null, (p, s) -> s));
    }

    @Test
    public void testLogicalOperatorsAndParentheses() {
        // AND
        ConditionExpression andTrue = ConditionParser.parse("5 < 10 AND 20 > 15");
        assertTrue(andTrue.evaluate(null, (p, s) -> s));

        ConditionExpression andFalse = ConditionParser.parse("5 < 10 AND 10 > 20");
        assertFalse(andFalse.evaluate(null, (p, s) -> s));

        // Symbolic &&
        ConditionExpression andSym = ConditionParser.parse("5 < 10 && 20 > 15");
        assertTrue(andSym.evaluate(null, (p, s) -> s));

        // OR
        ConditionExpression orTrue = ConditionParser.parse("5 > 10 OR 20 > 15");
        assertTrue(orTrue.evaluate(null, (p, s) -> s));

        ConditionExpression orFalse = ConditionParser.parse("5 > 10 OR 10 > 20");
        assertFalse(orFalse.evaluate(null, (p, s) -> s));

        // Symbolic ||
        ConditionExpression orSym = ConditionParser.parse("5 > 10 || 20 > 15");
        assertTrue(orSym.evaluate(null, (p, s) -> s));

        // NOT
        ConditionExpression notTrue = ConditionParser.parse("NOT (5 > 10)");
        assertTrue(notTrue.evaluate(null, (p, s) -> s));

        ConditionExpression notFalse = ConditionParser.parse("!(5 < 10)");
        assertFalse(notFalse.evaluate(null, (p, s) -> s));

        // Complex precedence with parentheses
        ConditionExpression complex1 = ConditionParser.parse("(5 < 10 AND 10 > 20) OR 1 == 1");
        assertTrue(complex1.evaluate(null, (p, s) -> s));

        ConditionExpression complex2 = ConditionParser.parse("5 < 10 AND (10 > 20 OR 1 == 2)");
        assertFalse(complex2.evaluate(null, (p, s) -> s));
    }

    @Test
    public void testPlaceholderEvaluation() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%claims_limit%", "45");
        placeholders.put("%claims_max%", "50");
        placeholders.put("%player_level%", "25");
        placeholders.put("%vault_group%", "vip");
        placeholders.put("%is_banned%", "false");

        var resolver = createMockResolver(placeholders);

        // User scenario: claim limit increase in crate when player's limit is already maxed
        ConditionExpression underLimit = ConditionParser.parse("%claims_limit% < 50");
        assertTrue(underLimit.evaluate(null, resolver));

        // When player limit is 50 (maxed)
        placeholders.put("%claims_limit%", "50");
        assertFalse(underLimit.evaluate(null, resolver));

        // Two placeholders comparison
        ConditionExpression claimsCheck = ConditionParser.parse("%claims_limit% < %claims_max%");
        assertFalse(claimsCheck.evaluate(null, resolver)); // 50 < 50 is false

        placeholders.put("%claims_limit%", "40");
        assertTrue(claimsCheck.evaluate(null, resolver)); // 40 < 50 is true

        // Compound expression with PAPI and logical operators
        ConditionExpression compound = ConditionParser.parse("(%claims_limit% < 50 AND %player_level% >= 20) OR %vault_group% == admin");
        assertTrue(compound.evaluate(null, resolver));

        // Standalone boolean check
        ConditionExpression boolCheck = ConditionParser.parse("!%is_banned%");
        assertTrue(boolCheck.evaluate(null, resolver));
    }
}
