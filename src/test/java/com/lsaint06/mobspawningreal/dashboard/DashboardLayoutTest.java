package com.lsaint06.mobspawningreal.dashboard;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DashboardLayoutTest {
    @Test void onlyTheSwitchAreaTogglesARule() {
        assertTrue(DashboardLayout.switchHit(300, 100, 250, 110));
        assertFalse(DashboardLayout.switchHit(300, 100, 250, 138));
        assertFalse(DashboardLayout.switchHit(300, 100, 80, 110));
    }
    @Test void compactAndWideLayoutsKeepPanelsWithinTheWindow() {
        for (int width : new int[]{256, 320, 427, 640, 960, 1920}) {
            for (int height : new int[]{180, 240, 360, 720, 1080}) {
                var layout = DashboardLayout.of(width, height);
                assertTrue(layout.x() >= 0 && layout.y() >= 0);
                assertTrue(layout.x() + layout.width() <= width);
                assertTrue(layout.y() + layout.height() <= height);
                assertTrue(layout.contentHeight() >= 1);
                assertEquals(width >= 640, layout.wide());
            }
        }
    }
}
