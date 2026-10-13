package com.lsaint06.mobspawningreal.control;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeStateStoreTest {
    @TempDir Path directory;
    @Test void roundTripPreservesOverridesDuringGlobalPause() throws Exception {
        Path file = directory.resolve("state.json");
        var store = new RuntimeStateStore(file);
        assertTrue(store.update(false, Map.of("farm", true, "unknown", false)).success());
        var loaded = new RuntimeStateStore(file);
        assertTrue(loaded.load().success());
        assertFalse(loaded.state().enabled());
        assertEquals(Map.of("farm", true, "unknown", false), loaded.state().ruleOverrides());
    }
    @Test void failedWriteDoesNotPublishStateOrDamagePreviousFile() throws Exception {
        Path parent = directory.resolve("blocked"); Files.writeString(parent, "original");
        var store = new RuntimeStateStore(parent.resolve("state.json"));
        assertFalse(store.update(false, Map.of("farm", false)).success());
        assertTrue(store.state().enabled());
        assertTrue(store.state().ruleOverrides().isEmpty());
        assertEquals("original", Files.readString(parent));
    }
    @Test void malformedStateIsPreservedAndCannotBeOverwritten() throws Exception {
        Path file = directory.resolve("state.json"); Files.writeString(file, "broken");
        var store = new RuntimeStateStore(file);
        assertFalse(store.load().success());
        assertFalse(store.update(false, Map.of()).success());
        assertEquals("broken", Files.readString(file));
    }
}
