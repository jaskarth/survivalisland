package supercoder79.survivalisland.world;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import supercoder79.survivalisland.world.density.IslandContinentalnessFunction;

public final class IslandDensityFunctions {

    public static void init() {
        // build types
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, new ResourceLocation("survivalisland", "islandcont"), IslandContinentalnessFunction.UCODEC);
    }
}
