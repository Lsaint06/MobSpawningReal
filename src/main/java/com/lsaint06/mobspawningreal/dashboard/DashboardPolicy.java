package com.lsaint06.mobspawningreal.dashboard;

public final class DashboardPolicy {
    private DashboardPolicy() {}
    public static String check(boolean administrator, long expected, long current, boolean validIdentity) {
        if (!administrator) return "Administrator permission is required.";
        if (expected != current) return "Configuration changed. Review the refreshed state and try again.";
        if (!validIdentity) return "This rule is invalid or no longer exists. Reload and select a valid rule.";
        return "";
    }
}
