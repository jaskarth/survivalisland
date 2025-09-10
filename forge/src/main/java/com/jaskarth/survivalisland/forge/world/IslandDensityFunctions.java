package com.jaskarth.survivalisland.forge.world;

import com.jaskarth.survivalisland.world.density.IslandContinentalNoiseFunction;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.*;
import net.minecraftforge.registries.DeferredRegister;

public final class IslandDensityFunctions {
    public static final DeferredRegister<Codec<? extends DensityFunction>> REGISTER2 = DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, "survivalisland");

    public static void init() {
        REGISTER2.register("islandcont", () -> IslandContinentalNoiseFunction.UCODEC);
    }
}
