package supercoder79.survivalisland.world;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraftforge.registries.DeferredRegister;
import supercoder79.survivalisland.mixin.NoiseRouterDataAccessor;

import java.util.function.Supplier;

public final class IslandNoiseGenSettings {
    public static final DeferredRegister<NoiseGeneratorSettings> REGISTER = DeferredRegister.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, "survivalisland");
    public static final ResourceKey<NoiseGeneratorSettings> ISLAND_SETTINGS = ResourceKey.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("survivalisland", "survivalisland"));

    public static void init() {
        register(ISLAND_SETTINGS, IslandNoiseGenSettings::islands);
    }

    private static NoiseGeneratorSettings islands() {
        NoiseRouter oldRouter = NoiseRouterDataAccessor.callOverworld(BuiltinRegistries.DENSITY_FUNCTION, false, false);


        return new NoiseGeneratorSettings(
                OVERWORLD_NOISE_SETTINGS,
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                rerouteRouter(oldRouter),
                SurfaceRuleData.overworld(),
                new OverworldBiomeBuilder().spawnTarget(),
                63,
                false,
                true,
                true,
                false
        );
    }

    private static NoiseRouter rerouteRouter(NoiseRouter router) {
        return new NoiseRouter(
                router.barrierNoise(),
                router.fluidLevelFloodednessNoise(),
                router.fluidLevelSpreadNoise(),
                router.lavaNoise(),
                router.temperature(),
                router.vegetation(),
                getFunction(BuiltinRegistries.DENSITY_FUNCTION, IslandDensityFunctions.ISLAND_CONTINENTALNESS), //router.continents(),
                router.erosion(),
                getFunction(BuiltinRegistries.DENSITY_FUNCTION, IslandDensityFunctions.ISLAND_DEPTH_SPLINE),
                router.ridges(),
                getFunction(BuiltinRegistries.DENSITY_FUNCTION, IslandDensityFunctions.ISLAND_BASE_TERRAIN),
                getFunction(BuiltinRegistries.DENSITY_FUNCTION, IslandDensityFunctions.ISLAND_FINAL_DENSITY),
                router.veinToggle(),
                router.veinRidged(),
                router.veinGap()
        );
    }

    private static DensityFunction getFunction(Registry<DensityFunction> registry, ResourceKey<DensityFunction> resourceKey) {
        return new DensityFunctions.HolderHolder(registry.getHolderOrThrow(resourceKey));
    }

    private static final NoiseSettings OVERWORLD_NOISE_SETTINGS = NoiseSettings.create(-64, 384, 1, 2);

    private static Holder<NoiseGeneratorSettings> register(ResourceKey<NoiseGeneratorSettings> resourceKey, Supplier<NoiseGeneratorSettings> noiseGeneratorSettings) {
        return REGISTER.register(resourceKey.location().getPath(), noiseGeneratorSettings).getHolder().get();
    }
}
