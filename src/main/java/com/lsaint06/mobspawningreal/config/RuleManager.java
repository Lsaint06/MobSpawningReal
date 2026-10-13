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
    private static final AtomicReference<RuleCatalog> CATALOG = new AtomicReference<>(new RuleCatalog(List.of(), List.of(), List.of()));
    private static volatile long revision;
    private static final Path RULE_DIR = FMLPaths.CONFIGDIR.get().resolve(MobSpawningReal.MOD_ID).resolve("rules");

    private RuleManager() {}

    public static List<MobRule> rules() { return CATALOG.get().rules(); }
    public static RuleCatalog catalog() { return CATALOG.get(); }
    public static long revision() { return revision; }
    public static Path ruleDirectory() { return RULE_DIR; }

    public static synchronized LoadResult reload() {
        RuleCatalog catalog;
        try {
            Files.createDirectories(RULE_DIR);
            ensureExample();
            catalog = RuleCatalog.load(RULE_DIR);
        } catch (IOException e) {
            String error = "Could not access rule directory: " + e.getMessage();
            catalog = new RuleCatalog(List.of(), List.of(new RuleCatalog.Entry("directory", "rules", "Configuration unavailable", null, List.of(error))), List.of(error));
        }
        CATALOG.set(catalog);
        revision++;
        MobSpawningReal.LOGGER.info("Loaded {} Mob Spawning Real rules with {} validation errors", catalog.rules().size(), catalog.errors().size());
        catalog.errors().forEach(MobSpawningReal.LOGGER::warn);
        return new LoadResult(catalog.rules().size(), catalog.errors());
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

