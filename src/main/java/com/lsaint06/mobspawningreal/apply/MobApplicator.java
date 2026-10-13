package com.lsaint06.mobspawningreal.apply;

import com.lsaint06.mobspawningreal.MobSpawningReal;
import com.lsaint06.mobspawningreal.rule.*;
import com.lsaint06.mobspawningreal.util.RegistryLookup;
import com.lsaint06.mobspawningreal.util.RuleTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class MobApplicator {
    private MobApplicator() {}

    public static void apply(Mob mob, List<MobRule> rules) {
        for (MobRule rule : rules) RuleTags.mark(mob, rule);
        applyAttributes(mob, rules);
        applyName(mob, rules);
        applyEquipment(mob, rules);
    }

    private static void applyAttributes(Mob mob, List<MobRule> rules) {
        set(mob, Attributes.MAX_HEALTH, RuleResolution.first(rules, r -> r.then.attributes.maxHealth), true);
        set(mob, Attributes.MOVEMENT_SPEED, RuleResolution.first(rules, r -> r.then.attributes.movementSpeed), false);
        set(mob, Attributes.ATTACK_DAMAGE, RuleResolution.first(rules, r -> r.then.attributes.attackDamage), false);
        set(mob, Attributes.FOLLOW_RANGE, RuleResolution.first(rules, r -> r.then.attributes.followRange), false);
        set(mob, Attributes.KNOCKBACK_RESISTANCE, RuleResolution.first(rules, r -> r.then.attributes.knockbackResistance), false);
        set(mob, Attributes.SCALE, RuleResolution.first(rules, r -> r.then.attributes.scale), false);

        // Lower-priority rules fill gaps; higher-priority rules win for duplicate custom ids.
        java.util.LinkedHashMap<String,Double> custom = new java.util.LinkedHashMap<>();
        for (int i = rules.size() - 1; i >= 0; i--) {
            AttributeActions a = rules.get(i).then.attributes;
            if (a.custom != null) custom.putAll(a.custom);
        }
        for (var entry : custom.entrySet()) setCustom(mob, entry.getKey(), entry.getValue());
    }

    private static void set(Mob mob, net.minecraft.core.Holder<Attribute> attribute, Double value, boolean health) {
        if (value == null) return;
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) return;
        boolean wasFull = health && mob.getHealth() >= mob.getMaxHealth() - 0.01f;
        instance.setBaseValue(value);
        if (health) mob.setHealth(wasFull ? mob.getMaxHealth() : Math.min(mob.getHealth(), mob.getMaxHealth()));
    }

    private static void setCustom(Mob mob, String id, Double value) {
        if (id == null || value == null) return;
        try {
            var registry = mob.registryAccess().lookupOrThrow(Registries.ATTRIBUTE);
            var key = ResourceKey.create(Registries.ATTRIBUTE, Identifier.parse(id));
            registry.get(key).ifPresent(holder -> {
                AttributeInstance instance = mob.getAttribute(holder);
                if (instance != null) instance.setBaseValue(value);
            });
        } catch (Exception e) {
            MobSpawningReal.LOGGER.warn("Could not apply custom attribute {} to {}: {}", id, mob.getName().getString(), e.getMessage());
        }
    }

    private static void applyName(Mob mob, List<MobRule> rules) {
        String text = RuleResolution.first(rules, r -> r.then.name.text);
        Boolean visible = RuleResolution.first(rules, r -> r.then.name.alwaysVisible);
        if (text != null) mob.setCustomName(Component.literal(text));
        if (visible != null) mob.setCustomNameVisible(visible);
    }

    private static void applyEquipment(Mob mob, List<MobRule> rules) {
        equip(mob, EquipmentSlot.MAINHAND, RuleResolution.first(rules, r -> r.then.equipment.mainhand));
        equip(mob, EquipmentSlot.OFFHAND, RuleResolution.first(rules, r -> r.then.equipment.offhand));
        equip(mob, EquipmentSlot.HEAD, RuleResolution.first(rules, r -> r.then.equipment.head));
        equip(mob, EquipmentSlot.CHEST, RuleResolution.first(rules, r -> r.then.equipment.chest));
        equip(mob, EquipmentSlot.LEGS, RuleResolution.first(rules, r -> r.then.equipment.legs));
        equip(mob, EquipmentSlot.FEET, RuleResolution.first(rules, r -> r.then.equipment.feet));
    }

    private static void equip(Mob mob, EquipmentSlot slot, EquipmentEntry entry) {
        if (entry == null || entry.item == null) return;
        double chance = entry.chance == null ? 1.0 : entry.chance;
        if (mob.getRandom().nextDouble() >= chance) return;
        RegistryLookup.item(entry.item).ifPresent(item -> {
            ItemStack stack = new ItemStack(item, entry.count == null ? 1 : Math.max(1, entry.count));
            mob.setItemSlot(slot, stack);
            if (Boolean.TRUE.equals(entry.dropOnDeath)) mob.setGuaranteedDrop(slot);
            else if (Boolean.FALSE.equals(entry.dropOnDeath)) mob.setDropChance(slot, 0.0f);
        });
    }
}
