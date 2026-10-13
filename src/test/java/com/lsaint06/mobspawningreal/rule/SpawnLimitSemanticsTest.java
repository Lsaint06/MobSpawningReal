package com.lsaint06.mobspawningreal.rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpawnLimitSemanticsTest {
    @Test
    void maxLimitBlocksAtOrAboveLimit() {
        assertTrue(SpawnLimitSemantics.allowed(null, 999));
        assertTrue(SpawnLimitSemantics.allowed(5, 4));
        assertFalse(SpawnLimitSemantics.allowed(5, 5));
        assertFalse(SpawnLimitSemantics.allowed(5, 6));
    }

    @Test
    void deterministicExtraSpawnCountUsesConfiguredBounds() {
        SpawnActions actions = new SpawnActions();
        actions.extraSpawnMin = 2;
        actions.extraSpawnMax = 4;
        assertEquals(2, SpawnLimitSemantics.extraCount(actions, 0.0));
        assertEquals(3, SpawnLimitSemantics.extraCount(actions, 0.5));
        assertEquals(4, SpawnLimitSemantics.extraCount(actions, 0.999999));
    }

    @Test
    void rateMultiplierBelowOneCanSuppressSpawn() {
        assertTrue(SpawnLimitSemantics.baseSpawnPasses(0.25, 0.20));
        assertFalse(SpawnLimitSemantics.baseSpawnPasses(0.25, 0.30));
        assertTrue(SpawnLimitSemantics.baseSpawnPasses(2.0, 0.99));
    }
}
