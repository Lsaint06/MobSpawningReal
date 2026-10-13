package com.lsaint06.mobspawningreal.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public final class RegistryLookup {
    private RegistryLookup() {}

    public static Optional<Item> item(String id) {
        try { return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)); }
        catch (Exception ignored) { return Optional.empty(); }
    }

    public static Optional<EntityType<?>> entityType(String id) {
        try { return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)); }
        catch (Exception ignored) { return Optional.empty(); }
    }

    public static Optional<Block> block(String id) {
        try { return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)); }
        catch (Exception ignored) { return Optional.empty(); }
    }
}
