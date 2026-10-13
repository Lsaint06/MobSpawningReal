package com.lsaint06.mobspawningreal.rule;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RuleResolutionTest {
    @Test void higherPriorityNonNullValueWins() {
        MobRule high = new MobRule(); high.priority = 100; high.id = "high"; high.then.behavior.alwaysHostile = true;
        MobRule low = new MobRule(); low.priority = 1; low.id = "low"; low.then.behavior.alwaysHostile = false;
        assertEquals(Boolean.TRUE, RuleResolution.first(List.of(high, low), r -> r.then.behavior.alwaysHostile));
    }

    @Test void fallsThroughNullValues() {
        MobRule high = new MobRule(); high.priority = 100; high.id = "high";
        MobRule low = new MobRule(); low.priority = 1; low.id = "low"; low.then.outgoingDamage = 9.0;
        assertEquals(9.0, RuleResolution.first(List.of(high, low), r -> r.then.outgoingDamage));
    }
}
