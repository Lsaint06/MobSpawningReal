package com.lsaint06.mobspawningreal.ability;

import com.lsaint06.mobspawningreal.rule.*;
import com.lsaint06.mobspawningreal.util.RegistryLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class AbilityController {
    private AbilityController() {}

    public static void tick(Mob mob, ServerLevel level, List<MobRule> rules) {
        if (rules.isEmpty() || !mob.isAlive()) return;
        BehaviorActions behavior = resolveBehavior(rules);
        int cooldown = BehaviorSemantics.cooldown(behavior);
        if (mob.tickCount % cooldown != 0) {
            enforceContinuous(mob, level, behavior);
            return;
        }

        enforceContinuous(mob, level, behavior);
        if (Boolean.TRUE.equals(behavior.climb)) climb(mob);
        if (Boolean.TRUE.equals(behavior.breakBlocks)) breakBlockTowardTarget(mob, level, behavior);
        if (Boolean.TRUE.equals(behavior.pillarUp)) pillar(mob, level, behavior);
        if (Boolean.TRUE.equals(behavior.plantTrees)) plantNear(mob, level, behavior.treeBlock);
        if (Boolean.TRUE.equals(behavior.plantFlowers)) plantNear(mob, level, behavior.flowerBlock);
        if (Boolean.TRUE.equals(behavior.cutGrass)) cutGrass(mob, level);
        if (Boolean.TRUE.equals(behavior.eatPlants)) eatPlant(mob, level, BehaviorSemantics.plantHeal(behavior));
    }

    public static BehaviorActions resolveBehavior(List<MobRule> rules) {
        BehaviorActions b = new BehaviorActions();
        b.alwaysHostile = RuleResolution.first(rules, r -> r.then.behavior.alwaysHostile);
        b.peacefulToPlayers = RuleResolution.first(rules, r -> r.then.behavior.peacefulToPlayers);
        b.peacefulToMobs = RuleResolution.first(rules, r -> r.then.behavior.peacefulToMobs);
        for (int i = rules.size() - 1; i >= 0; i--) {
            if (rules.get(i).then.behavior.hostileTo != null && !rules.get(i).then.behavior.hostileTo.isEmpty()) b.hostileTo = new java.util.ArrayList<>(rules.get(i).then.behavior.hostileTo);
            if (rules.get(i).then.behavior.peacefulTo != null && !rules.get(i).then.behavior.peacefulTo.isEmpty()) b.peacefulTo = new java.util.ArrayList<>(rules.get(i).then.behavior.peacefulTo);
        }
        b.hostileWhenHurt = RuleResolution.first(rules, r -> r.then.behavior.hostileWhenHurt);
        b.alertOthersWhenHurt = RuleResolution.first(rules, r -> r.then.behavior.alertOthersWhenHurt);
        b.leashable = RuleResolution.first(rules, r -> r.then.behavior.leashable);
        b.burnsInSun = RuleResolution.first(rules, r -> r.then.behavior.burnsInSun);
        b.climb = RuleResolution.first(rules, r -> r.then.behavior.climb);
        b.breakBlocks = RuleResolution.first(rules, r -> r.then.behavior.breakBlocks);
        b.pillarUp = RuleResolution.first(rules, r -> r.then.behavior.pillarUp);
        b.plantTrees = RuleResolution.first(rules, r -> r.then.behavior.plantTrees);
        b.plantFlowers = RuleResolution.first(rules, r -> r.then.behavior.plantFlowers);
        b.cutGrass = RuleResolution.first(rules, r -> r.then.behavior.cutGrass);
        b.eatPlants = RuleResolution.first(rules, r -> r.then.behavior.eatPlants);
        b.healFromPlants = RuleResolution.first(rules, r -> r.then.behavior.healFromPlants);
        b.abilityCooldownTicks = RuleResolution.first(rules, r -> r.then.behavior.abilityCooldownTicks);
        b.blockBreakHardnessMax = RuleResolution.first(rules, r -> r.then.behavior.blockBreakHardnessMax);
        String pillar = RuleResolution.first(rules, r -> r.then.behavior.pillarBlock);
        String tree = RuleResolution.first(rules, r -> r.then.behavior.treeBlock);
        String flower = RuleResolution.first(rules, r -> r.then.behavior.flowerBlock);
        if (pillar != null) b.pillarBlock = pillar;
        if (tree != null) b.treeBlock = tree;
        if (flower != null) b.flowerBlock = flower;
        return b;
    }

    private static void enforceContinuous(Mob mob, ServerLevel level, BehaviorActions b) {
        LivingEntity current = mob.getTarget();
        if (current != null && shouldBePeacefulTo(b, current)) {
            mob.setTarget(null);
            mob.setAggressive(false);
            current = null;
        }

        if ((Boolean.TRUE.equals(b.alwaysHostile) || (b.hostileTo != null && !b.hostileTo.isEmpty())) && current == null) {
            LivingEntity nearest = null;
            double best = Double.MAX_VALUE;
            double range = mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
            if (range <= 0) range = 32.0;
            for (net.minecraft.world.entity.LivingEntity candidate : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, mob.getBoundingBox().inflate(range), e -> e != mob && e.isAlive())) {
                if (candidate instanceof Player player && (player.isSpectator() || player.isCreative())) continue;
                if (!shouldBeHostileTo(b, candidate)) continue;
                double d = candidate.distanceToSqr(mob);
                if (d < best) { nearest = candidate; best = d; }
            }
            if (nearest != null) {
                mob.setTarget(nearest);
                mob.setAggressive(true);
            }
        }

        if (b.burnsInSun != null && level.isBrightOutside() && level.canSeeSky(mob.blockPosition())) {
            if (b.burnsInSun) mob.igniteForSeconds(8.0f);
            else mob.clearFire();
        }
    }

    private static boolean shouldBeHostileTo(BehaviorActions b, LivingEntity target) {
        String id = target instanceof Player ? "minecraft:player" : BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        if (b.peacefulTo != null && TargetSemantics.matches(b.peacefulTo, id)) return false;
        if (target instanceof Player && Boolean.TRUE.equals(b.peacefulToPlayers)) return false;
        if (!(target instanceof Player) && Boolean.TRUE.equals(b.peacefulToMobs)) return false;
        if (b.hostileTo != null && !b.hostileTo.isEmpty()) return TargetSemantics.matches(b.hostileTo, id);
        return Boolean.TRUE.equals(b.alwaysHostile) && target instanceof Player;
    }

    private static boolean shouldBePeacefulTo(BehaviorActions b, LivingEntity target) {
        String id = target instanceof Player ? "minecraft:player" : BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        return (target instanceof Player && Boolean.TRUE.equals(b.peacefulToPlayers)) ||
            (!(target instanceof Player) && Boolean.TRUE.equals(b.peacefulToMobs)) ||
            (b.peacefulTo != null && TargetSemantics.matches(b.peacefulTo, id));
    }

    private static void climb(Mob mob) {
        if (!mob.horizontalCollision) return;
        var movement = mob.getDeltaMovement();
        mob.setDeltaMovement(movement.x, Math.max(0.22, movement.y), movement.z);
        mob.fallDistance = 0;
    }

    private static void breakBlockTowardTarget(Mob mob, ServerLevel level, BehaviorActions b) {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        int dx = Integer.compare(target.getBlockX(), mob.getBlockX());
        int dz = Integer.compare(target.getBlockZ(), mob.getBlockZ());
        BlockPos pos = mob.blockPosition().offset(dx, 0, dz);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) pos = pos.above();
        state = level.getBlockState(pos);
        if (state.isAir()) return;
        float hardness = state.getDestroySpeed(level, pos);
        double max = b.blockBreakHardnessMax == null ? 2.0 : Math.max(0.0, b.blockBreakHardnessMax);
        if (hardness < 0 || hardness > max) return;
        level.destroyBlock(pos, true, mob);
    }

    private static void pillar(Mob mob, ServerLevel level, BehaviorActions b) {
        LivingEntity target = mob.getTarget();
        if (target == null || target.getY() <= mob.getY() + 1.5) return;
        if (mob.onGround()) {
            var movement = mob.getDeltaMovement();
            mob.setDeltaMovement(movement.x, Math.max(0.42, movement.y), movement.z);
            return;
        }
        BlockPos under = mob.blockPosition().below();
        if (!level.getBlockState(under).isAir()) return;
        RegistryLookup.block(b.pillarBlock).ifPresent(block -> {
            BlockState state = block.defaultBlockState();
            if (state.canSurvive(level, under)) level.setBlockAndUpdate(under, state);
        });
    }

    private static void plantNear(Mob mob, ServerLevel level, String blockId) {
        RegistryLookup.block(blockId).ifPresent(block -> {
            BlockPos base = mob.blockPosition();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos pos = base.relative(d);
                if (!level.getBlockState(pos).isAir()) continue;
                BlockState state = block.defaultBlockState();
                if (state.canSurvive(level, pos)) {
                    level.setBlockAndUpdate(pos, state);
                    return;
                }
            }
        });
    }

    private static void cutGrass(Mob mob, ServerLevel level) {
        for (BlockPos pos : BlockPos.betweenClosed(mob.blockPosition().offset(-1, 0, -1), mob.blockPosition().offset(1, 1, 1))) {
            String id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
            if (isEdiblePlantId(id) && !id.endsWith("grass_block")) {
                level.destroyBlock(pos, true, mob);
                return;
            }
        }
    }

    private static void eatPlant(Mob mob, ServerLevel level, double heal) {
        for (BlockPos pos : BlockPos.betweenClosed(mob.blockPosition().offset(-1, 0, -1), mob.blockPosition().offset(1, 1, 1))) {
            String id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
            if (!isEdiblePlantId(id)) continue;
            level.destroyBlock(pos, false, mob);
            if (heal > 0) mob.heal((float)heal);
            return;
        }
    }

    private static boolean isEdiblePlantId(String id) {
        return id.contains("grass") || id.contains("flower") || id.contains("fern") || id.contains("crop") ||
               id.contains("wheat") || id.contains("carrot") || id.contains("potato") || id.contains("beetroot") ||
               id.contains("sapling") || id.contains("leaves");
    }
}
