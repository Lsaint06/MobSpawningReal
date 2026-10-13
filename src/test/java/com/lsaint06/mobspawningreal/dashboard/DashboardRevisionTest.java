package com.lsaint06.mobspawningreal.dashboard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DashboardRevisionTest {
    @Test void observingACommandChangeDoesNotConsumeItsPendingBroadcast() {
        var tracker = new DashboardRevision();
        tracker.observe(1, 1); tracker.broadcasted();
        tracker.observe(1, 2);
        assertTrue(tracker.needsBroadcast());
        tracker.observe(1, 2);
        assertTrue(tracker.needsBroadcast());
        tracker.broadcasted();
        assertFalse(tracker.needsBroadcast());
        assertEquals(2, tracker.current());
    }
}
