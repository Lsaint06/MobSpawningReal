package com.lsaint06.mobspawningreal.rule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RuleSemantics {
    private RuleSemantics() {}

    public static List<MobRule> sort(List<MobRule> rules) {
        ArrayList<MobRule> copy = new ArrayList<>(rules);
        copy.sort(Comparator.comparingInt((MobRule r) -> r.priority).reversed().thenComparing(r -> r.id));
        return List.copyOf(copy);
    }

    public static int extraSpawnCount(SpawnActions actions, double random01) {
        if (actions == null) return 0;
        int min = Math.max(0, actions.extraSpawnMin == null ? 0 : actions.extraSpawnMin);
        int max = Math.max(min, actions.extraSpawnMax == null ? min : actions.extraSpawnMax);
        double multiplier = Math.max(1.0, actions.rateMultiplier == null ? 1.0 : actions.rateMultiplier);
        int guaranteed = Math.max(0, (int)Math.floor(multiplier) - 1);
        double fractional = multiplier - Math.floor(multiplier);
        int rateExtra = guaranteed + (random01 < fractional ? 1 : 0);
        if (max == min) return Math.max(min, rateExtra);
        int ranged = min + (int)Math.floor(Math.max(0, Math.min(0.999999999, random01)) * (max - min + 1));
        return Math.max(ranged, rateExtra);
    }

    public static boolean chancePasses(Double chance, double random01) {
        if (chance == null) return true;
        double clamped = Math.max(0, Math.min(1, chance));
        return random01 < clamped;
    }
}
