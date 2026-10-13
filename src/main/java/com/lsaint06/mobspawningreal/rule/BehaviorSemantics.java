package com.lsaint06.mobspawningreal.rule;

public final class BehaviorSemantics {
    private BehaviorSemantics() {}

    public static int cooldown(BehaviorActions actions) {
        return Math.max(1, actions.abilityCooldownTicks == null ? 20 : actions.abilityCooldownTicks);
    }

    public static double plantHeal(BehaviorActions actions) {
        return Math.max(0.0, actions.healFromPlants == null ? 0.0 : actions.healFromPlants);
    }

    public static double knockbackMultiplier(Double value) {
        return Math.max(0.0, value == null ? 1.0 : value);
    }
}
