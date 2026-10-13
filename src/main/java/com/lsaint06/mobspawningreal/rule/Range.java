package com.lsaint06.mobspawningreal.rule;

public final class Range {
    public Double min;
    public Double max;

    public boolean contains(double value) {
        return (min == null || value >= min) && (max == null || value <= max);
    }
}
