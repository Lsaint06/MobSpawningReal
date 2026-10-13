package com.lsaint06.mobspawningreal.rule;

import java.util.List;

public final class RuleMatcher {
    private RuleMatcher() {}

    public static boolean matches(MobRule rule, RuleContext ctx) {
        return rule != null && rule.enabled && matchesRuntime(rule, ctx);
    }

    public static boolean matchesRuntime(MobRule rule, RuleContext ctx) {
        if (rule == null) return false;
        Conditions c = rule.when;
        if (!matchesList(c.entities, ctx.entityId())) return false;
        if (!matchesListIgnoreCase(c.spawnReasons, ctx.spawnReason())) return false;
        if (!matchesList(c.dimensions, ctx.dimension())) return false;
        if (!matchesList(c.biomes, ctx.biome())) return false;
        if (!c.structures.isEmpty() && c.structures.stream().noneMatch(ctx.structures()::contains)) return false;
        if (!c.day.contains(ctx.day())) return false;
        if (!matchesListIgnoreCase(c.weather, ctx.weather())) return false;
        if (!c.time.contains(ctx.timeOfDay())) return false;
        if (!c.light.contains(ctx.light())) return false;
        if (!c.y.contains(ctx.y())) return false;
        if (!c.playerDistance.contains(ctx.nearestPlayerDistance())) return false;
        if (c.nearbyMobCountMin != null && ctx.nearbySameMobCount() < c.nearbyMobCountMin) return false;
        if (c.nearbyMobCountMax != null && ctx.nearbySameMobCount() > c.nearbyMobCountMax) return false;
        return RuleSemantics.chancePasses(c.chance, ctx.random01());
    }

    private static boolean matchesList(List<String> wanted, String value) {
        if (wanted == null || wanted.isEmpty()) return true;
        return wanted.stream().anyMatch(s -> "*".equals(s) || s.equals(value));
    }

    private static boolean matchesListIgnoreCase(List<String> wanted, String value) {
        if (wanted == null || wanted.isEmpty()) return true;
        return wanted.stream().anyMatch(s -> "*".equals(s) || s.equalsIgnoreCase(value));
    }
}
