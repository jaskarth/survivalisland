package supercoder79.survivalisland.world;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import supercoder79.survivalisland.world.density.IslandContinentalNoiseFunction;

public final class IslandDensityFunctions {

    public static void init() {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, new ResourceLocation("survivalisland", "islandcont"), IslandContinentalNoiseFunction.UCODEC);
    }
}