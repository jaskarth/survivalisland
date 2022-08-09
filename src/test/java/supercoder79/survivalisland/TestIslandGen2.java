package supercoder79.survivalisland;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import supercoder79.survivalisland.noise.OctaveNoise;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class TestIslandGen2 {
    public static void main(String[] args) {
        int size = 2048;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < size; x++) {
            if (x % Math.max(size / 16, 1) == 0) {
                System.out.println((x / (double)size) * 100 + "%");
            }

            for (int z = 0; z < size; z++) {
                double value = compute(new DensityFunction.SinglePointContext(x - size/2, 0, z - size/2));
                int color;
                if (value < 0) {
                    value /= -0.4;
                    color = getIntFromColor((int)Mth.lerp(value, 255, 10), (int)Mth.lerp(value, 255, 90), (int)Mth.lerp(value, 255, 180));
                }
                else {
                    color = getIntFromColor((int)Mth.lerp(value, 0, 127), (int)Mth.lerp(value, 127, 255), (int)Mth.lerp(value, 0, 127));
                }

                img.setRGB(x, z, color);
            }
        }

        Path p = Paths.get("build", "island.png");
        try {
            ImageIO.write(img, "png", p.toAbsolutePath().toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getIntFromColor(int red, int green, int blue) {
        red = Mth.clamp(red, 0, 255);
        green = Mth.clamp(green, 0, 255);
        blue = Mth.clamp(blue, 0, 255);

        red = (red << 16) & 0x00FF0000; //Shift red 16-bits and mask out other stuff
        green = (green << 8) & 0x0000FF00; //Shift green 8-bits and mask out other stuff
        blue = blue & 0x000000FF; //Mask out anything not blue.

        return 0xFF000000 | red | green | blue; //0xFF000000 for 100% Alpha. Bitwise OR everything together.
    }

    private static OctaveNoise circleNoise = new OctaveNoise(2, new Random(10), 50, 50, 0.65, 1.9, 1.2);
    private static OctaveNoise radiusNoise = new OctaveNoise(2, new Random(11), 24, 24, 14, 1.9, 1.9);
    private static OctaveNoise sXNoise = new OctaveNoise(1, new Random(101), 24, 24, 14, 1.9, 1.9);
    private static OctaveNoise sZNoise = new OctaveNoise(1, new Random(102), 24, 24, 14, 1.9, 1.9);

    private static final long seed = 7;

    private static final long PRIME_X = 0x5205402B9270C86FL;
    private static final long PRIME_Z = 0x5BCC226E9FA0BACBL;
    private static final long PRIME_I = 0x598CD327003817B5L;
    private static final long HASH_MULTIPLIER = 0x53A3F72DEEC546F5L;

    private static final double TARGET_MAX_VALUE = 1.0;
    private static final double TARGET_MIN_VALUE = -0.4;

    private static final double UNIT_INTERVAL_MULTIPLIER = TARGET_MAX_VALUE - TARGET_MIN_VALUE;
    private static final double UNIT_INTERVAL_OFFSET = TARGET_MIN_VALUE;
    private static final double UNIT_INTERVAL_VALUE_CREATING_A_ZERO_OUTPUT = -UNIT_INTERVAL_OFFSET / UNIT_INTERVAL_MULTIPLIER;
    private static final double PRE_CURVED_VALUE_CREATING_A_ZERO_OUTPUT = Math.sqrt(1 - Math.pow(UNIT_INTERVAL_VALUE_CREATING_A_ZERO_OUTPUT, 1.0 / 3.0)); // inverse of falloff=(1-dist²)³
    private static final double RADIUS_MULTIPLIER_TO_ACCOUNT_FOR_UNDERWATER_FALLOFF = 1.0 / PRE_CURVED_VALUE_CREATING_A_ZERO_OUTPUT;

    private static final double ISLAND_SIZE_ABSOLUTE = 32.0;//SurvivalIsland.CONFIG.islandSize;
    private static final double ISLAND_SIZE_ABSOLUTE_PADDED = ISLAND_SIZE_ABSOLUTE
            * RADIUS_MULTIPLIER_TO_ACCOUNT_FOR_UNDERWATER_FALLOFF;

    private static final int ISLAND_SEPARATION_ABSOLUTE = 280;//SurvivalIsland.CONFIG.islandSeperation;
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

    // Number of islands to try placing = asymptotic # possible per area in densest packing (not considering square)
    // It's not critical for this to be exactly this value, but this was simple enough.
    // For ISLAND_SEPARATION_ABSOLUTE * SQUARE_DIAGONAL the result is 2.
    private static final double PACKING_RATIO = 2 / Math.sqrt(3);
    private static final int N_ISLANDS_TO_TRY_PER_CELL = Mth.ceil(Mth.square(PACKING_RATIO * GRID_CELL_SIZE / ISLAND_SEPARATION_ABSOLUTE));

    private record ProspectiveIslandEntry(
            float jitterX, float jitterZ, int rank
    ) { }
    private static final ThreadLocal<ProspectiveIslandEntry[]> prospectiveIslandEntriesThreadLocal
            = ThreadLocal.withInitial(() -> new ProspectiveIslandEntry[TOTAL_SEARCH_GRID_SIZE * N_ISLANDS_TO_TRY_PER_CELL]);

    public static double compute(DensityFunction.FunctionContext ctx) {
        double xWorld = ctx.blockX();
        double zWorld = ctx.blockZ();
        double x = xWorld;
        double z = zWorld;

        // TODO replace with vector-output noise
        x += sXNoise.sample(x, z);
        z += sZNoise.sample(x, z);

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
                            ((hash      ) & 0xFFFF) * (1.0f / 0x10000),
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
                        float falloff = Mth.cube(1 - distSqScaled); // (1-dist²)³ metaball curve
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

    private static long hash(int cellX, int cellZ, int i) {
        long hash = seed ^ (cellX * PRIME_X) ^ (cellZ * PRIME_Z) ^ (i * PRIME_I);
        hash *= HASH_MULTIPLIER;
        hash ^= hash >> 32;
        return hash;
    }
}