package com.jaskarth.survivalisland.forge;

import com.jaskarth.survivalisland.SurvivalIsland;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import com.jaskarth.survivalisland.forge.world.IslandDensityFunctions;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SurvivalIsland.ID)
public class SurvivalIslandForge extends SurvivalIsland {

    public SurvivalIslandForge() {
        super.init();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerFunctions);

        IslandDensityFunctions.REGISTER2.register(modEventBus);
        IslandDensityFunctions.init();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void registerFunctions(RegisterEvent event) {
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }
}
