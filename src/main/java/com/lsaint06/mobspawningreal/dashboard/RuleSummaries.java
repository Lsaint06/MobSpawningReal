package com.lsaint06.mobspawningreal.dashboard;
import com.google.gson.*;
import com.lsaint06.mobspawningreal.config.RuleCatalog;
import java.util.*;
import static com.lsaint06.mobspawningreal.dashboard.DashboardModels.*;

/** Display-only category adapters are the extension point for future editors. */
public final class RuleSummaries {
    private static final Gson GSON = new GsonBuilder().serializeSpecialFloatingPointValues().create();
    private RuleSummaries() {}
    public static RuleRow row(RuleCatalog.Entry entry, boolean selected, Boolean override) {
        var rule = entry.rule();
        Map<String, List<String>> categories = new LinkedHashMap<>();
        if (rule != null) {
            List<String> spawn = new ArrayList<>();
            spawn.addAll(lines("Conditions", rule.when));
            spawn.addAll(lines("Spawn", rule.then.spawn));
            spawn.addAll(lines("Active spawning", rule.then.activeSpawn));
            categories.put(Category.SPAWN.name(), List.copyOf(spawn));
            categories.put(Category.ATTRIBUTES.name(), lines("Attributes", rule.then.attributes));
            categories.put(Category.EQUIPMENT.name(), lines("Equipment", rule.then.equipment));
            categories.put(Category.LOOT.name(), lines("Loot", rule.then.loot));
            var behavior = new ArrayList<>(lines("Behavior", rule.then.behavior));
            if (rule.then.outgoingDamage != null) behavior.add("Outgoing damage: " + rule.then.outgoingDamage);
            behavior.addAll(lines("Name", rule.then.name));
            categories.put(Category.BEHAVIOR.name(), List.copyOf(behavior));
        }
        String position = entry.key().substring(entry.key().lastIndexOf('#')+1);
        String source = position.matches("[0-9]+") ? entry.source()+" [index "+position+"]" : entry.source();
        return new RuleRow(entry.key(), entry.id(), source, rule == null ? 0 : rule.priority,
                entry.valid(), selected, override, rule == null || rule.when.entities.isEmpty() ? "All mobs" : String.join(", ", rule.when.entities),
                entry.errors(), categories);
    }
    private static List<String> lines(String group, Object object) {
        List<String> lines = new ArrayList<>();
        flatten(group, GSON.toJsonTree(object), lines);
        return List.copyOf(lines);
    }
    private static void flatten(String label, JsonElement value, List<String> lines) {
        if (value == null || value.isJsonNull()) return;
        if (value.isJsonObject()) value.getAsJsonObject().entrySet().forEach(e -> flatten(label + " / " + humanize(e.getKey()), e.getValue(), lines));
        else if (value.isJsonArray()) {
            if (!value.getAsJsonArray().isEmpty()) {
                int index = 1;
                for (var child : value.getAsJsonArray()) flatten(label + " " + index++, child, lines);
            }
        } else lines.add(label + ": " + value.getAsString());
    }
    private static String humanize(String text) { return text.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' '); }
}
