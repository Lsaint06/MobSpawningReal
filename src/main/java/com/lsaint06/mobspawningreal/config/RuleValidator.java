package com.lsaint06.mobspawningreal.config;

import com.lsaint06.mobspawningreal.rule.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class RuleValidator {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private RuleValidator() {}

    public static List<String> validate(MobRule r) {
        List<String> errors = new ArrayList<>();
        if (r.id == null || r.id.isBlank()) errors.add("id must not be blank");
        validateIds("when.entities", r.when.entities, true, errors);
        validateIds("when.dimensions", r.when.dimensions, true, errors);
        validateIds("when.biomes", r.when.biomes, true, errors);
        validateIds("when.structures", r.when.structures, true, errors);
        validateRange("when.day", r.when.day == null ? null : r.when.day.min, r.when.day == null ? null : r.when.day.max, errors);
        validateRange("when.light", r.when.light == null ? null : r.when.light.min, r.when.light == null ? null : r.when.light.max, errors);
        validateRange("when.y", r.when.y == null ? null : r.when.y.min, r.when.y == null ? null : r.when.y.max, errors);
        if (r.when.chance != null && (r.when.chance < 0 || r.when.chance > 1)) errors.add("when.chance must be 0..1");
        if (r.when.nearbyMobRadius != null && r.when.nearbyMobRadius <= 0) errors.add("nearbyMobRadius must be > 0");
        if (r.when.nearbyMobCountMin != null && r.when.nearbyMobCountMin < 0) errors.add("nearbyMobCountMin must be >= 0");
        if (r.when.nearbyMobCountMax != null && r.when.nearbyMobCountMax < 0) errors.add("nearbyMobCountMax must be >= 0");

        SpawnActions s = r.then.spawn;
        if (s.rateMultiplier != null && s.rateMultiplier < 0) errors.add("then.spawn.rateMultiplier must be >= 0");
        if (s.extraSpawnMin != null && s.extraSpawnMin < 0) errors.add("extraSpawnMin must be >= 0");
        if (s.extraSpawnMax != null && s.extraSpawnMax < 0) errors.add("extraSpawnMax must be >= 0");
        if (s.extraSpawnMin != null && s.extraSpawnMax != null && s.extraSpawnMax < s.extraSpawnMin) errors.add("extraSpawnMax must be >= extraSpawnMin");
        if (s.maxPerMob != null && s.maxPerMob < 0) errors.add("maxPerMob must be >= 0");
        if (s.maxPerPlayer != null && s.maxPerPlayer < 0) errors.add("maxPerPlayer must be >= 0");
        if (s.maxPerCondition != null && s.maxPerCondition < 0) errors.add("maxPerCondition must be >= 0");
        if (s.capRadius != null && s.capRadius <= 0) errors.add("capRadius must be > 0");


        ActiveSpawnActions active = r.then.activeSpawn;
        if (active != null && Boolean.TRUE.equals(active.enabled)) {
            if (active.intervalTicks != null && active.intervalTicks < 1) errors.add("then.activeSpawn.intervalTicks must be >= 1");
            if (active.attempts != null && active.attempts < 1) errors.add("then.activeSpawn.attempts must be >= 1");
            if (active.maxNearby != null && active.maxNearby < 0) errors.add("then.activeSpawn.maxNearby must be >= 0");
            if (active.capRadius != null && active.capRadius <= 0) errors.add("then.activeSpawn.capRadius must be > 0");
            if (active.area == null || !active.area.complete()) errors.add("then.activeSpawn.area must define minX/maxX/minY/maxY/minZ/maxZ");
            if (r.when.entities == null || r.when.entities.isEmpty()) errors.add("active spawning requires at least one concrete when.entities id");
            else if (r.when.entities.stream().anyMatch("*"::equals)) errors.add("active spawning does not support '*' in when.entities; list concrete mob ids");
        }

        AttributeActions a = r.then.attributes;
        if (a.maxHealth != null && a.maxHealth <= 0) errors.add("maxHealth must be > 0");
        if (a.movementSpeed != null && a.movementSpeed < 0) errors.add("movementSpeed must be >= 0");
        if (a.attackDamage != null && a.attackDamage < 0) errors.add("attackDamage must be >= 0");
        if (a.followRange != null && a.followRange < 0) errors.add("followRange must be >= 0");
        if (a.knockbackResistance != null && (a.knockbackResistance < 0 || a.knockbackResistance > 1)) errors.add("knockbackResistance must be 0..1");
        if (a.scale != null && (a.scale <= 0 || a.scale > 16)) errors.add("scale must be > 0 and <= 16");
        if (a.custom != null) for (Map.Entry<String,Double> entry : a.custom.entrySet()) {
            if (!validId(entry.getKey())) errors.add("invalid custom attribute id: " + entry.getKey());
            if (entry.getValue() == null) errors.add("custom attribute value may not be null: " + entry.getKey());
        }

        BehaviorActions b = r.then.behavior;
        if (b.healFromPlants != null && b.healFromPlants < 0) errors.add("healFromPlants must be >= 0");
        if (b.abilityCooldownTicks != null && b.abilityCooldownTicks < 1) errors.add("abilityCooldownTicks must be >= 1");
        if (b.blockBreakHardnessMax != null && b.blockBreakHardnessMax < 0) errors.add("blockBreakHardnessMax must be >= 0");
        validateIds("behavior.hostileTo", b.hostileTo, true, errors);
        validateIds("behavior.peacefulTo", b.peacefulTo, true, errors);
        validateOptionalId("behavior.pillarBlock", b.pillarBlock, errors);
        validateOptionalId("behavior.treeBlock", b.treeBlock, errors);
        validateOptionalId("behavior.flowerBlock", b.flowerBlock, errors);

        validateEquipment(r.then.equipment, errors);
        validateLoot(r.then.loot, errors);
        return errors;
    }

    private static void validateEquipment(EquipmentActions e, List<String> errors) {
        if (e == null) return;
        validateEquipmentEntry("mainhand", e.mainhand, errors);
        validateEquipmentEntry("offhand", e.offhand, errors);
        validateEquipmentEntry("head", e.head, errors);
        validateEquipmentEntry("chest", e.chest, errors);
        validateEquipmentEntry("legs", e.legs, errors);
        validateEquipmentEntry("feet", e.feet, errors);
    }

    private static void validateEquipmentEntry(String slot, EquipmentEntry e, List<String> errors) {
        if (e == null) return;
        if (!validId(e.item)) errors.add("invalid equipment item for " + slot + ": " + e.item);
        if (e.count != null && e.count < 1) errors.add("equipment count must be >= 1 for " + slot);
        if (e.chance != null && (e.chance < 0 || e.chance > 1)) errors.add("equipment chance must be 0..1 for " + slot);
    }

    private static void validateLoot(LootActions loot, List<String> errors) {
        if (loot == null || loot.add == null) return;
        for (LootEntry e : loot.add) {
            if (e == null || !validId(e.item)) { errors.add("loot item must be a registry id such as minecraft:gunpowder"); continue; }
            if (e.chance != null && (e.chance < 0 || e.chance > 1)) errors.add("loot chance must be 0..1");
            if (e.min != null && e.min < 0) errors.add("loot min must be >= 0");
            if (e.max != null && e.max < 0) errors.add("loot max must be >= 0");
            if (e.min != null && e.max != null && e.max < e.min) errors.add("loot max must be >= min");
        }
    }

    private static void validateIds(String field, List<String> values, boolean wildcard, List<String> errors) {
        if (values == null) return;
        for (String value : values) if (!(wildcard && "*".equals(value)) && !validId(value)) errors.add(field + " contains invalid registry id: " + value);
    }
    private static void validateOptionalId(String field, String value, List<String> errors) {
        if (value != null && !value.isBlank() && !validId(value)) errors.add(field + " must be a registry id: " + value);
    }
    private static boolean validId(String value) { return value != null && ID.matcher(value).matches(); }
    private static void validateRange(String field, Integer min, Integer max, List<String> errors) {
        if (min != null && max != null && max < min) errors.add(field + ".max must be >= .min");
    }
}
