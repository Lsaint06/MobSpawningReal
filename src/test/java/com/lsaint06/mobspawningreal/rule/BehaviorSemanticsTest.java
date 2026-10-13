package com.lsaint06.mobspawningreal.rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BehaviorSemanticsTest {
    @Test
    void cooldownDefaultsAndClamps() {
        BehaviorActions b = new BehaviorActions();
        assertEquals(20, BehaviorSemantics.cooldown(b));
        b.abilityCooldownTicks = 1;
        assertEquals(1, BehaviorSemantics.cooldown(b));
        b.abilityCooldownTicks = -5;
        assertEquals(1, BehaviorSemantics.cooldown(b));
    }

    @Test
    void plantHealingDefaultsToZero() {
        BehaviorActions b = new BehaviorActions();
        assertEquals(0.0, BehaviorSemantics.plantHeal(b), 0.0001);
        b.healFromPlants = 3.5;
        assertEquals(3.5, BehaviorSemantics.plantHeal(b), 0.0001);
    }
    @Test
    void knockbackMultiplierDefaultsAndClamps() {
        assertEquals(1.0, BehaviorSemantics.knockbackMultiplier(null), 0.0001);
        assertEquals(1.75, BehaviorSemantics.knockbackMultiplier(1.75), 0.0001);
        assertEquals(0.0, BehaviorSemantics.knockbackMultiplier(-2.0), 0.0001);
    }

}
