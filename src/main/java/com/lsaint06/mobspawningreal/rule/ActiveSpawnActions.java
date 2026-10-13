package com.lsaint06.mobspawningreal.rule;

public final class ActiveSpawnActions {
    public Boolean enabled;
    public Integer intervalTicks;
    public Integer attempts;
    public SpawnArea area = new SpawnArea();
    public Integer maxNearby;
    public Double capRadius;
    public Boolean requireLoadedChunk;
    public Boolean requireEmptySpace;
    public Boolean requireSolidGround;
}
