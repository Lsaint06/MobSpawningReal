package com.lsaint06.mobspawningreal.apply;

import com.lsaint06.mobspawningreal.rule.MobRule;
import com.lsaint06.mobspawningreal.rule.SpawnActions;
import com.lsaint06.mobspawningreal.rule.SpawnLimitSemantics;
import com.lsaint06.mobspawningreal.util.RuleTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;

import java.util.List;

public final class SpawnController {
    private static final ThreadLocal<Boolean> EXTRA_SPAWN_GUARD = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> CONTROLLED_SPAWN_GUARD = ThreadLocal.withInitial(() -> false);
    private SpawnController() {}

    public static boolean isSpawningExtra() { return EXTRA_SPAWN_GUARD.get(); }
    public static boolean isControlledSpawn() { return CONTROLLED_SPAWN_GUARD.get(); }
    public static void setControlledSpawn(boolean value) { CONTROLLED_SPAWN_GUARD.set(value); }

    public static boolean shouldBlock(Mob mob, ServerLevel level, List<MobRule> rules) {
        for (MobRule rule : rules) {
            SpawnActions a = rule.then.spawn;
            if (Boolean.FALSE.equals(a.allow)) return true;
            if (!SpawnLimitSemantics.baseSpawnPasses(a.rateMultiplier, level.getRandom().nextDouble())) return true;
            double radius = a.capRadius == null ? 64.0 : Math.max(1.0, a.capRadius);
            if (!withinCaps(mob, level, rule, a, radius)) return true;
        }
        return false;
    }

    private static boolean withinCaps(Mob mob, ServerLevel level, MobRule rule, SpawnActions a, double radius) {
        var area = mob.getBoundingBox().inflate(radius);
        if (a.maxPerMob != null) {
            int count = level.getEntitiesOfClass(Mob.class, area, e -> e.isAlive() && e.getType() == mob.getType()).size();
            if (!SpawnLimitSemantics.allowed(a.maxPerMob, count)) return false;
        }
        if (a.maxPerCondition != null) {
            String tag = com.lsaint06.mobspawningreal.util.RuleTags.tag(rule);
            int count = level.getEntitiesOfClass(Mob.class, area, e -> e.isAlive() && e.getPersistentData().getBooleanOr(tag, false)).size();
            if (!SpawnLimitSemantics.allowed(a.maxPerCondition, count)) return false;
        }
        if (a.maxPerPlayer != null) {
            Player nearest = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), radius, false);
            if (nearest != null) {
                var playerArea = nearest.getBoundingBox().inflate(radius);
                int count = level.getEntitiesOfClass(Mob.class, playerArea, e -> e.isAlive() && e.getType() == mob.getType()).size();
                if (!SpawnLimitSemantics.allowed(a.maxPerPlayer, count)) return false;
            }
        }
        return true;
    }

    public static void spawnExtras(Mob source, ServerLevel level, List<MobRule> rules) {
        if (EXTRA_SPAWN_GUARD.get()) return;
        int extras = 0;
        for (MobRule rule : rules) {
            extras = Math.max(extras, SpawnLimitSemantics.extraCount(rule.then.spawn, level.getRandom().nextDouble()));
        }
        if (extras <= 0) return;

        EXTRA_SPAWN_GUARD.set(true);
        try {
            BlockPos origin = source.blockPosition();
            for (int i = 0; i < extras; i++) {
                BlockPos pos = origin.offset(level.getRandom().nextInt(5) - 2, 0, level.getRandom().nextInt(5) - 2);
                Mob extra = (Mob)source.getType().spawn(level, pos, EntitySpawnReason.EVENT);
                if (extra == null) continue;
                for (MobRule rule : rules) RuleTags.mark(extra, rule);
                MobApplicator.apply(extra, rules);
            }
        } finally {
            EXTRA_SPAWN_GUARD.set(false);
        }
    }
}
