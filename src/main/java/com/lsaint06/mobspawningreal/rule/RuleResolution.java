package com.lsaint06.mobspawningreal.rule;

import java.util.List;
import java.util.function.Function;

public final class RuleResolution {
    private RuleResolution() {}

    public static <T> T first(List<MobRule> rules, Function<MobRule,T> getter) {
        for (MobRule rule : rules) {
            T value = getter.apply(rule);
            if (value != null) return value;
        }
        return null;
    }
}
