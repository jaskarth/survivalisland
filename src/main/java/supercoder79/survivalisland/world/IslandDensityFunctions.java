package supercoder79.survivalisland.world;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import supercoder79.survivalisland.world.density.IslandContinentalNoiseFunction;

public final class IslandDensityFunctions {

    public static void init() {
        Registry.register(Registries.DENSITY_FUNCTION_TYPE, Identifier.of("survivalisland", "islandcont"), IslandContinentalNoiseFunction.UCODEC);
    }
}