package com.lsaint06.mobspawningreal.rule;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RuleSemanticsTest {
    @Test void timeRangeSupportsOvernightWindows() {
        TimeRange r = new TimeRange(); r.start = 13000; r.end = 23000;
        assertTrue(r.contains(18000));
        assertFalse(r.contains(6000));
        r.start = 22000; r.end = 2000;
        assertTrue(r.contains(23000));
        assertTrue(r.contains(1000));
        assertFalse(r.contains(12000));
    }

    @Test void higherPriorityRulesSortFirst() {
        MobRule low = new MobRule(); low.id = "low"; low.priority = 1;
        MobRule high = new MobRule(); high.id = "high"; high.priority = 100;
        assertEquals(List.of(high, low), RuleSemantics.sort(List.of(low, high)));
    }

    @Test void fractionalRateMultiplierAddsProbabilisticExtraSpawn() {
        SpawnActions s = new SpawnActions(); s.rateMultiplier = 2.5;
        assertEquals(2, RuleSemantics.extraSpawnCount(s, 0.2));
        assertEquals(1, RuleSemantics.extraSpawnCount(s, 0.8));
    }

    @Test void rangesAreInclusive() {
        IntRange r = new IntRange(); r.min = 4; r.max = 8;
        assertTrue(r.contains(4)); assertTrue(r.contains(8)); assertFalse(r.contains(9));
    }
    @Test void matcherCombinesUniversalSpawnConditions() {
        MobRule r = new MobRule();
        r.when.entities = java.util.List.of("minecraft:zombie");
        r.when.dimensions = java.util.List.of("minecraft:overworld");
        r.when.biomes = java.util.List.of("minecraft:plains");
        r.when.weather = java.util.List.of("clear");
        r.when.y.min = 60; r.when.y.max = 90;
        r.when.light.max = 7;
        RuleContext c = new RuleContext("minecraft:zombie", "natural", "minecraft:overworld", "minecraft:plains", java.util.Set.of(), 2, "clear", 14000, 0, 70, 40, 1, 0.2);
        assertTrue(RuleMatcher.matches(r, c));
    }

}
