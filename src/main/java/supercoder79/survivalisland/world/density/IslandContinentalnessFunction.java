package supercoder79.survivalisland.world.density;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.slf4j.Logger;
import supercoder79.survivalisland.SurvivalIsland;
import supercoder79.survivalisland.noise.OctaveNoise;
import supercoder79.survivalisland.world.util.SeedStealer;

import java.util.Random;

public class IslandContinentalnessFunction implements DensityFunction {
    public static final Codec<IslandContinentalnessFunction> UCODEC = Codec.unit(IslandContinentalnessFunction::new);
    public static final KeyDispatchDataCodec<IslandContinentalnessFunction> CODEC = KeyDispatchDataCodec.of(UCODEC);
    private static final Logger LOGGER = LogUtils.getLogger();

    private long seed;

    OctaveNoise circleNoise = null;
    OctaveNoise radiusNoise = null;
    OctaveNoise sXNoise = null;
    OctaveNoise sZNoise = null;

    private void initSeed(long seed) {
        this.seed = seed;
        Random random = new Random(seed);
        circleNoise = SurvivalIsland.CONFIG.islandCutoffNoise.makeLive(random);
        radiusNoise = SurvivalIsland.CONFIG.radiusModifyNoise.makeLive(random);
        sXNoise = SurvivalIsland.CONFIG.centerXShiftNoise.makeLive(random);
        sZNoise = SurvivalIsland.CONFIG.centerZShiftNoise.makeLive(random);
    }

    private static final double ISLAND_SIZE = SurvivalIsland.CONFIG.islandSize;

    @Override
    public double compute(FunctionContext ctx) {
        int x = ctx.blockX() / 4;
        int z = ctx.blockZ() / 4;

        Vec2i center = findClosestCentroid(this.seed, x, z);

        double ra = radiusNoise.sample(x, z);
        double cx = center.x + sXNoise.sample(x, z);
        double cz = center.z + sZNoise.sample(x, z);
        double dx = (x - cx) / (ISLAND_SIZE + ra);
        double dz = (z - cz) / (ISLAND_SIZE + ra);
        double dist = dx * dx + dz * dz;
        double dstAdd = circleNoise.sample(x, z);
        if (dist < 1 + dstAdd) {
            return Mth.clampedMap(Math.sqrt(dist), 0, 1, 1, -0.4);
        }

        return -0.4;
    }

    @Override
    public void fillArray(double[] ds, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(ds, this);
    }

    private static IslandContinentalnessFunction fork(long seed) {
        IslandContinentalnessFunction func = new IslandContinentalnessFunction();
        func.initSeed(seed);

        return func;
    }

    private static final int GRID_SIZE = SurvivalIsland.CONFIG.islandSeperation;
    private static final int INNER_GRID_SIZE = SurvivalIsland.CONFIG.islandSpacing;

    private static Vec2i findClosestCentroid(long seed, int x, int z) {
        if (SurvivalIsland.CONFIG.hardcoreMode) {
            return new Vec2i(0, 0);
        }

        int cx = x / GRID_SIZE;
        int cz = z / GRID_SIZE;

        Random random = new Random();
        Vec2i min = null;
        int minDist = Integer.MAX_VALUE;
        for (int ax = -1; ax <= 1; ax++) {
            for (int az = -1; az <= 1; az++) {
                int dx = cx + ax;
                int dz = cz + az;
                setDecorationSeed(random, seed, dx, dz);
//                random.setSeed(ChunkPos.asLong(dx, dz));

                int rx = random.nextInt(INNER_GRID_SIZE) + (dx * GRID_SIZE);
                int rz = random.nextInt(INNER_GRID_SIZE) + (dz * GRID_SIZE);

                int dist = ((x - rx) * (x - rx)) + ((z - rz) * (z - rz));
                if (minDist > dist) {
                    minDist = dist;
                    min = new Vec2i(rx, rz);
                }
            }
        }

        return min;
    }

    public static void setDecorationSeed(Random r, long l, int i, int j) {
        r.setSeed(l);
        long m = r.nextLong() | 1L;
        long n = r.nextLong() | 1L;
        long o = (long)i * m + (long)j * n ^ l;
        r.setSeed(o);
    }

    private record Vec2i(int x, int z) {}

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        if (visitor instanceof SeedStealer seed) {
            return fork(seed.steal());
        }

//        LOGGER.error("Couldn't steal seed, something seedy is happening!");

        return this;
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return 1;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
