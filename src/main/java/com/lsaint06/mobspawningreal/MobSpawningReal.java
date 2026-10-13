package com.lsaint06.mobspawningreal;

import com.lsaint06.mobspawningreal.config.RuleManager;
import com.lsaint06.mobspawningreal.control.RuntimeControl;
import com.lsaint06.mobspawningreal.event.MobControlEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MobSpawningReal.MOD_ID)
public final class MobSpawningReal {
    public static final String MOD_ID = "mobspawningreal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public MobSpawningReal(IEventBus modBus) {
        RuntimeControl.load();
        RuleManager.reload();
        NeoForge.EVENT_BUS.register(MobControlEvents.class);
    }
}
