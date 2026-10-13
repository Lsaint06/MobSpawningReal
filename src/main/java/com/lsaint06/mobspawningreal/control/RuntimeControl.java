package com.lsaint06.mobspawningreal.control;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lsaint06.mobspawningreal.MobSpawningReal;
import com.lsaint06.mobspawningreal.rule.MobRule;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RuntimeControl {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_FILE = FMLPaths.CONFIGDIR.get().resolve(MobSpawningReal.MOD_ID).resolve("runtime-state.json");
    private static final RuntimeToggleState STATE = new RuntimeToggleState();

    private RuntimeControl() {}

    public static synchronized void load() {
        STATE.setGloballyEnabled(true);
        STATE.replaceRuleOverrides(Map.of());
        try {
            Files.createDirectories(STATE_FILE.getParent());
            if (!Files.exists(STATE_FILE)) {
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(STATE_FILE)) {
                PersistedState persisted = GSON.fromJson(reader, PersistedState.class);
                if (persisted == null) return;
                STATE.setGloballyEnabled(persisted.enabled);
                STATE.replaceRuleOverrides(persisted.ruleOverrides);
            }
        } catch (Exception e) {
            MobSpawningReal.LOGGER.warn("Could not load Mob Spawning Real runtime state: {}", e.getMessage());
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(STATE_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(STATE_FILE)) {
                GSON.toJson(new PersistedState(STATE.isGloballyEnabled(), new LinkedHashMap<>(STATE.ruleOverrides())), writer);
            }
        } catch (Exception e) {
            MobSpawningReal.LOGGER.warn("Could not save Mob Spawning Real runtime state: {}", e.getMessage());
        }
    }

    public static boolean isGloballyEnabled() {
        return STATE.isGloballyEnabled();
    }

    public static boolean isRuleEnabled(MobRule rule) {
        return rule != null && STATE.isRuleEnabled(rule.id, rule.enabled);
    }

    public static synchronized void setGloballyEnabled(boolean enabled) {
        STATE.setGloballyEnabled(enabled);
        save();
    }

    public static synchronized void setRuleEnabled(String ruleId, boolean enabled) {
        if (enabled) STATE.enableRule(ruleId);
        else STATE.disableRule(ruleId);
        save();
    }

    public static synchronized void clearRuleOverride(String ruleId) {
        STATE.clearRuleOverride(ruleId);
        save();
    }

    public static Boolean ruleOverride(String ruleId) {
        return STATE.ruleOverrides().get(ruleId == null ? "" : ruleId.trim());
    }

    public static Path stateFile() {
        return STATE_FILE;
    }

    private static final class PersistedState {
        boolean enabled = true;
        Map<String, Boolean> ruleOverrides = new LinkedHashMap<>();

        PersistedState() {}

        PersistedState(boolean enabled, Map<String, Boolean> ruleOverrides) {
            this.enabled = enabled;
            this.ruleOverrides = ruleOverrides;
        }
    }
}
