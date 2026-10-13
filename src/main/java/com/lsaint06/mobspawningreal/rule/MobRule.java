package com.lsaint06.mobspawningreal.rule;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public final class MobRule {
    public String id = "unnamed";
    public boolean enabled = true;
    public int priority = 0;
    public boolean stopProcessing = false;
    public Conditions when = new Conditions();
    public Actions then = new Actions();

    public void normalize() {
        if (id == null || id.isBlank()) id = "unnamed";
        if (when == null) when = new Conditions();
        if (then == null) then = new Actions();
        if (then.spawn == null) then.spawn = new SpawnActions();
        if (then.activeSpawn == null) then.activeSpawn = new ActiveSpawnActions();
        if (then.activeSpawn.area == null) then.activeSpawn.area = new SpawnArea();
        if (then.attributes == null) then.attributes = new AttributeActions();
        if (then.name == null) then.name = new NameActions();
        if (then.equipment == null) then.equipment = new EquipmentActions();
        if (then.loot == null) then.loot = new LootActions();
        if (then.behavior == null) then.behavior = new BehaviorActions();

        if (when.entities == null) when.entities = new ArrayList<>();
        if (when.entityTags == null) when.entityTags = new ArrayList<>();
        if (when.spawnReasons == null) when.spawnReasons = new ArrayList<>();
        if (when.dimensions == null) when.dimensions = new ArrayList<>();
        if (when.biomes == null) when.biomes = new ArrayList<>();
        if (when.biomeTags == null) when.biomeTags = new ArrayList<>();
        if (when.structures == null) when.structures = new ArrayList<>();
        if (when.weather == null) when.weather = new ArrayList<>();
        if (when.day == null) when.day = new IntRange();
        if (when.time == null) when.time = new TimeRange();
        if (when.light == null) when.light = new IntRange();
        if (when.y == null) when.y = new IntRange();
        if (when.playerDistance == null) when.playerDistance = new Range();

        if (then.attributes.custom == null) then.attributes.custom = new LinkedHashMap<>();
        if (then.loot.add == null) then.loot.add = new ArrayList<>();
        if (then.behavior.hostileTo == null) then.behavior.hostileTo = new ArrayList<>();
        if (then.behavior.peacefulTo == null) then.behavior.peacefulTo = new ArrayList<>();
    }
}
