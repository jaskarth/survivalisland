package com.jaskarth.survivalisland;

import com.jaskarth.survivalisland.world.IslandDensityFunctions;
import net.fabricmc.api.ModInitializer;

public class SurvivalIslandFabric extends SurvivalIsland implements ModInitializer {

    @Override
    public void onInitialize() {
        super.init();
        IslandDensityFunctions.init();
    }
}