package com.lsaint06.mobspawningreal.rule;

public final class IntRange {
    public Integer min;
    public Integer max;

    public boolean contains(long value) {
        return (min == null || value >= min) && (max == null || value <= max);
    }
}
