package supercoder79.survivalisland.noise;

import net.minecraft.util.Mth;

public class IslandContinentalNoise {

    private static final long PRIME_X = 0x5205402B9270C86FL;
    private static final long PRIME_Z = 0x5BCC226E9FA0BACBL;
    private static final long PRIME_I = 0x598CD327003817B5L;
    private static final long HASH_MULTIPLIER = 0x53A3F72DEEC546F5L;

    // These values seemed to make things look good.
    private static final double GRID_CELL_SIZE_SPACING_MULTIPLIER = Math.sqrt(2);
    private final int ISLAND_PLACEMENT_ATTEMPT_COUNT_PER_CELL = 3;

    private final long seed;

    private final float targetMinValueA, targetMaxValueA;
    private final float targetMinValueB, targetMaxValueB;
    private final double minValue, maxValue;
    private final float islandCurveValueAtCoastUntunedUnmapped;

    private final double gridFrequency;
    private final float islandRadiusGridScalePadded;
    private final float islandSeparationDistanceGridScale;

    private final int totalSearchRadiusBound;
    private final int totalSearchDiameterBound;
    private final int totalSearchGridSize;
    private final int islandRadiusSearchBound;

    private final OctaveNoise domainWarpNoise;
    private final OctaveNoise rangeVariationNoise;

    private record ProspectiveIslandEntry(
            float jitterX, float jitterZ, int rank
    ) { }
    private final ThreadLocal<ProspectiveIslandEntry[]> prospectiveIslandEntriesThreadLocal;

    public IslandContinentalNoise(long seed,
                                  double islandRadius, double islandSeparationDistance,
                                  float targetMinValueA, float targetMaxValueA,
                                  float targetMinValueB, float targetMaxValueB,
                                  double underwaterFalloffDistanceRatioToRadius,
                                  OctaveNoise domainWarpNoise,
                                  OctaveNoise rangeVariationNoise
    ) {
        this.seed = seed;

        this.targetMinValueA = targetMinValueA;
        this.targetMaxValueA = targetMaxValueA;
        this.targetMinValueB = targetMinValueB;
        this.targetMaxValueB = targetMaxValueB;

        this.minValue = Math.min(targetMinValueA, targetMinValueB);
        this.maxValue = Math.max(targetMaxValueA, targetMaxValueB);

        this.domainWarpNoise = domainWarpNoise;
        this.rangeVariationNoise = rangeVariationNoise;

        double underwaterFalloffDistance = underwaterFalloffDistanceRatioToRadius * islandRadius;
        double totalFalloffDistance = underwaterFalloffDistance + islandRadius;

        double islandCoastCurveInput = islandRadius / totalFalloffDistance;
        double islandCurveValueAtCoastUntunedUnmappedBase = (1 - islandCoastCurveInput * islandCoastCurveInput);
        islandCurveValueAtCoastUntunedUnmapped = (float)cube(islandCurveValueAtCoastUntunedUnmappedBase);

        double gridCellSize = islandSeparationDistance * GRID_CELL_SIZE_SPACING_MULTIPLIER;
        gridFrequency = 1.0 / gridCellSize;
        double islandRadiusGridScalePadded = totalFalloffDistance * gridFrequency;
        this.islandRadiusGridScalePadded = (float)islandRadiusGridScalePadded;
        double islandSeparationDistanceGridScale = islandSeparationDistance * gridFrequency;
        this.islandSeparationDistanceGridScale = (float)islandSeparationDistanceGridScale;

        double totalSearchRadius = islandRadiusGridScalePadded + islandSeparationDistanceGridScale;
        totalSearchRadiusBound = Mth.ceil(totalSearchRadius);
        totalSearchDiameterBound = totalSearchRadiusBound * 2 + 1;
        totalSearchGridSize = totalSearchDiameterBound * totalSearchDiameterBound;
        islandRadiusSearchBound = Mth.ceil(islandRadiusGridScalePadded);

        prospectiveIslandEntriesThreadLocal =
                ThreadLocal.withInitial(() -> new ProspectiveIslandEntry[totalSearchGridSize * totalSearchGridSize]);
    }

