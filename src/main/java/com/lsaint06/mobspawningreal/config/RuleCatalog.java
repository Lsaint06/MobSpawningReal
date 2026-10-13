package com.lsaint06.mobspawningreal.config;
import com.google.gson.*;
import com.lsaint06.mobspawningreal.rule.MobRule;
import com.lsaint06.mobspawningreal.rule.RuleSemantics;
import java.nio.file.*;
import java.util.*;

/** Diagnostics are retained separately from executable rules. */
public record RuleCatalog(List<MobRule> rules, List<Entry> entries, List<String> errors) {
    private static final Gson GSON = new Gson();
    public RuleCatalog { rules = List.copyOf(rules); entries = List.copyOf(entries); errors = List.copyOf(errors); }
    public record Entry(String key, String source, String id, MobRule rule, List<String> errors) {
        public Entry { errors = List.copyOf(errors); }
        public boolean valid() { return rule != null && errors.isEmpty(); }
    }
    public static RuleCatalog load(Path directory) {
        List<Entry> entries = new ArrayList<>();
        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                String source = path.getFileName().toString();
                try (var reader = Files.newBufferedReader(path)) {
                    JsonElement root = JsonParser.parseReader(reader);
                    if (!root.isJsonArray()) throw new IllegalArgumentException("Expected an array of rules");
                    int index = 0;
                    for (JsonElement json : root.getAsJsonArray()) {
                        String key = source + "#" + index++;
                        MobRule rule = null; String id = "Invalid entry";
                        List<String> problems = new ArrayList<>();
                        try {
                            if (!json.isJsonObject()) throw new IllegalArgumentException("Rule must be an object");
                            var rawId = json.getAsJsonObject().get("id");
                            if (rawId == null || !rawId.isJsonPrimitive() || !rawId.getAsJsonPrimitive().isString() || rawId.getAsString().isBlank()) throw new IllegalArgumentException("id must be a nonblank string");
                            id = rawId.getAsString();
                            if (!id.equals(id.trim())) throw new IllegalArgumentException("id must not have surrounding whitespace");
                            rule = GSON.fromJson(json, MobRule.class); rule.normalize();
                            problems.addAll(RuleValidator.validate(rule));
                        } catch (Exception e) { problems.add(message(e)); }
                        entries.add(new Entry(key, source, id, rule, problems));
                    }
                } catch (Exception e) { entries.add(new Entry(source + "#file", source, "Invalid file", null, List.of(message(e)))); }
            }
        } catch (Exception e) { entries.add(new Entry("directory", "rules", "Configuration unavailable", null, List.of(message(e)))); }
        Map<String, Long> counts = new HashMap<>();
        entries.stream().filter(e -> e.rule != null).forEach(e -> counts.merge(e.id, 1L, Long::sum));
        List<Entry> checked = entries.stream().map(e -> {
            if (e.rule == null || counts.getOrDefault(e.id, 0L) < 2) return e;
            List<String> problems = new ArrayList<>(e.errors); problems.add("Duplicate rule id: " + e.id);
            return new Entry(e.key, e.source, e.id, e.rule, problems);
        }).toList();
        var valid = RuleSemantics.sort(checked.stream().filter(Entry::valid).map(Entry::rule).toList());
        var errors = checked.stream().flatMap(e -> e.errors.stream().map(p -> e.source + " :: " + e.id + " :: " + p)).toList();
        return new RuleCatalog(valid, checked, errors);
    }
    private static String message(Exception e) { return Objects.toString(e.getMessage(), e.getClass().getSimpleName()); }
}
