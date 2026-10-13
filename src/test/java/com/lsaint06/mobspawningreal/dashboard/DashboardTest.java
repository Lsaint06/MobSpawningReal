package com.lsaint06.mobspawningreal.dashboard;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DashboardTest {
    @Test void mutationsRequirePermissionCurrentRevisionAndValidIdentity() {
        assertFalse(DashboardPolicy.check(false, 2, 2, true).isEmpty());
        assertFalse(DashboardPolicy.check(true, 1, 2, true).isEmpty());
        assertFalse(DashboardPolicy.check(true, 2, 2, false).isEmpty());
        assertEquals("", DashboardPolicy.check(true, 2, 2, true));
    }
    @Test void assemblerPublishesOnlyCompleteMatchingTransfers() {
        var assembler = new SnapshotAssembler();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertNull(assembler.accept(a, 1, 2, "world"));
        assertEquals("hello world", assembler.accept(a, 0, 2, "hello "));
        assertNull(assembler.accept(b, 0, 2, "new"));
        assertNull(assembler.accept(a, 1, 2, "old"));
        assertThrows(IllegalArgumentException.class, () -> assembler.accept(b, 0, 999999, "x"));
    }
    @Test void pagingReassemblesLargeUnicodeSnapshots() {
        String original = "Zombie \uD83D\uDC7E ".repeat(10000);
        var pages = SnapshotAssembler.pages(original);
        assertTrue(pages.size() > 1);
        assertTrue(pages.stream().allMatch(s -> s.length() <= SnapshotAssembler.CHUNK_SIZE));
        assertEquals(original, String.join("", pages));
    }
    @Test void pagesDoNotSplitSurrogatePairsForUtf8Transport() {
        String original = "x".repeat(SnapshotAssembler.CHUNK_SIZE-1) + "\uD83D\uDC7E" + "more";
        var pages = SnapshotAssembler.pages(original);
        for (String page : pages) {
            assertFalse(Character.isHighSurrogate(page.charAt(page.length()-1)));
            assertFalse(Character.isLowSurrogate(page.charAt(0)));
        }
    }
    @Test void filteringMatchesTargetsAndDiagnosticsWithoutCaseSensitivity() {
        var row = new DashboardModels.RuleRow("key", "night", "a.json", 10, true, true, null, "minecraft:ZOMBIE", List.of("Bad health"), Map.of());
        assertTrue(row.matches("zombie")); assertTrue(row.matches("HEALTH"));
        assertFalse(row.matches("creeper"));
        assertEquals("Paused", row.status(false));
    }
}
