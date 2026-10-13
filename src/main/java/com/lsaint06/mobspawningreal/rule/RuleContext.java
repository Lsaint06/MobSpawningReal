package com.lsaint06.mobspawningreal.rule;

import java.util.Set;

public record RuleContext(
    String entityId,
    String spawnReason,
    String dimension,
    String biome,
    Set<String> structures,
    long day,
    String weather,
    long timeOfDay,
    int light,
    int y,
    double nearestPlayerDistance,
    int nearbySameMobCount,
    double random01
) {}
