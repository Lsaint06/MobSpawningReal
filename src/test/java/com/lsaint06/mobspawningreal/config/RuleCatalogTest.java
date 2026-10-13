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
    @Test void malformedDuplicateStillDisablesItsValidCounterpart() throws Exception {
        Files.writeString(directory.resolve("a.json"), "[{\"id\":\"same\"},{\"id\":\"same\",\"priority\":\"bad\"}]");
        var catalog = RuleCatalog.load(directory);
        assertTrue(catalog.rules().isEmpty());
        assertEquals(2, catalog.entries().stream().filter(e -> e.errors().stream().anyMatch(s -> s.contains("Duplicate"))).count());
    }
    @Test void nonfiniteNumbersAreInvalidButSafeToDisplay() throws Exception {
        Files.writeString(directory.resolve("a.json"), "[{\"id\":\"bad\",\"then\":{\"attributes\":{\"maxHealth\":-1,\"movementSpeed\":\"NaN\"}}}]");
        var catalog = RuleCatalog.load(directory);
        assertTrue(catalog.rules().isEmpty());
        assertTrue(catalog.entries().getFirst().errors().stream().anyMatch(s -> s.contains("finite")));
        var row = com.lsaint06.mobspawningreal.dashboard.RuleSummaries.row(catalog.entries().getFirst(), false, null);
        assertFalse(row.valid());
        assertFalse(row.categories().isEmpty());
    }
    @Test void diagnosticsShowTheirArrayPosition() throws Exception {
        Files.writeString(directory.resolve("a.json"), "[null,null]");
        var catalog = RuleCatalog.load(directory);
        assertTrue(catalog.errors().getFirst().contains("a.json#0"));
        var row = com.lsaint06.mobspawningreal.dashboard.RuleSummaries.row(catalog.entries().get(1),false,null);
        assertTrue(row.source().contains("index 1"));
    }
}
