package com.lsaint06.mobspawningreal.rule;

import java.util.List;

public final class TargetSemantics {
    private TargetSemantics() {}
    public static boolean matches(List<String> ids, String targetId) {
        if (ids == null || ids.isEmpty()) return false;
        return ids.stream().anyMatch(id -> "*".equals(id) || id.equals(targetId));
    }
}
