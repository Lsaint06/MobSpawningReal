package com.lsaint06.mobspawningreal.rule;

import java.util.ArrayList;
import java.util.List;

public final class Conditions {
    public List<String> entities = new ArrayList<>();
    public List<String> entityTags = new ArrayList<>();
    public List<String> spawnReasons = new ArrayList<>();
    public List<String> dimensions = new ArrayList<>();
    public List<String> biomes = new ArrayList<>();
    public List<String> biomeTags = new ArrayList<>();
    public List<String> structures = new ArrayList<>();
    public IntRange day = new IntRange();
    public List<String> weather = new ArrayList<>();
    public TimeRange time = new TimeRange();
    public IntRange light = new IntRange();
    public IntRange y = new IntRange();
    public Range playerDistance = new Range();
    public Integer nearbyMobCountMin;
    public Integer nearbyMobCountMax;
    public Double nearbyMobRadius;
    public Double chance;
}
