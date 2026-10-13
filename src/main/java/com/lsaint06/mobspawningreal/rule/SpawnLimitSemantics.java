package com.lsaint06.mobspawningreal.rule;

public final class SpawnLimitSemantics {
    private SpawnLimitSemantics() {}

    public static boolean allowed(Integer limit, int currentCount) {
        return limit == null || currentCount < limit;
    }

    public static boolean baseSpawnPasses(Double rateMultiplier, double random01) {
        if (rateMultiplier == null || rateMultiplier >= 1.0) return true;
        if (rateMultiplier <= 0.0) return false;
        return random01 < rateMultiplier;
    }

    public static int extraCount(SpawnActions actions, double random01) {
        int explicitMin = actions.extraSpawnMin == null ? 0 : Math.max(0, actions.extraSpawnMin);
        int explicitMax = actions.extraSpawnMax == null ? explicitMin : Math.max(explicitMin, actions.extraSpawnMax);
        int explicit = inclusiveRandom(explicitMin, explicitMax, random01);

        double multiplier = actions.rateMultiplier == null ? 1.0 : Math.max(0.0, actions.rateMultiplier);
        if (multiplier <= 1.0) return explicit;

        double extras = multiplier - 1.0;
        int guaranteed = (int)Math.floor(extras);
        double fractional = extras - guaranteed;
        int rateExtra = guaranteed + (random01 < fractional ? 1 : 0);
        return Math.max(explicit, rateExtra);
    }

    private static int inclusiveRandom(int min, int max, double random01) {
        if (max <= min) return min;
        double clamped = Math.max(0.0, Math.min(Math.nextDown(1.0), random01));
        return min + (int)Math.floor(clamped * (max - min + 1));
    }
}
