package com.lsaint06.mobspawningreal.rule;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class RuntimeContexts {
    private RuntimeContexts() {}

    public static RuleContext forPosition(EntityType<?> type, ServerLevel level, net.minecraft.core.BlockPos pos, String spawnReason, MobRule rule) {
        String entityId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        String dimension = level.dimension().identifier().toString();
        String biome = level.getBiome(pos).unwrapKey().map(k -> k.identifier().toString()).orElse("");
        Set<String> structures = structureIds(level, pos);
        long dayTime = level.getDefaultClockTime();
        long day = Math.floorDiv(dayTime, 24000L) + 1L;
        String weather = level.isThundering() ? "thunder" : level.isRaining() ? "rain" : "clear";
        int light = level.getMaxLocalRawBrightness(pos);
        double playerDistance = nearestPlayerDistance(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        double radius = rule.when.nearbyMobRadius == null ? (rule.then.activeSpawn == null || rule.then.activeSpawn.capRadius == null ? 64.0 : rule.then.activeSpawn.capRadius) : rule.when.nearbyMobRadius;
        AABB area = new AABB(pos).inflate(radius);
        int nearbyCount = level.getEntitiesOfClass(Mob.class, area, other -> other.getType() == type && other.isAlive()).size();
        return new RuleContext(entityId, spawnReason.toLowerCase(Locale.ROOT), dimension, biome, structures,
            day, weather, dayTime % 24000L, light, pos.getY(), playerDistance, nearbyCount, level.getRandom().nextDouble());
    }

    public static RuleContext forMob(Mob mob, ServerLevel level, String spawnReason, MobRule rule) {
        var pos = mob.blockPosition();
        String entityId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
        String dimension = level.dimension().identifier().toString();
        String biome = level.getBiome(pos).unwrapKey().map(k -> k.identifier().toString()).orElse("");
        Set<String> structures = structureIds(level, pos);
        long dayTime = level.getDefaultClockTime();
        long day = Math.floorDiv(dayTime, 24000L) + 1L;
        String weather = level.isThundering() ? "thunder" : level.isRaining() ? "rain" : "clear";
        int light = level.getMaxLocalRawBrightness(pos);
        double playerDistance = nearestPlayerDistance(level, mob);
        double radius = rule.when.nearbyMobRadius == null ? (rule.then.spawn.capRadius == null ? 64.0 : rule.then.spawn.capRadius) : rule.when.nearbyMobRadius;
        int nearbyCount = level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(radius), other -> other.getType() == mob.getType() && other.isAlive()).size();
        return new RuleContext(entityId, spawnReason.toLowerCase(Locale.ROOT), dimension, biome, structures,
            day, weather, dayTime % 24000L, light, pos.getY(), playerDistance, nearbyCount, level.getRandom().nextDouble());
    }

    private static double nearestPlayerDistance(ServerLevel level, Mob mob) {
        return nearestPlayerDistance(level, mob.getX(), mob.getY(), mob.getZ());
    }

    private static double nearestPlayerDistance(ServerLevel level, double x, double y, double z) {
        double best = Double.POSITIVE_INFINITY;
        for (Player player : level.players()) {
            if (player.isSpectator()) continue;
            double dx = player.getX() - x, dy = player.getY() - y, dz = player.getZ() - z;
            best = Math.min(best, Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
        return best;
    }

    private static Set<String> structureIds(ServerLevel level, net.minecraft.core.BlockPos pos) {
        var present = level.structureManager().getAllStructuresAt(pos).keySet();
        if (present.isEmpty()) return Set.of();
        Set<String> ids = new HashSet<>();
        var lookup = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        lookup.listElements().forEach(holder -> {
            Structure structure = holder.value();
            if (present.contains(structure)) ids.add(holder.key().identifier().toString());
        });
        return Set.copyOf(ids);
    }
}
