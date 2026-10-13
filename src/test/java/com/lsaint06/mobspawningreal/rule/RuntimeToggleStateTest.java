package com.lsaint06.mobspawningreal.rule;

import com.lsaint06.mobspawningreal.control.RuntimeToggleState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RuntimeToggleStateTest {
    @Test
    void globalToggleOverridesRuleState() {
        RuntimeToggleState state = new RuntimeToggleState();
        assertTrue(state.isRuleEnabled("farm", true));

        state.setGloballyEnabled(false);
        assertFalse(state.isRuleEnabled("farm", true));
    }

    @Test
    void runtimeRuleOverrideCanEnableOrDisableConfiguredRule() {
        RuntimeToggleState state = new RuntimeToggleState();

        state.disableRule("farm");
        assertFalse(state.isRuleEnabled("farm", true));

        state.enableRule("farm");
        assertTrue(state.isRuleEnabled("farm", false));

        state.clearRuleOverride("farm");
        assertFalse(state.isRuleEnabled("farm", false));
    }
}
