package supercoder79.survivalisland.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegisterEvent;
import supercoder79.survivalisland.world.density.IslandContinentalnessFunction;

import java.util.function.Supplier;

public final class IslandDensityFunctions {
//    public static final DeferredRegister<DensityFunction> REGISTER = DeferredRegister.create(Registry.DENSITY_FUNCTION_REGISTRY, "survivalisland");
    public static final DeferredRegister<Codec<? extends DensityFunction>> REGISTER2 = DeferredRegister.create(Registry.DENSITY_FUNCTION_TYPE_REGISTRY, "survivalisland");
    public static ResourceKey<DensityFunction> ISLAND_CONTINENTALNESS = of("island_continentalness");
    public static ResourceKey<DensityFunction> ISLAND_OFFSET_SPLINE = of("island_offset_spline");
    public static ResourceKey<DensityFunction> ISLAND_FACTOR_SPLINE = of("island_factor_spline");
    public static ResourceKey<DensityFunction> ISLAND_DEPTH_SPLINE = of("island_depth_spline");
    public static ResourceKey<DensityFunction> ISLAND_JAGGED_SPLINE = of("island_jagged_spline");
    public static ResourceKey<DensityFunction> ISLAND_SLOPED_CHEESE = of("island_sloped_cheese");

    public static ResourceKey<DensityFunction> ISLAND_BASE_TERRAIN = of("island_base_terrain");
    public static ResourceKey<DensityFunction> ISLAND_FINAL_DENSITY = of("island_final_density");

    private static final ResourceKey<DensityFunction> BASE_3D_NOISE_OVERWORLD = ofMc("overworld/base_3d_noise");
    private static final ResourceKey<DensityFunction> SPAGHETTI_ROUGHNESS_FUNCTION = ofMc("overworld/caves/spaghetti_roughness_function");
    private static final ResourceKey<DensityFunction> NOODLE = ofMc("overworld/caves/noodle");
    private static final ResourceKey<DensityFunction> ENTRANCES = ofMc("overworld/caves/entrances");
    private static final ResourceKey<DensityFunction> PILLARS = ofMc("overworld/caves/pillars");
    private static final ResourceKey<DensityFunction> SPAGHETTI_2D = ofMc("overworld/caves/spaghetti_2d");

    private static final DensityFunction BLENDING_FACTOR = DensityFunctions.constant(10.0);
    private static final DensityFunction BLENDING_JAGGEDNESS = DensityFunctions.zero();

    private static ResourceKey<DensityFunction> of(String val) {
        return ResourceKey.create(Registry.DENSITY_FUNCTION_REGISTRY, new ResourceLocation("survivalisland", val));
    }

    private static ResourceKey<DensityFunction> ofMc(String val) {
        return ResourceKey.create(Registry.DENSITY_FUNCTION_REGISTRY, new ResourceLocation(val));
    }

    public static void init() {
        // build types
        REGISTER2.register("islandcont", () -> IslandContinentalnessFunction.UCODEC);
    }

