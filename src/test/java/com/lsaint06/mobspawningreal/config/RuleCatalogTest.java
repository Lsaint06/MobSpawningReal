package com.lsaint06.mobspawningreal.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class RuleCatalogTest {
    @TempDir Path directory;
    @Test void retainsInvalidEntriesAndRejectsEveryDuplicate() throws Exception {
        Files.writeString(directory.resolve("a.json"), "[{\"id\":\"same\"},{\"id\":\"ok\"},{\"then\":{}},null]");
        Files.writeString(directory.resolve("b.json"), "[{\"id\":\"same\"},{\"id\":\"bad\",\"then\":{\"attributes\":{\"maxHealth\":-1}}}]");
        Files.writeString(directory.resolve("broken.json"), "[oops");
        var catalog = RuleCatalog.load(directory);
        assertEquals(1, catalog.rules().size());
        assertEquals("ok", catalog.rules().getFirst().id);
        assertEquals(7, catalog.entries().size());
        assertEquals(6, catalog.entries().stream().filter(e -> !e.valid()).count());
        assertEquals(2, catalog.entries().stream().filter(e -> e.errors().stream().anyMatch(s -> s.contains("Duplicate"))).count());
        assertEquals("a.json#1", catalog.entries().get(1).key());
    }
    @Test void loadsValidRulesInPriorityOrderWithoutChangingFiles() throws Exception {
        String json = "[{\"id\":\"low\",\"enabled\":false},{\"id\":\"high\",\"priority\":100}]";
        Path file = directory.resolve("rules.json"); Files.writeString(file, json);
        var catalog = RuleCatalog.load(directory);
        assertEquals("high", catalog.rules().getFirst().id);
        assertFalse(catalog.rules().get(1).enabled);
        assertEquals(json, Files.readString(file));
    }
}
