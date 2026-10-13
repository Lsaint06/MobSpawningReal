package com.lsaint06.mobspawningreal.apply;

import com.lsaint06.mobspawningreal.rule.MobRule;
import com.lsaint06.mobspawningreal.rule.RuleResolution;
import com.lsaint06.mobspawningreal.rule.TargetSemantics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

public final class CombatController {
    private CombatController() {}

    public static void incomingDamage(LivingIncomingDamageEvent event, List<MobRule> victimRules, List<MobRule> attackerRules) {
        Double outgoing = RuleResolution.first(attackerRules, r -> r.then.outgoingDamage);
        if (outgoing != null) event.setAmount(Math.max(0.0f, outgoing.floatValue()));

        if (!(event.getEntity() instanceof Mob victim)) return;
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) return;

        Boolean hostileWhenHurt = RuleResolution.first(victimRules, r -> r.then.behavior.hostileWhenHurt);
        if (Boolean.TRUE.equals(hostileWhenHurt)) victim.setTarget(attacker);

        Boolean alert = RuleResolution.first(victimRules, r -> r.then.behavior.alertOthersWhenHurt);
        if (Boolean.TRUE.equals(alert) && victim.level() instanceof ServerLevel level) {
            double radius = 16.0;
            level.getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(radius),
                mob -> mob != victim && mob.isAlive() && mob.getType() == victim.getType())
                .forEach(mob -> mob.setTarget(attacker));
        }
    }

    public static void changeTarget(LivingChangeTargetEvent event, List<MobRule> rules) {
        if (!(event.getEntity() instanceof Mob)) return;
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null) return;
        Boolean peacefulPlayers = RuleResolution.first(rules, r -> r.then.behavior.peacefulToPlayers);
        Boolean peacefulMobs = RuleResolution.first(rules, r -> r.then.behavior.peacefulToMobs);
        String id = target instanceof Player ? "minecraft:player" : BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        boolean explicitPeace = rules.stream().anyMatch(r -> TargetSemantics.matches(r.then.behavior.peacefulTo, id));
        if ((target instanceof Player && Boolean.TRUE.equals(peacefulPlayers)) ||
            (!(target instanceof Player) && Boolean.TRUE.equals(peacefulMobs)) || explicitPeace) {
            event.setNewAboutToBeSetTarget(null);
        }
    }
}
