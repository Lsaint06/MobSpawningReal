package com.lsaint06.mobspawningreal.apply;

import com.lsaint06.mobspawningreal.rule.MobRule;
import com.lsaint06.mobspawningreal.rule.RuleResolution;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

public final class LeashController {
    private LeashController() {}

    public static void interact(PlayerInteractEvent.EntityInteract event, Mob mob, List<MobRule> rules) {
        if (!event.getItemStack().is(Items.LEAD)) return;
        Boolean leashable = RuleResolution.first(rules, r -> r.then.behavior.leashable);
        if (leashable == null) return;

        if (!leashable) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        if (!event.getLevel().isClientSide() && !mob.isLeashed()) {
            mob.setLeashedTo(event.getEntity(), true);
            if (!event.getEntity().getAbilities().instabuild) event.getItemStack().shrink(1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
