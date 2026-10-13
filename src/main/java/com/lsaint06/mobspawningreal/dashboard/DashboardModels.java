package com.lsaint06.mobspawningreal.dashboard;
import java.util.*;

/** Transport summaries are deliberately separate from executable MobRule objects. */
public final class DashboardModels {
    private DashboardModels() {}
    public enum Action { REFRESH, GLOBAL, RULE, RESET, RELOAD, CLOSE }
    public enum Category {
        SPAWN("Spawn Settings"), ATTRIBUTES("Attributes"), EQUIPMENT("Equipment"), LOOT("Loot"), BEHAVIOR("AI Behaviors");
        public final String title;
        Category(String title) { this.title = title; }
    }
    public record Snapshot(long revision, long request, boolean enabled, String message, boolean error, List<RuleRow> rules) {
        public Snapshot { rules = List.copyOf(rules); }
    }
    public record RuleRow(String key, String id, String source, int priority, boolean valid,
                          boolean enabled, Boolean override, String targets, List<String> errors,
                          Map<String, List<String>> categories) {
        public RuleRow { errors = List.copyOf(errors); categories = Map.copyOf(categories); }
        public boolean matches(String query) {
            String q = query.strip().toLowerCase(Locale.ROOT);
            return (id + " " + source + " " + targets + " " + String.join(" ", errors)).toLowerCase(Locale.ROOT).contains(q);
        }
        public String status(boolean global) { return !valid ? "Invalid" : !enabled ? "Disabled" : !global ? "Paused" : "Enabled"; }
        public String stateSource() { return override == null ? "Following JSON" : "Runtime override"; }
    }
}
