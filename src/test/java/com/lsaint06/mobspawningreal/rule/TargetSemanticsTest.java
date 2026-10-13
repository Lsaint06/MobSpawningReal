package com.lsaint06.mobspawningreal.rule;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TargetSemanticsTest {
    @Test void emptyListMatchesNothingForExplicitTargetActions() {
        assertFalse(TargetSemantics.matches(List.of(), "minecraft:player"));
    }
    @Test void wildcardAndExactIdsMatch() {
        assertTrue(TargetSemantics.matches(List.of("*"), "minecraft:villager"));
        assertTrue(TargetSemantics.matches(List.of("minecraft:villager"), "minecraft:villager"));
        assertFalse(TargetSemantics.matches(List.of("minecraft:zombie"), "minecraft:villager"));
    }
}
