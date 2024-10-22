package supercoder79.survivalisland.world.density;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import supercoder79.survivalisland.SurvivalIsland;
import supercoder79.survivalisland.noise.IslandContinentalNoise;
import supercoder79.survivalisland.noise.OctaveNoise;
import supercoder79.survivalisland.util.ConcurrentLinkedHashCache;
import supercoder79.survivalisland.world.util.SeedStealer;

import java.util.Objects;

public class IslandContinentalNoiseFunction implements DensityFunction {
    public static final MapCodec<IslandContinentalNoiseFunction> UCODEC = MapCodec.unit(IslandContinentalNoiseFunction::new);
    public static final CodecHolder<IslandContinentalNoiseFunction> CODEC = CodecHolder.of(UCODEC);

    private static final ConcurrentLinkedHashCache<IslandContinentalNoise, IslandContinentalNoise> ISLAND_CONTINENTAL_NOISE_INSTANCE_CACHE =
            new ConcurrentLinkedHashCache<>(1, Integer.MAX_VALUE, 512);

    private final IslandContinentalNoise islandContinentalNoise;

    public IslandContinentalNoiseFunction() {
        this(0);
    }

    public IslandContinentalNoiseFunction(long seed) {
        Random random = new Xoroshiro128PlusPlusRandom(seed);
        RandomSplitter positionalRandomFactory = random.nextSplitter();
        OctaveNoise domainWarpNoise = SurvivalIsland.CONFIG.domainWarpNoise.makeLive(positionalRandomFactory.split("domain_warp_noise"));
        OctaveNoise rangeVariationNoise = SurvivalIsland.CONFIG.rangeVariationNoise.makeLive(positionalRandomFactory.split("range_variation_noise"));
        IslandContinentalNoise islandContinentalNoise = new IslandContinentalNoise(seed,
                SurvivalIsland.CONFIG.islandSize, SurvivalIsland.CONFIG.islandSeperation,
                SurvivalIsland.CONFIG.continentalTargetRangeA.min(), SurvivalIsland.CONFIG.continentalTargetRangeA.max(),
                SurvivalIsland.CONFIG.continentalTargetRangeB.min(), SurvivalIsland.CONFIG.continentalTargetRangeB.max(),
                SurvivalIsland.CONFIG.islandUnderwaterFalloffDistanceMultiplier,
                domainWarpNoise, rangeVariationNoise
        );
        this.islandContinentalNoise = ISLAND_CONTINENTAL_NOISE_INSTANCE_CACHE.computeIfAbsent(islandContinentalNoise, (k) -> islandContinentalNoise);
    }

    @Override
    public double sample(NoisePos ctx) {
        return islandContinentalNoise.compute(ctx.blockX(), ctx.blockZ());
    }

    @Override
    public void fill(double[] ds, EachApplier contextProvider) {
        contextProvider.fill(ds, this);
    }

    private static IslandContinentalNoiseFunction fork(long seed) {
        return new IslandContinentalNoiseFunction(seed);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
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
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IslandContinentalNoiseFunction that = (IslandContinentalNoiseFunction) o;
        return islandContinentalNoise.equals(that.islandContinentalNoise);
    }

    @Override
    public int hashCode() {
        return Objects.hash(islandContinentalNoise);
    }
}
