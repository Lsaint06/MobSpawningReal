package com.lsaint06.mobspawningreal.rule;

public final class SpawnArea {
    public Integer minX, maxX, minY, maxY, minZ, maxZ;

    public boolean complete() {
        return minX != null && maxX != null && minY != null && maxY != null && minZ != null && maxZ != null;
    }
    public int lowX() { return Math.min(minX, maxX); }
    public int highX() { return Math.max(minX, maxX); }
    public int lowY() { return Math.min(minY, maxY); }
    public int highY() { return Math.max(minY, maxY); }
    public int lowZ() { return Math.min(minZ, maxZ); }
    public int highZ() { return Math.max(minZ, maxZ); }
    public boolean contains(int x, int y, int z) {
        return complete() && x >= lowX() && x <= highX() && y >= lowY() && y <= highY() && z >= lowZ() && z <= highZ();
    }
}
