package com.lsaint06.mobspawningreal.command;

import com.lsaint06.mobspawningreal.config.RuleManager;
import com.lsaint06.mobspawningreal.control.RuntimeControl;
import com.lsaint06.mobspawningreal.rule.MobRule;
import com.lsaint06.mobspawningreal.rule.RuntimeRuleEngine;
import com.lsaint06.mobspawningreal.util.RuleTags;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.List;

public final class MobControlCommands {
    private MobControlCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mobspawningreal")
            .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
            .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource())))
            .then(Commands.literal("validate").executes(ctx -> validate(ctx.getSource())))
            .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
            .then(Commands.literal("enable").executes(ctx -> setGlobal(ctx.getSource(), true)))
            .then(Commands.literal("disable").executes(ctx -> setGlobal(ctx.getSource(), false)))
            .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
            .then(Commands.literal("rule")
                .then(Commands.literal("enable").then(ruleIdArgument().executes(ctx -> setRule(ctx.getSource(), StringArgumentType.getString(ctx, "id"), true))))
                .then(Commands.literal("disable").then(ruleIdArgument().executes(ctx -> setRule(ctx.getSource(), StringArgumentType.getString(ctx, "id"), false))))
                .then(Commands.literal("clear").then(ruleIdArgument().executes(ctx -> clearRule(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("status").then(ruleIdArgument().executes(ctx -> ruleStatus(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))
            .then(Commands.literal("explain")
                .then(Commands.argument("target", EntityArgument.entity())
                    .executes(ctx -> explain(ctx.getSource(), EntityArgument.getEntity(ctx, "target")))))
        );
        dispatcher.register(Commands.literal("msr").redirect(dispatcher.getRoot().getChild("mobspawningreal")));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> ruleIdArgument() {
        return Commands.argument("id", StringArgumentType.word())
            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(RuleManager.rules().stream().map(rule -> rule.id).toList(), builder));
    }

    private static int reload(CommandSourceStack source) {
        RuleManager.LoadResult result = RuleManager.reload();
        source.sendSuccess(() -> Component.literal("Mob Spawning Real: loaded " + result.loaded() + " rules, " + result.errors().size() + " errors."), true);
        result.errors().stream().limit(10).forEach(error -> source.sendFailure(Component.literal(error)));
        return result.errors().isEmpty() ? 1 : 0;
    }

    private static int validate(CommandSourceStack source) {
        RuleManager.LoadResult result = RuleManager.reload();
        if (result.errors().isEmpty()) source.sendSuccess(() -> Component.literal("All " + result.loaded() + " rules are valid."), false);
        else {
            source.sendFailure(Component.literal(result.errors().size() + " rule validation error(s):"));
            result.errors().stream().limit(20).forEach(error -> source.sendFailure(Component.literal(error)));
        }
        return result.errors().isEmpty() ? 1 : 0;
    }

    private static int list(CommandSourceStack source) {
        List<MobRule> rules = RuleManager.rules();
        source.sendSuccess(() -> Component.literal("Mob Spawning Real rules (" + rules.size() + ") - global " + globalLabel() + ":"), false);
        for (MobRule rule : rules) {
            source.sendSuccess(() -> Component.literal("- [" + rule.priority + "] " + rule.id + " - " + ruleStateLabel(rule)), false);
        }
        return rules.size();
    }

    private static int setGlobal(CommandSourceStack source, boolean enabled) {
        RuntimeControl.setGloballyEnabled(enabled);
        source.sendSuccess(() -> Component.literal("Mob Spawning Real is now " + (enabled ? "ENABLED" : "DISABLED") + ". This state will persist across server restarts."), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Mob Spawning Real status: " + globalLabel()), false);
        source.sendSuccess(() -> Component.literal("Runtime state file: " + RuntimeControl.stateFile()), false);
        return RuntimeControl.isGloballyEnabled() ? 1 : 0;
    }

    private static int setRule(CommandSourceStack source, String ruleId, boolean enabled) {
        MobRule rule = findRule(ruleId);
        if (rule == null) {
            source.sendFailure(Component.literal("Unknown rule id: " + ruleId));
            return 0;
        }
        RuntimeControl.setRuleEnabled(rule.id, enabled);
        source.sendSuccess(() -> Component.literal("Rule '" + rule.id + "' is now " + (enabled ? "ENABLED" : "DISABLED") + " by runtime override. This persists across restarts."), true);
        return 1;
    }

    private static int clearRule(CommandSourceStack source, String ruleId) {
        MobRule rule = findRule(ruleId);
        if (rule == null) {
            source.sendFailure(Component.literal("Unknown rule id: " + ruleId));
            return 0;
        }
        RuntimeControl.clearRuleOverride(rule.id);
        source.sendSuccess(() -> Component.literal("Cleared runtime override for '" + rule.id + "'. It now follows enabled=" + rule.enabled + " from JSON."), true);
        return 1;
    }

    private static int ruleStatus(CommandSourceStack source, String ruleId) {
        MobRule rule = findRule(ruleId);
        if (rule == null) {
            source.sendFailure(Component.literal("Unknown rule id: " + ruleId));
            return 0;
        }
        Boolean override = RuntimeControl.ruleOverride(rule.id);
        String overrideLabel = override == null ? "none" : (override ? "enabled" : "disabled");
        source.sendSuccess(() -> Component.literal("Rule '" + rule.id + "': effective=" + (RuntimeControl.isRuleEnabled(rule) ? "ENABLED" : "DISABLED") + ", JSON enabled=" + rule.enabled + ", runtime override=" + overrideLabel), false);
        return RuntimeControl.isRuleEnabled(rule) ? 1 : 0;
    }

    private static MobRule findRule(String ruleId) {
        if (ruleId == null) return null;
        return RuleManager.rules().stream().filter(rule -> rule.id.equals(ruleId)).findFirst().orElse(null);
    }

    private static String globalLabel() {
        return RuntimeControl.isGloballyEnabled() ? "ENABLED" : "DISABLED";
    }

    private static String ruleStateLabel(MobRule rule) {
        Boolean override = RuntimeControl.ruleOverride(rule.id);
        String source = override == null ? "JSON" : "runtime override";
        return (RuntimeControl.isRuleEnabled(rule) ? "ENABLED" : "DISABLED") + " (" + source + ")";
    }

    private static int explain(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof Mob mob)) {
            source.sendFailure(Component.literal("Target is not a mob."));
            return 0;
        }
        List<MobRule> applied = RuntimeRuleEngine.appliedRules(mob);
        source.sendSuccess(() -> Component.literal("Entity: " + entity.getType().toString()), false);
        source.sendSuccess(() -> Component.literal("Applied rules: " + (applied.isEmpty() ? "none" : applied.stream().map(r -> r.id).toList())), false);
        source.sendSuccess(() -> Component.literal("Rule markers: " + RuleTags.markedRuleIds(entity)), false);
        return applied.size();
    }
}
