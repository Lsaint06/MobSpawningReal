package com.lsaint06.mobspawningreal.event;

import com.lsaint06.mobspawningreal.ability.AbilityController;
import com.lsaint06.mobspawningreal.apply.*;
import com.lsaint06.mobspawningreal.command.MobControlCommands;
import com.lsaint06.mobspawningreal.control.RuntimeControl;
import com.lsaint06.mobspawningreal.rule.MobRule;
import com.lsaint06.mobspawningreal.rule.RuntimeRuleEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

public final class MobControlEvents {
    private MobControlEvents() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        MobControlCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void finalizeSpawn(FinalizeSpawnEvent event) {
        if (!RuntimeControl.isGloballyEnabled()) return;
        if (SpawnController.isSpawningExtra() || SpawnController.isControlledSpawn()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        List<MobRule> rules = RuntimeRuleEngine.matchingSpawnRules(mob, level, event.getSpawnType().name());
        if (rules.isEmpty()) return;
        if (SpawnController.shouldBlock(mob, level, rules)) {
            event.setSpawnCancelled(true);
            return;
        }
        MobApplicator.apply(mob, rules);
        SpawnController.spawnExtras(mob, level, rules);
    }

    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent event) {
        if (!RuntimeControl.isGloballyEnabled()) return;
        if (SpawnController.isSpawningExtra() || SpawnController.isControlledSpawn()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!RuntimeRuleEngine.appliedRules(mob).isEmpty()) return;
        String reason = mob.getSpawnType() == null ? "unknown" : mob.getSpawnType().name();
        List<MobRule> rules = RuntimeRuleEngine.matchingSpawnRules(mob, level, reason);
        if (!rules.isEmpty()) MobApplicator.apply(mob, rules);
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!RuntimeControl.isGloballyEnabled()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        List<MobRule> rules = RuntimeRuleEngine.appliedRules(mob);
        if (!rules.isEmpty()) LootController.apply(event, mob, rules);
    }

    @SubscribeEvent
    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!RuntimeControl.isGloballyEnabled()) return;
        List<MobRule> victimRules = event.getEntity() instanceof Mob mob ? RuntimeRuleEngine.appliedRules(mob) : List.of();
        Entity source = event.getSource().getEntity();
        List<MobRule> attackerRules = source instanceof Mob mob ? RuntimeRuleEngine.appliedRules(mob) : List.of();
        if (!victimRules.isEmpty() || !attackerRules.isEmpty()) CombatController.incomingDamage(event, victimRules, attackerRules);
    }

    @SubscribeEvent
    public static void knockback(LivingKnockBackEvent event) {
        if (!RuntimeControl.isGloballyEnabled()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        List<MobRule> rules = RuntimeRuleEngine.appliedRules(mob);
        if (rules.isEmpty()) return;
        Double configured = com.lsaint06.mobspawningreal.rule.RuleResolution.first(
                rules, rule -> rule.then.behavior.knockbackTakenMultiplier);
        if (configured == null) return;
        double multiplier = com.lsaint06.mobspawningreal.rule.BehaviorSemantics.knockbackMultiplier(configured);
        event.setStrength((float)(event.getStrength() * multiplier));
    }

    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent event) {
        if (!RuntimeControl.isGloballyEnabled()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        List<MobRule> rules = RuntimeRuleEngine.appliedRules(mob);
        if (!rules.isEmpty()) CombatController.changeTarget(event, rules);
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        if (!RuntimeControl.isGloballyEnabled()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        List<MobRule> rules = RuntimeRuleEngine.appliedRules(mob);
        if (!rules.isEmpty()) AbilityController.tick(mob, level, rules);
    }


    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        com.lsaint06.mobspawningreal.dashboard.DashboardService.tick(event.getServer());
        if (!RuntimeControl.isGloballyEnabled()) return;
        ActiveSpawnController.tick(event.getServer());
    }

    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!RuntimeControl.isGloballyEnabled()) return;
        if (!(event.getTarget() instanceof Mob mob)) return;
        List<MobRule> rules = RuntimeRuleEngine.appliedRules(mob);
        if (!rules.isEmpty()) LeashController.interact(event, mob, rules);
    }

    @SubscribeEvent
    public static void serverStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        com.lsaint06.mobspawningreal.dashboard.DashboardService.reset();
        RuntimeControl.load();
        com.lsaint06.mobspawningreal.config.RuleManager.reload();
    }
    @SubscribeEvent
    public static void serverStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        com.lsaint06.mobspawningreal.dashboard.DashboardService.reset();
    }
    @SubscribeEvent
    public static void playerLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
            com.lsaint06.mobspawningreal.dashboard.DashboardService.remove(player);
    }
}
