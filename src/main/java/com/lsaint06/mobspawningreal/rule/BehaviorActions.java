package com.lsaint06.mobspawningreal.rule;

import java.util.ArrayList;
import java.util.List;

public final class BehaviorActions {
    public Boolean alwaysHostile;
    public Boolean peacefulToPlayers;
    public Boolean peacefulToMobs;
    public List<String> hostileTo = new ArrayList<>();
    public List<String> peacefulTo = new ArrayList<>();
    public Boolean hostileWhenHurt;
    public Boolean alertOthersWhenHurt;
    public Boolean leashable;
    public Boolean burnsInSun;
    public Double knockbackTakenMultiplier;
    public Boolean climb;
    public Boolean breakBlocks;
    public Boolean pillarUp;
    public Boolean plantTrees;
    public Boolean plantFlowers;
    public Boolean cutGrass;
    public Boolean eatPlants;
    public Double healFromPlants;
    public Integer abilityCooldownTicks;
    public Double blockBreakHardnessMax;
    public String pillarBlock = "minecraft:cobblestone";
    public String treeBlock = "minecraft:oak_sapling";
    public String flowerBlock = "minecraft:dandelion";
}
