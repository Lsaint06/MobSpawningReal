package com.lsaint06.mobspawningreal.apply;

import com.lsaint06.mobspawningreal.rule.LootActions;
import com.lsaint06.mobspawningreal.rule.LootEntry;
import com.lsaint06.mobspawningreal.rule.MobRule;
import com.lsaint06.mobspawningreal.util.RegistryLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.List;

public final class LootController {
    private LootController() {}

    public static void apply(LivingDropsEvent event, Mob mob, List<MobRule> rules) {
        boolean clear = rules.stream().anyMatch(r -> Boolean.TRUE.equals(r.then.loot.clearVanilla));
        if (clear) event.getDrops().clear();
        if (!(mob.level() instanceof ServerLevel level)) return;

        for (MobRule rule : rules) {
            LootActions actions = rule.then.loot;
            if (actions == null || actions.add == null) continue;
            for (LootEntry entry : actions.add) {
                if (entry == null || entry.item == null) continue;
                double chance = entry.chance == null ? 1.0 : entry.chance;
                if (level.getRandom().nextDouble() >= chance) continue;
                int min = entry.min == null ? 1 : Math.max(0, entry.min);
                int max = entry.max == null ? min : Math.max(min, entry.max);
                int count = min + (max <= min ? 0 : level.getRandom().nextInt(max - min + 1));
                if (count <= 0) continue;
                RegistryLookup.item(entry.item).ifPresent(item -> event.getDrops().add(
                    new ItemEntity(level, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(item, count))
                ));
            }
        }
    }
}
