package supercoder79.survivalisland.world.density;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.slf4j.Logger;
import supercoder79.survivalisland.SurvivalIsland;
import supercoder79.survivalisland.noise.IslandContinentalNoise;
import supercoder79.survivalisland.noise.OctaveNoise;
import supercoder79.survivalisland.world.util.SeedStealer;

public class IslandContinentalNoiseFunction implements DensityFunction {
    public static final Codec<IslandContinentalNoiseFunction> UCODEC = Codec.unit(IslandContinentalNoiseFunction::new);
    public static final KeyDispatchDataCodec<IslandContinentalNoiseFunction> CODEC = KeyDispatchDataCodec.of(UCODEC);

    private final IslandContinentalNoise islandContinentalNoise;

    public IslandContinentalNoiseFunction() {
        this(0);
    }

    public IslandContinentalNoiseFunction(long seed) {
        RandomSource random = new XoroshiroRandomSource(seed);
        PositionalRandomFactory positionalRandomFactory = random.forkPositional();
        OctaveNoise domainWarpNoise = SurvivalIsland.CONFIG.domainWarpNoise.makeLive(positionalRandomFactory.fromHashOf("domain_warp_noise"));
        OctaveNoise rangeVariationNoise = SurvivalIsland.CONFIG.rangeVariationNoise.makeLive(positionalRandomFactory.fromHashOf("range_variation_noise"));
        islandContinentalNoise = new IslandContinentalNoise(seed,
                SurvivalIsland.CONFIG.islandSize, SurvivalIsland.CONFIG.islandSeperation,
                SurvivalIsland.CONFIG.continentalTargetRangeA.min(), SurvivalIsland.CONFIG.continentalTargetRangeA.max(),
                SurvivalIsland.CONFIG.continentalTargetRangeB.min(), SurvivalIsland.CONFIG.continentalTargetRangeB.max(),
                SurvivalIsland.CONFIG.islandUnderwaterFalloffDistanceMultiplier,
                domainWarpNoise, rangeVariationNoise
        );
    }

    @Override
    public double compute(FunctionContext ctx) {
        return islandContinentalNoise.compute(ctx.blockX(), ctx.blockZ());
    }

    @Override
    public void fillArray(double[] ds, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(ds, this);
    }

    private static IslandContinentalNoiseFunction fork(long seed) {
        return new IslandContinentalNoiseFunction(seed);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        if (visitor instanceof SeedStealer seed) {
            return fork(seed.steal());
        }
        return this;
    }

    @Override
    public double minValue() {
        return islandContinentalNoise.minValue();
    }

    @Override
    public double maxValue() {
        return islandContinentalNoise.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
