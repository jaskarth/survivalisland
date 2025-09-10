package com.jaskarth.survivalisland.forge;

import com.jaskarth.survivalisland.SurvivalIsland;
import com.mojang.logging.LogUtils;
import com.jaskarth.survivalisland.forge.world.IslandDensityFunctions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SurvivalIsland.ID)
public class SurvivalIslandForge extends SurvivalIsland {

    public SurvivalIslandForge(IEventBus modEventBus, ModContainer container) {
        super.init();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerFunctions);

        IslandDensityFunctions.REGISTER2.register(modEventBus);
        IslandDensityFunctions.init();

        // Register ourselves for server and other game events we are interested in
//        NeoForge.EVENT_BUS.register(this);
    }

    private void registerFunctions(RegisterEvent event) {
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }
}
