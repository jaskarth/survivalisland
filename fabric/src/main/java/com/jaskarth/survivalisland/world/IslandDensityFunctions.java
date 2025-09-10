package com.jaskarth.survivalisland.world;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.jaskarth.survivalisland.world.density.IslandContinentalNoiseFunction;

public final class IslandDensityFunctions {

    public static void init() {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, ResourceLocation.fromNamespaceAndPath("survivalisland", "islandcont"), IslandContinentalNoiseFunction.UCODEC);
    }
}