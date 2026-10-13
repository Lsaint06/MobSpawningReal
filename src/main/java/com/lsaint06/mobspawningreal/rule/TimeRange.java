package com.lsaint06.mobspawningreal.rule;

public final class TimeRange {
    public Integer start;
    public Integer end;

    public boolean contains(long dayTime) {
        if (start == null && end == null) return true;
        int s = start == null ? 0 : Math.floorMod(start, 24000);
        int e = end == null ? 23999 : Math.floorMod(end, 24000);
        int t = Math.floorMod((int)(dayTime % 24000), 24000);
        return s <= e ? t >= s && t <= e : t >= s || t <= e;
    }
}