    public static void post(RegisterEvent.RegisterHelper<DensityFunction> rh) {

        DensityFunction cont = DensityFunctions.flatCache(DensityFunctions.cache2d(new IslandContinentalnessFunction()));
        rh.register(ISLAND_CONTINENTALNESS, cont);
        Holder<DensityFunction> isContEntry = BuiltinRegistries.DENSITY_FUNCTION.getHolderOrThrow(ISLAND_CONTINENTALNESS);

        DensityFunctions.Spline.Coordinate coordinate = new DensityFunctions.Spline.Coordinate(isContEntry);
        DensityFunctions.Spline.Coordinate coordinate2 = new DensityFunctions.Spline.Coordinate(BuiltinRegistries.DENSITY_FUNCTION.getHolderOrThrow(NoiseRouterData.EROSION));
        DensityFunctions.Spline.Coordinate coordinate3 = new DensityFunctions.Spline.Coordinate(BuiltinRegistries.DENSITY_FUNCTION.getHolderOrThrow(NoiseRouterData.RIDGES));
        DensityFunctions.Spline.Coordinate coordinate4 = new DensityFunctions.Spline.Coordinate(BuiltinRegistries.DENSITY_FUNCTION.getHolderOrThrow(NoiseRouterData.RIDGES_FOLDED));
        DensityFunction offsetSpline = registerAndWrap(
                rh,
                ISLAND_OFFSET_SPLINE,
                splineWithBlending(
                        DensityFunctions.add(
                                DensityFunctions.constant(-0.50375F), DensityFunctions.spline(TerrainProvider.overworldOffset(coordinate, coordinate2, coordinate4, false))
                        ),
                        DensityFunctions.blendOffset()
                )
        );
        DensityFunction factorSpline = registerAndWrap(
                rh,
                ISLAND_FACTOR_SPLINE,
                splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldFactor(coordinate, coordinate2, coordinate3, coordinate4, false)), BLENDING_FACTOR)
        );
        DensityFunction depthSpline = registerAndWrap(
                rh,
                ISLAND_DEPTH_SPLINE, DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5, -1.5), offsetSpline)
        );
        DensityFunction jaggedSpline = registerAndWrap(
                rh,
                ISLAND_JAGGED_SPLINE,
                splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldJaggedness(coordinate, coordinate2, coordinate3, coordinate4, false)), BLENDING_JAGGEDNESS)
        );

        DensityFunction noisegrad = noiseGradientDensity(DensityFunctions.cache2d(factorSpline), depthSpline);

        DensityFunction baseTerrain = slideOverworld(false, DensityFunctions.add(noisegrad, DensityFunctions.constant(-0.703125)).clamp(-64.0, 64.0));
        register(rh, ISLAND_BASE_TERRAIN, baseTerrain);

        DensityFunction jaggednoise = DensityFunctions.noise(getNoise(Noises.JAGGED), 1500.0, 0.0);
        DensityFunction densityFunction6 = DensityFunctions.mul(jaggedSpline, jaggednoise.halfNegative());
        DensityFunction noisegrad2 = noiseGradientDensity(factorSpline, DensityFunctions.add(depthSpline, densityFunction6));

        register(rh, ISLAND_SLOPED_CHEESE, DensityFunctions.add(noisegrad2, getFunction(BASE_3D_NOISE_OVERWORLD)));

        DensityFunction densityFunction12 = getFunction(ISLAND_SLOPED_CHEESE);
        DensityFunction densityFunction13 = DensityFunctions.min(
                densityFunction12, DensityFunctions.mul(DensityFunctions.constant(5.0), getFunction(ENTRANCES))
        );
        DensityFunction densityFunction14 = DensityFunctions.rangeChoice(
                densityFunction12, -1000000.0, 1.5625, densityFunction13, underground(densityFunction12)
        );
        DensityFunction finalDensity = DensityFunctions.min(postProcess(slideOverworld(false, densityFunction14)), getFunction(NOODLE));

        register(rh, ISLAND_FINAL_DENSITY, finalDensity);
    }

    private static DensityFunction underground(DensityFunction densityFunction) {
        DensityFunction densityFunction2 = getFunction(SPAGHETTI_2D);
        DensityFunction densityFunction3 = getFunction(SPAGHETTI_ROUGHNESS_FUNCTION);
        DensityFunction densityFunction4 = DensityFunctions.noise(getNoise(Noises.CAVE_LAYER), 8.0);
        DensityFunction densityFunction5 = DensityFunctions.mul(DensityFunctions.constant(4.0), densityFunction4.square());
        DensityFunction densityFunction6 = DensityFunctions.noise(getNoise(Noises.CAVE_CHEESE), 0.6666666666666666);
        DensityFunction densityFunction7 = DensityFunctions.add(
                DensityFunctions.add(DensityFunctions.constant(0.27), densityFunction6).clamp(-1.0, 1.0),
                DensityFunctions.add(DensityFunctions.constant(1.5), DensityFunctions.mul(DensityFunctions.constant(-0.64), densityFunction)).clamp(0.0, 0.5)
        );
        DensityFunction densityFunction8 = DensityFunctions.add(densityFunction5, densityFunction7);
        DensityFunction densityFunction9 = DensityFunctions.min(
                DensityFunctions.min(densityFunction8, getFunction(ENTRANCES)), DensityFunctions.add(densityFunction2, densityFunction3)
        );
        DensityFunction densityFunction10 = getFunction(PILLARS);
        DensityFunction densityFunction11 = DensityFunctions.rangeChoice(
                densityFunction10, -1000000.0, 0.03, DensityFunctions.constant(-1000000.0), densityFunction10
        );
        return DensityFunctions.max(densityFunction9, densityFunction11);
    }

    private static DensityFunction postProcess(DensityFunction densityFunction) {
        DensityFunction densityFunction2 = DensityFunctions.blendDensity(densityFunction);
        return DensityFunctions.mul(DensityFunctions.interpolated(densityFunction2), DensityFunctions.constant(0.64)).squeeze();
    }

    private static DensityFunction noiseGradientDensity(DensityFunction densityFunction, DensityFunction densityFunction2) {
        DensityFunction densityFunction3 = DensityFunctions.mul(densityFunction2, densityFunction);
        return DensityFunctions.mul(DensityFunctions.constant(4.0), densityFunction3.quarterNegative());
    }

    private static DensityFunction getFunction(ResourceKey<DensityFunction> resourceKey) {
        return new DensityFunctions.HolderHolder(BuiltinRegistries.DENSITY_FUNCTION.getHolderOrThrow(resourceKey));
    }

    private static DensityFunction splineWithBlending(DensityFunction densityFunction, DensityFunction densityFunction2) {
        DensityFunction densityFunction3 = DensityFunctions.lerp(DensityFunctions.blendAlpha(), densityFunction2, densityFunction);
        return DensityFunctions.flatCache(DensityFunctions.cache2d(densityFunction3));
    }

    private static DensityFunction registerAndWrap(RegisterEvent.RegisterHelper<DensityFunction> rh, ResourceKey<DensityFunction> resourceKey, DensityFunction func) {
        rh.register(resourceKey, func);

        return new DensityFunctions.HolderHolder(BuiltinRegistries.DENSITY_FUNCTION.getHolderOrThrow(resourceKey));
    }

    private static DensityFunction slideOverworld(boolean bl, DensityFunction densityFunction) {
        return slide(densityFunction, -64, 384, bl ? 16 : 80, bl ? 0 : 64, -0.078125, 0, 24, bl ? 0.4 : 0.1171875);
    }

    private static Holder<NormalNoise.NoiseParameters> getNoise(ResourceKey<NormalNoise.NoiseParameters> resourceKey) {
        return BuiltinRegistries.NOISE.getHolderOrThrow(resourceKey);
    }

    private static DensityFunction slide(DensityFunction densityFunction, int i, int j, int k, int l, double d, int m, int n, double e) {
        DensityFunction densityFunction3 = DensityFunctions.yClampedGradient(i + j - k, i + j - l, 1.0, 0.0);
        DensityFunction densityFunction2 = DensityFunctions.lerp(densityFunction3, d, densityFunction);
        DensityFunction densityFunction4 = DensityFunctions.yClampedGradient(i + m, i + n, 0.0, 1.0);
        return DensityFunctions.lerp(densityFunction4, e, densityFunction2);
    }

    private static void register(RegisterEvent.RegisterHelper<DensityFunction> rh, ResourceKey<DensityFunction> resourceKey, DensityFunction func) {
        rh.register(resourceKey.location().getPath(), func);
//        return REGISTER.register(resourceKey.location().getPath(), () -> func).getHolder().get();
//        return BuiltinRegistries.register(BuiltinRegistries.DENSITY_FUNCTION, resourceKey.location(), func);
    }
}
