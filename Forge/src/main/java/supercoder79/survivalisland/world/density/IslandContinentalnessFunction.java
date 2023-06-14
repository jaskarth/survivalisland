package supercoder79.survivalisland.world.density;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
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

    //OctaveNoise circleNoise = null;
    //OctaveNoise radiusNoise = null;
    OctaveNoise sXNoise = null;
    OctaveNoise sZNoise = null;

    private void initSeed(long seed) {
        this.seed = seed;
        Random random = new Random(seed);
        //circleNoise = SurvivalIsland.CONFIG.islandCutoffNoise.makeLive(random);
        //radiusNoise = SurvivalIsland.CONFIG.radiusModifyNoise.makeLive(random);
        sXNoise = SurvivalIsland.CONFIG.domainWarpNoise.makeLive(random);
        sZNoise = SurvivalIsland.CONFIG.domainWarpNoise.makeLive(random);
    }

    private static final long PRIME_X = 0x5205402B9270C86FL;
    private static final long PRIME_Z = 0x5BCC226E9FA0BACBL;
    private static final long PRIME_I = 0x598CD327003817B5L;
    private static final long HASH_MULTIPLIER = 0x53A3F72DEEC546F5L;

    private static final double TARGET_MAX_VALUE = 0.6;
    private static final double TARGET_MIN_VALUE = -0.4;

    private static final double UNIT_INTERVAL_MULTIPLIER = TARGET_MAX_VALUE - TARGET_MIN_VALUE;
    private static final double UNIT_INTERVAL_OFFSET = TARGET_MIN_VALUE;
    private static final double UNIT_INTERVAL_VALUE_CREATING_A_ZERO_OUTPUT = -UNIT_INTERVAL_OFFSET / UNIT_INTERVAL_MULTIPLIER;
    private static final double PRE_CURVED_VALUE_CREATING_A_ZERO_OUTPUT = Math.sqrt(1 - Math.pow(UNIT_INTERVAL_VALUE_CREATING_A_ZERO_OUTPUT, 1.0 / 3.0)); // inverse of falloff=(1-dist²)³
    private static final double RADIUS_MULTIPLIER_TO_ACCOUNT_FOR_UNDERWATER_FALLOFF = 1.0 / PRE_CURVED_VALUE_CREATING_A_ZERO_OUTPUT;

    private static final double ISLAND_SIZE_ABSOLUTE = SurvivalIsland.CONFIG.islandSize;
    private static final double ISLAND_SIZE_ABSOLUTE_PADDED = ISLAND_SIZE_ABSOLUTE
            * RADIUS_MULTIPLIER_TO_ACCOUNT_FOR_UNDERWATER_FALLOFF;

    private static final int ISLAND_SEPARATION_ABSOLUTE = SurvivalIsland.CONFIG.islandSeperation;
    private static final double SQUARE_DIAGONAL = Math.sqrt(2);
    private static final double GRID_CELL_SIZE = ISLAND_SEPARATION_ABSOLUTE * SQUARE_DIAGONAL; // This seemed to make it look good
    private static final double GRID_FREQUENCY = 1.0 / GRID_CELL_SIZE;
    private static final double ISLAND_SIZE_RELATIVE_PADDED = ISLAND_SIZE_ABSOLUTE_PADDED * GRID_FREQUENCY;
    private static final double ISLAND_SEPARATION_RELATIVE = ISLAND_SEPARATION_ABSOLUTE * GRID_FREQUENCY;

    private static final double TOTAL_SEARCH_RADIUS = ISLAND_SIZE_RELATIVE_PADDED + ISLAND_SEPARATION_RELATIVE;
    private static final int TOTAL_SEARCH_RADIUS_BOUND = Mth.ceil(TOTAL_SEARCH_RADIUS);
    private static final int TOTAL_SEARCH_DIAMETER_BOUND = TOTAL_SEARCH_RADIUS_BOUND * 2 + 1;
    private static final int TOTAL_SEARCH_GRID_SIZE = TOTAL_SEARCH_DIAMETER_BOUND * TOTAL_SEARCH_DIAMETER_BOUND;

    private static final int ISLAND_SIZE_SEARCH_BOUND = Mth.ceil(ISLAND_SIZE_RELATIVE_PADDED);

    // Number of islands to try placing = asymptotic # possible per area in densest packing
    // It's not critical for this to be exactly this value, but this was simple enough.
    // For ISLAND_SEPARATION_ABSOLUTE * SQUARE_DIAGONAL the ceiling result is 2.
    private static final double PACKING_RATIO = 2 / Math.sqrt(3);
    private static final int N_ISLANDS_TO_TRY_PER_CELL = Mth.ceil(Mth.square(PACKING_RATIO * GRID_CELL_SIZE / ISLAND_SEPARATION_ABSOLUTE));

    private record ProspectiveIslandEntry(
        float jitterX, float jitterZ, int rank
    ) { }
    private final ThreadLocal<ProspectiveIslandEntry[]> prospectiveIslandEntriesThreadLocal
            = ThreadLocal.withInitial(() -> new ProspectiveIslandEntry[TOTAL_SEARCH_GRID_SIZE * N_ISLANDS_TO_TRY_PER_CELL]);

    @Override
    public double compute(FunctionContext ctx) {
        double xWorld = ctx.blockX();
        double zWorld = ctx.blockZ();
        double x = xWorld;
        double z = zWorld;

        // TODO replace with vector-output noise
        x += sXNoise.sample(xWorld, zWorld);
        z += sZNoise.sample(xWorld, zWorld);

        x *= GRID_FREQUENCY;
        z *= GRID_FREQUENCY;

        int xBase = Mth.floor(x);
        int zBase = Mth.floor(z);
        float xInsideCell = (float)(x - xBase);
        float zInsideCell = (float)(z - zBase);

        // Populate prospective island points.
        ProspectiveIslandEntry[] prospectiveIslandEntries = prospectiveIslandEntriesThreadLocal.get();
        for (int dz = -TOTAL_SEARCH_RADIUS_BOUND; dz <= TOTAL_SEARCH_RADIUS_BOUND; dz++) {
            for (int dx = -TOTAL_SEARCH_RADIUS_BOUND; dx <= TOTAL_SEARCH_RADIUS_BOUND; dx++) {
                int cellIndex = (dz + TOTAL_SEARCH_RADIUS_BOUND) * TOTAL_SEARCH_DIAMETER_BOUND + (dx + TOTAL_SEARCH_RADIUS_BOUND);
                int xCellBase = xBase + dx;
                int zCellBase = zBase + dz;
                for (int i = 0; i < N_ISLANDS_TO_TRY_PER_CELL; i++) {
                    long hash = hash(xCellBase, zCellBase, i);
                    prospectiveIslandEntries[cellIndex * N_ISLANDS_TO_TRY_PER_CELL + i] = new ProspectiveIslandEntry(
                            ((hash >> 0) & 0xFFFF) * (1.0f / 0x10000),
                            ((hash >> 16) & 0xFFFF) * (1.0f / 0x10000),
                            (int) (hash >> 32)
                    );
                }
            }
        }

        // Add prospective island point falloff curves.
        float value = 0;
        for (int dzConsidering = -ISLAND_SIZE_SEARCH_BOUND; dzConsidering <= ISLAND_SIZE_SEARCH_BOUND; dzConsidering++) {
            for (int dxConsidering = -ISLAND_SIZE_SEARCH_BOUND; dxConsidering <= ISLAND_SIZE_SEARCH_BOUND; dxConsidering++) {
                int cellIndexConsidering = (dzConsidering + TOTAL_SEARCH_RADIUS_BOUND) * TOTAL_SEARCH_DIAMETER_BOUND + (dxConsidering + TOTAL_SEARCH_RADIUS_BOUND);
                for (int iConsidering = 0; iConsidering < N_ISLANDS_TO_TRY_PER_CELL; iConsidering++) {
                    ProspectiveIslandEntry entryConsidering = prospectiveIslandEntries[cellIndexConsidering * N_ISLANDS_TO_TRY_PER_CELL + iConsidering];

                    float xConsideringRelativeToMainCell = dxConsidering + entryConsidering.jitterX;
                    float zConsideringRelativeToMainCell = dzConsidering + entryConsidering.jitterZ;
                    float distSqToIslandCenter = Mth.square(xConsideringRelativeToMainCell - xInsideCell) + Mth.square(zConsideringRelativeToMainCell - zInsideCell);
                    if (distSqToIslandCenter >= ISLAND_SIZE_RELATIVE_PADDED * ISLAND_SIZE_RELATIVE_PADDED) continue;

                    int conflictSearchMinX = Math.max(-TOTAL_SEARCH_RADIUS_BOUND, Mth.floor(xConsideringRelativeToMainCell - (float)ISLAND_SEPARATION_RELATIVE));
                    int conflictSearchMinZ = Math.max(-TOTAL_SEARCH_RADIUS_BOUND, Mth.floor(zConsideringRelativeToMainCell - (float)ISLAND_SEPARATION_RELATIVE));
                    int conflictSearchMaxX = Math.min( TOTAL_SEARCH_RADIUS_BOUND, Mth.floor(xConsideringRelativeToMainCell + (float)ISLAND_SEPARATION_RELATIVE));
                    int conflictSearchMaxZ = Math.min( TOTAL_SEARCH_RADIUS_BOUND, Mth.floor(zConsideringRelativeToMainCell + (float)ISLAND_SEPARATION_RELATIVE));
                    boolean conflictFound = false;

                    // Search a radius
                    ConflictSearchLoop:
                    for (int dzAgainst = conflictSearchMinZ; dzAgainst <= conflictSearchMaxZ; dzAgainst++) {
                        for (int dxAgainst = conflictSearchMinX; dxAgainst <= conflictSearchMaxX; dxAgainst++) {
                            float xCellBaseRelativeToPointConsidering = dxAgainst - xConsideringRelativeToMainCell;
                            float zCellBaseRelativeToPointConsidering = dzAgainst - zConsideringRelativeToMainCell;

                            int cellIndexAgainst = (dzAgainst + TOTAL_SEARCH_RADIUS_BOUND) * TOTAL_SEARCH_DIAMETER_BOUND + (dxAgainst + TOTAL_SEARCH_RADIUS_BOUND);
                            for (int iAgainst = 0; iAgainst < N_ISLANDS_TO_TRY_PER_CELL; iAgainst++) {
                                ProspectiveIslandEntry entryAgainst = prospectiveIslandEntries[cellIndexAgainst * N_ISLANDS_TO_TRY_PER_CELL + iAgainst];

                                // Only yield to higher (or on the off-chance equal) ranks.
                                if (entryAgainst.rank < entryConsidering.rank) continue;

                                // Don't yield to oneself.
                                if (dxAgainst == dxConsidering && dzAgainst == dzConsidering && iConsidering == iAgainst) continue;

                                // Check the separation distance for a conflict.
                                float separationDistSq = Mth.square(xCellBaseRelativeToPointConsidering + entryAgainst.jitterX)
                                        + Mth.square(zCellBaseRelativeToPointConsidering + entryAgainst.jitterZ);
                                if (separationDistSq < ISLAND_SEPARATION_RELATIVE * ISLAND_SEPARATION_RELATIVE) {
                                    conflictFound = true;
                                    break ConflictSearchLoop;
                                }
                            }
                        }
                    }

                    if (!conflictFound) {
                        float distSqScaled = distSqToIslandCenter * (float)(1.0 / (ISLAND_SIZE_RELATIVE_PADDED * ISLAND_SIZE_RELATIVE_PADDED));
                        float falloff = cube(1 - distSqScaled); // (1-x²)³ metaball curve
                        value += falloff;
                    }

                }
            }
        }

        // Rescale to between TARGET_MIN_VALUE and TARGET_MAX_VALUE
        // (under normal circumstances; see below)
        value = value * (float)UNIT_INTERVAL_MULTIPLIER + (float)UNIT_INTERVAL_OFFSET;

        // Rarely, under certain configurations, the summed falloffs can slightly exceed the maximum value.
        // This can occur if the separation is small enough that one island's underwater falloff portion
        // extends to the center of another island.
        value = Math.min(value, (float)TARGET_MAX_VALUE);

        return value;
    }

    private long hash(int cellX, int cellZ, int i) {
        long hash = seed ^ (cellX * PRIME_X) ^ (cellZ * PRIME_Z) ^ (i * PRIME_I);
        hash *= HASH_MULTIPLIER;
        hash ^= hash >> 32;
        return hash;
    }

    private static float cube(float i) {
        return (i * i) * i; // nano-opt
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
        return TARGET_MIN_VALUE;
    }

    @Override
    public double maxValue() {
        return TARGET_MAX_VALUE;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
