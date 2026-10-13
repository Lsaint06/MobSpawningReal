package com.lsaint06.mobspawningreal.rule;

public final class Actions {
    public SpawnActions spawn = new SpawnActions();
    public ActiveSpawnActions activeSpawn = new ActiveSpawnActions();
    public AttributeActions attributes = new AttributeActions();
    public NameActions name = new NameActions();
    public EquipmentActions equipment = new EquipmentActions();
    public LootActions loot = new LootActions();
    public BehaviorActions behavior = new BehaviorActions();
    public Double outgoingDamage;
}
