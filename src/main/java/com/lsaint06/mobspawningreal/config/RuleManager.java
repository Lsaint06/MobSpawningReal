package com.lsaint06.mobspawningreal.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lsaint06.mobspawningreal.MobSpawningReal;
import com.lsaint06.mobspawningreal.rule.MobRule;
import com.lsaint06.mobspawningreal.rule.RuleSemantics;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class RuleManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final AtomicReference<List<MobRule>> RULES = new AtomicReference<>(List.of());
    private static final Path RULE_DIR = FMLPaths.CONFIGDIR.get().resolve(MobSpawningReal.MOD_ID).resolve("rules");

    private RuleManager() {}

    public static List<MobRule> rules() { return RULES.get(); }
    public static Path ruleDirectory() { return RULE_DIR; }

    public static LoadResult reload() {
        List<String> errors = new ArrayList<>();
        List<MobRule> loaded = new ArrayList<>();
        try {
            Files.createDirectories(RULE_DIR);
            ensureExample();
            try (var stream = Files.list(RULE_DIR)) {
                for (Path path : stream.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    try (Reader reader = Files.newBufferedReader(path)) {
                        MobRule[] rules = GSON.fromJson(reader, MobRule[].class);
                        if (rules == null) continue;
                        for (MobRule rule : rules) {
                            if (rule == null) continue;
                            rule.normalize();
                            List<String> ruleErrors = RuleValidator.validate(rule);
                            if (ruleErrors.isEmpty()) loaded.add(rule);
                            else ruleErrors.forEach(e -> errors.add(path.getFileName() + " :: " + rule.id + " :: " + e));
                        }
                    } catch (Exception e) {
                        errors.add(path.getFileName() + " :: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            errors.add("Could not access rule directory: " + e.getMessage());
        }
        List<MobRule> sorted = RuleSemantics.sort(loaded);
        RULES.set(sorted);
        MobSpawningReal.LOGGER.info("Loaded {} Mob Spawning Real rules with {} validation errors", sorted.size(), errors.size());
        errors.forEach(MobSpawningReal.LOGGER::warn);
        return new LoadResult(sorted.size(), List.copyOf(errors));
    }

    private static void ensureExample() throws IOException {
        Path example = RULE_DIR.resolve("00-example.disabled.json");
        if (Files.exists(example)) return;
        try (Writer writer = Files.newBufferedWriter(example)) {
            writer.write("[\n  {\n    \"id\": \"example_zombie\",\n    \"enabled\": false,\n    \"priority\": 100,\n    \"when\": {\n      \"entities\": [\"minecraft:zombie\"],\n      \"dimensions\": [\"minecraft:overworld\"],\n      \"time\": {\"start\": 13000, \"end\": 23000},\n      \"light\": {\"max\": 7}\n    },\n    \"then\": {\n      \"spawn\": {\"allow\": true, \"rateMultiplier\": 2.0, \"maxPerPlayer\": 30},\n      \"attributes\": {\"maxHealth\": 30.0, \"movementSpeed\": 0.27},\n      \"name\": {\"text\": \"Night Stalker\", \"alwaysVisible\": false},\n      \"behavior\": {\"alwaysHostile\": true, \"climb\": true}\n    }\n  }\n]\n");
        }
    }

    public record LoadResult(int loaded, List<String> errors) {}
}
