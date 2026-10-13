package com.lsaint06.mobspawningreal.rule;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AttributeActions {
    public Double maxHealth;
    public Double movementSpeed;
    public Double attackDamage;
    public Double followRange;
    public Double knockbackResistance;
    public Double scale;
    /** Registry id -> base value, for vanilla or modded attributes. */
    public Map<String,Double> custom = new LinkedHashMap<>();
}
