package com.lsaint06.mobspawningreal.apply;

import com.lsaint06.mobspawningreal.MobSpawningReal;
import com.lsaint06.mobspawningreal.config.RuleManager;
import com.lsaint06.mobspawningreal.control.RuntimeControl;
import com.lsaint06.mobspawningreal.rule.*;
import com.lsaint06.mobspawningreal.util.RegistryLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class ActiveSpawnController {
    private ActiveSpawnController() {}

    public static void tick(MinecraftServer server) {
        if (!RuntimeControl.isGloballyEnabled()) return;
        long tick = server.getTickCount();
        for (MobRule rule : RuleManager.rules()) {
            ActiveSpawnActions active = rule.then.activeSpawn;
            if (!RuntimeControl.isRuleEnabled(rule) || !ActiveSpawnSemantics.enabled(active)) continue;
            int interval = ActiveSpawnSemantics.intervalTicks(active);
            if (!ActiveSpawnSemantics.isDue(tick, interval)) continue;
            for (ServerLevel level : server.getAllLevels()) tickRule(level, rule, active);
        }
    }

    private static void tickRule(ServerLevel level, MobRule rule, ActiveSpawnActions active) {
        SpawnArea area = active.area;
        if (area == null || !area.complete() || rule.when.entities == null || rule.when.entities.isEmpty()) return;
        List<EntityType<?>> types = new ArrayList<>();
        for (String id : rule.when.entities) RegistryLookup.entityType(id).ifPresent(types::add);
        if (types.isEmpty()) return;

        for (int i = 0; i < ActiveSpawnSemantics.attempts(active); i++) {
            EntityType<?> type = types.get(level.getRandom().nextInt(types.size()));
            BlockPos pos = randomPos(level, area);
            if (ActiveSpawnSemantics.requireLoadedChunk(active) && !level.hasChunkAt(pos)) continue;
            RuleContext ctx = RuntimeContexts.forPosition(type, level, pos, "active", rule);
            if (!RuleMatcher.matchesRuntime(rule, ctx)) continue;
            if (!underCap(level, type, pos, active)) continue;
            if (ActiveSpawnSemantics.requireSolidGround(active) && !level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below())) continue;

            SpawnController.setControlledSpawn(true);
            try {
                var entity = type.spawn(level, pos, EntitySpawnReason.EVENT);
                if (!(entity instanceof Mob mob)) {
                    if (entity != null) entity.discard();
                    continue;
                }
                if (ActiveSpawnSemantics.requireEmptySpace(active) && !level.noCollision(mob)) {
                    mob.discard();
                    continue;
                }
                List<MobRule> matched = RuntimeRuleEngine.matchingSpawnRules(mob, level, "active");
                if (matched.isEmpty()) matched = List.of(rule);
                MobApplicator.apply(mob, matched);
            } catch (Exception e) {
                MobSpawningReal.LOGGER.warn("Active spawn failed for rule {} at {}: {}", rule.id, pos, e.getMessage());
            } finally {
                SpawnController.setControlledSpawn(false);
            }
        }
    }

    private static boolean underCap(ServerLevel level, EntityType<?> type, BlockPos pos, ActiveSpawnActions active) {
        if (active.maxNearby == null) return true;
        double radius = ActiveSpawnSemantics.capRadius(active);
        AABB box = new AABB(pos).inflate(radius);
        int count = level.getEntitiesOfClass(Mob.class, box, mob -> mob.isAlive() && mob.getType() == type).size();
        return ActiveSpawnSemantics.underNearbyCap(active, count);
    }

    private static BlockPos randomPos(ServerLevel level, SpawnArea area) {
        int x = area.lowX() + level.getRandom().nextInt(area.highX() - area.lowX() + 1);
        int y = area.lowY() + level.getRandom().nextInt(area.highY() - area.lowY() + 1);
        int z = area.lowZ() + level.getRandom().nextInt(area.highZ() - area.lowZ() + 1);
        return new BlockPos(x, y, z);
    }
}
