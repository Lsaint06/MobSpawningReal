package com.lsaint06.mobspawningreal.control;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RuntimeToggleState {
    private boolean globallyEnabled = true;
    private final Map<String, Boolean> ruleOverrides = new LinkedHashMap<>();

    public boolean isGloballyEnabled() {
        return globallyEnabled;
    }

    public void setGloballyEnabled(boolean enabled) {
        this.globallyEnabled = enabled;
    }

    public boolean isRuleEnabled(String ruleId, boolean configuredEnabled) {
        if (!globallyEnabled) return false;
        Boolean override = ruleOverrides.get(normalize(ruleId));
        return override != null ? override : configuredEnabled;
    }

    public void disableRule(String ruleId) {
        String normalized = normalize(ruleId);
        if (!normalized.isEmpty()) ruleOverrides.put(normalized, false);
    }

    public void enableRule(String ruleId) {
        String normalized = normalize(ruleId);
        if (!normalized.isEmpty()) ruleOverrides.put(normalized, true);
    }

    public void clearRuleOverride(String ruleId) {
        ruleOverrides.remove(normalize(ruleId));
    }

    public boolean isRuleDisabled(String ruleId) {
        return Boolean.FALSE.equals(ruleOverrides.get(normalize(ruleId)));
    }

    public Map<String, Boolean> ruleOverrides() {
        return Map.copyOf(ruleOverrides);
    }

    public void replaceRuleOverrides(Map<String, Boolean> overrides) {
        ruleOverrides.clear();
        if (overrides == null) return;
        overrides.forEach((ruleId, enabled) -> {
            String normalized = normalize(ruleId);
            if (!normalized.isEmpty() && enabled != null) ruleOverrides.put(normalized, enabled);
        });
    }

    private static String normalize(String ruleId) {
        return ruleId == null ? "" : ruleId.trim();
    }
}
