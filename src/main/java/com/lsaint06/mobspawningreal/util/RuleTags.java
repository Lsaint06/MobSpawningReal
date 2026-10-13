package com.lsaint06.mobspawningreal.util;

import com.lsaint06.mobspawningreal.rule.MobRule;
import net.minecraft.world.entity.Entity;

public final class RuleTags {
    private static final String PREFIX = "msr_rule_";
    private RuleTags() {}

    public static String tag(MobRule rule) {
        return PREFIX + rule.id.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
    }

    public static void mark(Entity entity, MobRule rule) {
        entity.getPersistentData().putBoolean(tag(rule), true);
    }

    public static boolean has(Entity entity, MobRule rule) {
        return entity.getPersistentData().getBooleanOr(tag(rule), false);
    }

    public static java.util.List<String> markedRuleIds(Entity entity) {
        return entity.getPersistentData().keySet().stream()
            .filter(key -> key.startsWith(PREFIX) && entity.getPersistentData().getBooleanOr(key, false))
            .sorted()
            .toList();
    }
}
