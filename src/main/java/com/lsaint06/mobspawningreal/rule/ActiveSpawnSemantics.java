package com.lsaint06.mobspawningreal.rule;

public final class ActiveSpawnSemantics {
    private ActiveSpawnSemantics() {}
    public static boolean isDue(long tick, int interval) { return interval > 0 && Math.floorMod(tick, interval) == 0; }
    public static int intervalTicks(ActiveSpawnActions a) { return a.intervalTicks == null ? 20 : Math.max(1, a.intervalTicks); }
    public static int attempts(ActiveSpawnActions a) { return a.attempts == null ? 1 : Math.max(1, a.attempts); }
    public static double capRadius(ActiveSpawnActions a) { return a.capRadius == null ? 64.0 : Math.max(1.0, a.capRadius); }
    public static boolean underNearbyCap(ActiveSpawnActions a, int current) { return a.maxNearby == null || current < a.maxNearby; }
    public static boolean enabled(ActiveSpawnActions a) { return a != null && Boolean.TRUE.equals(a.enabled); }
    public static boolean requireLoadedChunk(ActiveSpawnActions a) { return a.requireLoadedChunk == null || a.requireLoadedChunk; }
    public static boolean requireEmptySpace(ActiveSpawnActions a) { return a.requireEmptySpace == null || a.requireEmptySpace; }
    public static boolean requireSolidGround(ActiveSpawnActions a) { return Boolean.TRUE.equals(a.requireSolidGround); }
}