    public double compute(double xWorld, double zWorld) {

        OpenSimplex2S.Vec3 displacement = new OpenSimplex2S.Vec3();
        domainWarpNoise.sampleVector3(xWorld, 0, zWorld, displacement);
        double x = xWorld + displacement.x;
        double z = zWorld + displacement.z;

        float rangeVariationNoiseValue = Mth.clamp(rangeVariationNoise.sample(x, z) * 0.5f + 0.5f, 0, 1);

        x *= gridFrequency;
        z *= gridFrequency;

        int xBase = Mth.floor(x);
        int zBase = Mth.floor(z);
        float xInsideCell = (float)(x - xBase);
        float zInsideCell = (float)(z - zBase);

        // Populate prospective island points.
        ProspectiveIslandEntry[] prospectiveIslandEntries = prospectiveIslandEntriesThreadLocal.get();
        for (int dz = -totalSearchRadiusBound; dz <= totalSearchRadiusBound; dz++) {
            for (int dx = -totalSearchRadiusBound; dx <= totalSearchRadiusBound; dx++) {
                int cellIndex = (dz + totalSearchRadiusBound) * totalSearchDiameterBound + (dx + totalSearchRadiusBound);
                int xCellBase = xBase + dx;
                int zCellBase = zBase + dz;
                for (int i = 0; i < ISLAND_PLACEMENT_ATTEMPT_COUNT_PER_CELL; i++) {
                    long hash = hash(xCellBase, zCellBase, i);
                    prospectiveIslandEntries[cellIndex * ISLAND_PLACEMENT_ATTEMPT_COUNT_PER_CELL + i] = new ProspectiveIslandEntry(
                            ((hash      ) & 0xFFFF) * (1.0f / 0x10000),
                            ((hash >> 16) & 0xFFFF) * (1.0f / 0x10000),
                            (int)(hash >> 32)
                    );
                }
            }
        }

        float targetMin = Mth.lerp(rangeVariationNoiseValue, targetMinValueA, targetMinValueB);
        float targetMax = Mth.lerp(rangeVariationNoiseValue, targetMaxValueA, targetMaxValueB);

        // Given that the curve will be mapped from [0. 1] to [targetMin, targetMax], which unmapped value will occur at the coastline?
        float tunedUnmappedCurveValueAtCoast = Mth.inverseLerp(0, targetMin, targetMax);

        // This tuning parameter bends the curve so that the coastline always maps to zero within that range.
        float islandCurveTuningValue = (1 - islandCurveValueAtCoastUntunedUnmapped) * tunedUnmappedCurveValueAtCoast /
                (islandCurveValueAtCoastUntunedUnmapped - tunedUnmappedCurveValueAtCoast);

        // Add prospective island point falloff curves.
        float value = 0;
        for (int dzConsidering = -islandRadiusSearchBound; dzConsidering <= islandRadiusSearchBound; dzConsidering++) {
            for (int dxConsidering = -islandRadiusSearchBound; dxConsidering <= islandRadiusSearchBound; dxConsidering++) {
                int cellIndexConsidering = (dzConsidering + totalSearchRadiusBound) * totalSearchDiameterBound + (dxConsidering + totalSearchRadiusBound);
                for (int iConsidering = 0; iConsidering < ISLAND_PLACEMENT_ATTEMPT_COUNT_PER_CELL; iConsidering++) {
                    ProspectiveIslandEntry entryConsidering = prospectiveIslandEntries[cellIndexConsidering * ISLAND_PLACEMENT_ATTEMPT_COUNT_PER_CELL + iConsidering];

                    float xConsideringRelativeToMainCell = dxConsidering + entryConsidering.jitterX;
                    float zConsideringRelativeToMainCell = dzConsidering + entryConsidering.jitterZ;
                    float distSqToIslandCenter = Mth.square(xConsideringRelativeToMainCell - xInsideCell) + Mth.square(zConsideringRelativeToMainCell - zInsideCell);
                    if (distSqToIslandCenter >= islandRadiusGridScalePadded * islandRadiusGridScalePadded) continue;

                    int conflictSearchMinX = Math.max(-totalSearchRadiusBound, Mth.floor(xConsideringRelativeToMainCell - islandSeparationDistanceGridScale));
                    int conflictSearchMinZ = Math.max(-totalSearchRadiusBound, Mth.floor(zConsideringRelativeToMainCell - islandSeparationDistanceGridScale));
                    int conflictSearchMaxX = Math.min( totalSearchRadiusBound, Mth.floor(xConsideringRelativeToMainCell + islandSeparationDistanceGridScale));
                    int conflictSearchMaxZ = Math.min( totalSearchRadiusBound, Mth.floor(zConsideringRelativeToMainCell + islandSeparationDistanceGridScale));
                    boolean conflictFound = false;

                    // Search a radius
                    ConflictSearchLoop:
                    for (int dzAgainst = conflictSearchMinZ; dzAgainst <= conflictSearchMaxZ; dzAgainst++) {
                        for (int dxAgainst = conflictSearchMinX; dxAgainst <= conflictSearchMaxX; dxAgainst++) {
                            float xCellBaseRelativeToPointConsidering = dxAgainst - xConsideringRelativeToMainCell;
                            float zCellBaseRelativeToPointConsidering = dzAgainst - zConsideringRelativeToMainCell;

                            int cellIndexAgainst = (dzAgainst + totalSearchRadiusBound) * totalSearchDiameterBound + (dxAgainst + totalSearchRadiusBound);
                            for (int iAgainst = 0; iAgainst < ISLAND_PLACEMENT_ATTEMPT_COUNT_PER_CELL; iAgainst++) {
                                ProspectiveIslandEntry entryAgainst = prospectiveIslandEntries[cellIndexAgainst * ISLAND_PLACEMENT_ATTEMPT_COUNT_PER_CELL + iAgainst];

                                // Only yield to higher (or on the off-chance equal) ranks.
                                if (entryAgainst.rank < entryConsidering.rank) continue;

                                // Don't yield to oneself.
                                if (dxAgainst == dxConsidering && dzAgainst == dzConsidering && iConsidering == iAgainst) continue;

                                // Check the separation distance for a conflict.
                                float separationDistSq = Mth.square(xCellBaseRelativeToPointConsidering + entryAgainst.jitterX)
                                        + Mth.square(zCellBaseRelativeToPointConsidering + entryAgainst.jitterZ);
                                if (separationDistSq < islandSeparationDistanceGridScale * islandSeparationDistanceGridScale) {
                                    conflictFound = true;
                                    break ConflictSearchLoop;
                                }
                            }
                        }
                    }

                    if (!conflictFound) {
                        float distSqScaled = distSqToIslandCenter * (float)(1.0 / (islandRadiusGridScalePadded * islandRadiusGridScalePadded));
                        float falloff = cube(1 - distSqScaled); // (1-dist²)³ metaball curve

                        // Drag the coast down to what will be zero after the min/max mapping.
                        falloff = islandCurveTuningValue * falloff / (islandCurveTuningValue + 1 - falloff);

                        value += falloff;
                    }

                }
            }
        }

        // Under certain configurations, it is possible for multiple overlapping island falloffs to push this value above 1.
        value = Math.min(1, value);

        return Mth.lerp(value, targetMin, targetMax);
    }

    private long hash(int cellX, int cellZ, int i) {
        long hash = seed ^ (cellX * PRIME_X) ^ (cellZ * PRIME_Z) ^ (i * PRIME_I);
        hash *= HASH_MULTIPLIER;
        hash ^= hash >> 32;
        return hash;
    }

    private static double cube(double t) {
        return (t * t) * t;
    }

    private static float cube(float t) {
        return (t * t) * t;
    }

    public double minValue() {
        return minValue;
    }

    public double maxValue() {
        return maxValue;
    }

}