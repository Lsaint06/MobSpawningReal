package com.lsaint06.mobspawningreal.rule;

import com.lsaint06.mobspawningreal.config.RuleManager;
import com.lsaint06.mobspawningreal.control.RuntimeControl;
import com.lsaint06.mobspawningreal.util.RuleTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

public final class RuntimeRuleEngine {
    private RuntimeRuleEngine() {}

    public static List<MobRule> matchingSpawnRules(Mob mob, ServerLevel level, String spawnReason) {
        List<MobRule> result = new ArrayList<>();
        if (!RuntimeControl.isGloballyEnabled()) return result;
        for (MobRule rule : RuleManager.rules()) {
            if (!RuntimeControl.isRuleEnabled(rule)) continue;
            RuleContext context = RuntimeContexts.forMob(mob, level, spawnReason, rule);
            if (!RuleMatcher.matchesRuntime(rule, context)) continue;
            result.add(rule);
            if (rule.stopProcessing) break;
        }
        return result;
    }

    public static List<MobRule> appliedRules(Mob mob) {
        List<MobRule> result = new ArrayList<>();
        if (!RuntimeControl.isGloballyEnabled()) return result;
        for (MobRule rule : RuleManager.rules()) if (RuntimeControl.isRuleEnabled(rule) && RuleTags.has(mob, rule)) result.add(rule);
        return result;
    }
}
