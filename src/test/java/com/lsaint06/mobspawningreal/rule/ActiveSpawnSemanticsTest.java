package com.lsaint06.mobspawningreal.rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ActiveSpawnSemanticsTest {
    @Test
    void intervalDueUsesPositiveIntervalAndWorldTickModulo() {
        assertTrue(ActiveSpawnSemantics.isDue(100, 20));
        assertFalse(ActiveSpawnSemantics.isDue(101, 20));
        assertFalse(ActiveSpawnSemantics.isDue(100, 0));
        assertFalse(ActiveSpawnSemantics.isDue(100, -5));
    }

    @Test
    void areaNormalizesReversedCornersAndContainsEdges() {
        SpawnArea area = new SpawnArea();
        area.minX = 10; area.maxX = -10;
        area.minY = 80; area.maxY = 60;
        area.minZ = 5; area.maxZ = -5;

        assertEquals(-10, area.lowX());
        assertEquals(10, area.highX());
        assertEquals(60, area.lowY());
        assertEquals(80, area.highY());
        assertEquals(-5, area.lowZ());
        assertEquals(5, area.highZ());
        assertTrue(area.contains(-10, 60, -5));
        assertTrue(area.contains(10, 80, 5));
        assertFalse(area.contains(11, 70, 0));
    }

    @Test
    void attemptsAndNearbyCapHaveSafeDefaults() {
        ActiveSpawnActions active = new ActiveSpawnActions();
        assertEquals(1, ActiveSpawnSemantics.attempts(active));
        assertEquals(20, ActiveSpawnSemantics.intervalTicks(active));
        assertEquals(64.0, ActiveSpawnSemantics.capRadius(active));
        assertTrue(ActiveSpawnSemantics.underNearbyCap(active, 999));

        active.attempts = 4;
        active.intervalTicks = 40;
        active.maxNearby = 3;
        active.capRadius = 24.0;
        assertEquals(4, ActiveSpawnSemantics.attempts(active));
        assertEquals(40, ActiveSpawnSemantics.intervalTicks(active));
        assertEquals(24.0, ActiveSpawnSemantics.capRadius(active));
        assertTrue(ActiveSpawnSemantics.underNearbyCap(active, 2));
        assertFalse(ActiveSpawnSemantics.underNearbyCap(active, 3));
    }
}
