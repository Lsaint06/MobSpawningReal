package com.lsaint06.mobspawningreal.control;
import com.lsaint06.mobspawningreal.MobSpawningReal;
import com.lsaint06.mobspawningreal.rule.MobRule;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.Path;
import java.util.LinkedHashMap;

public final class RuntimeControl {
    private static final Path STATE_FILE = FMLPaths.CONFIGDIR.get().resolve(MobSpawningReal.MOD_ID).resolve("runtime-state.json");
    private static final RuntimeStateStore STORE = new RuntimeStateStore(STATE_FILE);
    private static volatile long revision;
    private static volatile String lastError = "";
    private RuntimeControl() {}
    public static synchronized void load() { report(STORE.load()); revision++; }
    public static synchronized RuntimeStateStore.Result save() { return report(STORE.update(STORE.state().enabled(), STORE.state().ruleOverrides())); }
    public static boolean isGloballyEnabled() { return STORE.state().enabled(); }
    public static boolean isRuleEnabled(MobRule rule) { return isGloballyEnabled() && isRuleSelected(rule); }
    public static boolean isRuleSelected(MobRule rule) { return rule != null && STORE.state().ruleEnabled(rule.id, rule.enabled); }
    public static synchronized RuntimeStateStore.Result setGloballyEnabled(boolean enabled) {
        return changed(STORE.update(enabled, STORE.state().ruleOverrides()));
    }
    public static synchronized RuntimeStateStore.Result setRuleEnabled(String id, boolean enabled) {
        var overrides = new LinkedHashMap<>(STORE.state().ruleOverrides()); overrides.put(id.trim(), enabled);
        return changed(STORE.update(isGloballyEnabled(), overrides));
    }
    public static synchronized RuntimeStateStore.Result clearRuleOverride(String id) {
        var overrides = new LinkedHashMap<>(STORE.state().ruleOverrides()); overrides.remove(id.trim());
        return changed(STORE.update(isGloballyEnabled(), overrides));
    }
    private static RuntimeStateStore.Result changed(RuntimeStateStore.Result result) { if (result.success()) revision++; return report(result); }
    private static RuntimeStateStore.Result report(RuntimeStateStore.Result result) {
        lastError = result.success() ? "" : result.message();
        if (!result.success()) MobSpawningReal.LOGGER.warn("{}", result.message());
        return result;
    }
    public static Boolean ruleOverride(String id) { return STORE.state().ruleOverrides().get(id == null ? "" : id.trim()); }
    public static String lastError() { return lastError; }
    public static long revision() { return revision; }
    public static Path stateFile() { return STATE_FILE; }
}
