package supercoder79.survivalisland.noise;

import net.minecraft.util.Mth;
import supercoder79.survivalisland.util.CachedQueryableGrid2D;
import supercoder79.survivalisland.util.RoundedQueryShape;

import javax.annotation.Nonnull;
import java.util.Objects;

public class IslandContinentalNoise {

    private static final long PRIME_X = 0x5205402B9270C86FL;
    private static final long PRIME_Z = 0x5BCC226E9FA0BACBL;
    private static final long PRIME_I = 0x598CD327003817B5L;
    private static final long HASH_MULTIPLIER = 0x53A3F72DEEC546F5L;

    private static final int CACHE_SUBGRID_WIDTH_MULTIPLIER_EXPONENT = 2;

    // These values seemed to make things look good.
    private static final double GRID_CELL_SIZE_SPACING_MULTIPLIER = Math.sqrt(2);
    private static final int ISLAND_PLACEMENT_ATTEMPT_COUNT_PER_CELL = 3;

    private final long seed;

    private final float targetMinValueA, targetMaxValueA;
    private final float targetMinValueB, targetMaxValueB;
    private final double minValue, maxValue;

    private final double gridFrequency;

    private final float islandContinentalRadiusAtGridScaleSq;
    private final float islandSeparationDistanceAtGridScale;
    private final float islandFalloffRadiusAtGridScale;

    private final CachedQueryableGrid2D<ProspectiveIslandEntry> prospectiveIslandGrid;
    private final CachedQueryableGrid2D<ConfirmedIslandEntry> confirmedIslandGrid;
    private final CachedQueryableGrid2D<RefinedIslandEntry> refinedIslandGrid;

    private final OctaveNoise domainWarpNoise;
    private final OctaveNoise rangeVariationNoise;
    private final float rangeVariationNoiseSmoothUnitRangeBoundTSquaredMultiplier;

    private record ProspectiveIslandEntry(
            float jitterX, float jitterZ, int rank
    ) { }
    private record ConfirmedIslandEntry(
            float jitterX, float jitterZ
    ) { }
    private record RefinedIslandEntry(
            float jitterX, float jitterZ, float falloffRadiusAtGridScaleSq, float inverseFalloffRadiusAtGridScaleSq, float islandCurveValueAtCoastUntunedUnmapped
    ) { }

    public IslandContinentalNoise(long seed,
                                  double islandRadius, double islandSeparationDistance,
                                  float targetMinValueA, float targetMaxValueA,
                                  float targetMinValueB, float targetMaxValueB,
                                  double underwaterFalloffDistanceRatioToRadius,
                                  @Nonnull OctaveNoise domainWarpNoise,
                                  @Nonnull OctaveNoise rangeVariationNoise
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
        this.rangeVariationNoiseSmoothUnitRangeBoundTSquaredMultiplier =
                computeTSquaredMultiplierForSmoothUnitRangeBoundCurve(rangeVariationNoise.amplitude());

        double gridCellSize = islandSeparationDistance * GRID_CELL_SIZE_SPACING_MULTIPLIER;
        gridFrequency = 1.0 / gridCellSize;

        double underwaterFalloffDistance = underwaterFalloffDistanceRatioToRadius * islandRadius;
        double totalFalloffDistance = underwaterFalloffDistance + islandRadius;

        islandContinentalRadiusAtGridScaleSq = (float)((islandRadius * islandRadius) / (gridCellSize * gridCellSize));

        double islandSeparationDistanceAtGridScale = islandSeparationDistance / gridCellSize;
        this.islandSeparationDistanceAtGridScale = (float)islandSeparationDistanceAtGridScale;
        int islandSeparationSearchSubgridWidthExponent = Mth.ceillog2(Mth.ceil(islandSeparationDistanceAtGridScale)) +
                CACHE_SUBGRID_WIDTH_MULTIPLIER_EXPONENT;

        double islandFalloffRadiusAtGridScale = totalFalloffDistance / gridCellSize;
        this.islandFalloffRadiusAtGridScale = (float)islandFalloffRadiusAtGridScale;
        int islandFalloffRadiusSearchSubgridWidthExponent = Mth.ceillog2(Mth.ceil(islandFalloffRadiusAtGridScale)) +
                CACHE_SUBGRID_WIDTH_MULTIPLIER_EXPONENT;

        prospectiveIslandGrid = new CachedQueryableGrid2D<>(
                ProspectiveIslandEntry.class,
                islandSeparationSearchSubgridWidthExponent,
                ISLAND_PLACEMENT_ATTEMPT_COUNT_PER_CELL,
                this::generateProspectiveIslandEntrySubgrid,
                64, 1024, 32
        );

        confirmedIslandGrid = new CachedQueryableGrid2D<>(
                ConfirmedIslandEntry.class,
                islandFalloffRadiusSearchSubgridWidthExponent,
                ISLAND_PLACEMENT_ATTEMPT_COUNT_PER_CELL,
                this::generateConfirmedIslandEntrySubgrid,
                36, 576, 32
        );

        refinedIslandGrid = new CachedQueryableGrid2D<>(
                RefinedIslandEntry.class,
                islandFalloffRadiusSearchSubgridWidthExponent,
                ISLAND_PLACEMENT_ATTEMPT_COUNT_PER_CELL,
                this::generateRefinedIslandEntrySubgrid,
                16, 256, 32
        );
    }

    public double compute(double xWorld, double zWorld) {
        OpenSimplex2S.Vec3 displacement = new OpenSimplex2S.Vec3();
        domainWarpNoise.sampleVector3(xWorld, 0, zWorld, displacement);
        double x = xWorld + displacement.x;
        double z = zWorld + displacement.z;

        float rangeVariationNoiseValue = rangeVariationNoise.sample(x, z);
        rangeVariationNoiseValue = smoothUnitRangeBoundCurve(rangeVariationNoiseValue,
                rangeVariationNoiseSmoothUnitRangeBoundTSquaredMultiplier);
        rangeVariationNoiseValue = rangeVariationNoiseValue * 0.5f + 0.5f;

        x *= gridFrequency;
        z *= gridFrequency;

        int xBase = Mth.floor(x);
        int zBase = Mth.floor(z);
        float xInsideCell = (float)(x - xBase);
        float zInsideCell = (float)(z - zBase);

        float targetMin = Mth.lerp(rangeVariationNoiseValue, targetMinValueA, targetMinValueB);
        float targetMax = Mth.lerp(rangeVariationNoiseValue, targetMaxValueA, targetMaxValueB);

        // Given that the curve will be mapped from [0, 1] to [targetMin, targetMax], which unmapped value will occur at the coastline?
        float tunedUnmappedCurveValueAtCoast = Mth.inverseLerp(0, targetMin, targetMax);

        float value = 0;

        CachedQueryableGrid2D<RefinedIslandEntry>.QueriedView refinedIslandEntryGridView = refinedIslandGrid.query(
                RoundedQueryShape.createForPoint(xBase, zBase, xInsideCell, zInsideCell, islandFalloffRadiusAtGridScale)
        );

        for (CachedQueryableGrid2D.GridCellInfo<RefinedIslandEntry> gridCellInfo : refinedIslandEntryGridView) {
            RefinedIslandEntry cellEntry = gridCellInfo.entry();
            if (cellEntry == null) continue;

            float undisplacedDeltaX = gridCellInfo.cellX() - xBase;
            float undisplacedDeltaZ = gridCellInfo.cellZ() - zBase;
            float displacementDeltaX = cellEntry.jitterX() - xInsideCell;
            float displacementDeltaZ = cellEntry.jitterZ() - zInsideCell;
            float deltaX = undisplacedDeltaX + displacementDeltaX;
            float deltaZ = undisplacedDeltaZ + displacementDeltaZ;

            float distSqToIslandCenter = Mth.square(deltaX) + Mth.square(deltaZ);
            if (distSqToIslandCenter >= cellEntry.falloffRadiusAtGridScaleSq) continue;

            float distSqScaled = distSqToIslandCenter * cellEntry.inverseFalloffRadiusAtGridScaleSq;
            float falloff = cube(1 - distSqScaled); // (1-dist²)³ metaball curve

            // Drag the coast down to what will be zero after the min/max mapping.
            // https://www.desmos.com/calculator/xzvzgstyen
            float tuningValueNumerator = (1 - cellEntry.islandCurveValueAtCoastUntunedUnmapped) * tunedUnmappedCurveValueAtCoast;
            float tuningValueDenominator = cellEntry.islandCurveValueAtCoastUntunedUnmapped - tunedUnmappedCurveValueAtCoast;
            falloff = tuningValueNumerator * falloff / (tuningValueNumerator + (1 - falloff) * tuningValueDenominator);

            value += falloff;
        }

        return Mth.lerp(value, targetMin, targetMax);
    }

    private void generateProspectiveIslandEntrySubgrid(
            CachedQueryableGrid2D.Vector subgridBasePosition, CachedQueryableGrid2D<ProspectiveIslandEntry>.SubgridGenerationEndpoint generationEndpoint) {
        generationEndpoint.populateCells((cellX, cellZ, cellEntryIndex) -> {
            long hash = hash(cellX, cellZ, cellEntryIndex);
            return new ProspectiveIslandEntry(
                    ((hash) & 0xFFFF) * (1.0f / 0x10000),
                    ((hash >> 16) & 0xFFFF) * (1.0f / 0x10000),
                    (int) (hash >> 32)
            );
        });
    }

    private void generateConfirmedIslandEntrySubgrid(
            CachedQueryableGrid2D.Vector subgridBasePosition, CachedQueryableGrid2D<ConfirmedIslandEntry>.SubgridGenerationEndpoint generationEndpoint) {

        CachedQueryableGrid2D<ProspectiveIslandEntry>.QueriedView prospectiveIslandEntryGridView = prospectiveIslandGrid.query(
                RoundedQueryShape.createForSubgrid(subgridBasePosition.x(), subgridBasePosition.z(), generationEndpoint.subgridWidth(), islandSeparationDistanceAtGridScale)
        );

        generationEndpoint.populateCells((prospectiveCellX, prospectiveCellZ, prospectiveCellEntryIndex) -> {
            ProspectiveIslandEntry prospectiveEntry = prospectiveIslandEntryGridView.get(prospectiveCellX, prospectiveCellZ, prospectiveCellEntryIndex);

            CachedQueryableGrid2D<ProspectiveIslandEntry>.QueriedView comparisonIslandEntryGridView = prospectiveIslandEntryGridView.subQuery(
                    RoundedQueryShape.createForPoint(prospectiveCellX, prospectiveCellZ, prospectiveEntry.jitterX(), prospectiveEntry.jitterZ(), islandSeparationDistanceAtGridScale)
            );

            for (CachedQueryableGrid2D.GridCellInfo<ProspectiveIslandEntry> comparisonCellInfo : comparisonIslandEntryGridView) {
                ProspectiveIslandEntry comparisonEntry = comparisonCellInfo.entry();

                // Only yield to higher (or on the off-chance equal) ranks.
                if (comparisonEntry.rank < prospectiveEntry.rank) continue;

                // Don't yield to oneself.
                if (comparisonCellInfo.cellX() == prospectiveCellX
                        && comparisonCellInfo.cellZ() == prospectiveCellZ
                        && comparisonCellInfo.cellEntryIndex() == prospectiveCellEntryIndex) continue;

                float undisplacedDeltaX = comparisonCellInfo.cellX() - prospectiveCellX;
                float undisplacedDeltaZ = comparisonCellInfo.cellZ() - prospectiveCellZ;
                float displacementDeltaX = comparisonEntry.jitterX() - prospectiveEntry.jitterX();
                float displacementDeltaZ = comparisonEntry.jitterZ() - prospectiveEntry.jitterZ();
                float deltaX = undisplacedDeltaX + displacementDeltaX;
                float deltaZ = undisplacedDeltaZ + displacementDeltaZ;

                // Check the separation distance for a conflict. If there was one, don't return an entry for this cell..
                float separationDistSq = Mth.square(deltaX) + Mth.square(deltaZ);
                if (separationDistSq < islandSeparationDistanceAtGridScale * islandSeparationDistanceAtGridScale) {
                    return null;
                }
            }

            return new ConfirmedIslandEntry(
                    prospectiveEntry.jitterX(),
                    prospectiveEntry.jitterZ()
            );
        });
    }

    private void generateRefinedIslandEntrySubgrid(
            CachedQueryableGrid2D.Vector subgridBasePosition, CachedQueryableGrid2D<RefinedIslandEntry>.SubgridGenerationEndpoint generationEndpoint) {

        CachedQueryableGrid2D<ConfirmedIslandEntry>.QueriedView confirmedIslandEntryGridView = confirmedIslandGrid.query(
                RoundedQueryShape.createForSubgrid(subgridBasePosition.x(), subgridBasePosition.z(), generationEndpoint.subgridWidth(), islandFalloffRadiusAtGridScale)
        );

        generationEndpoint.populateCells((confirmedCellX, confirmedCellZ, confirmedCellEntryIndex) -> {
            ConfirmedIslandEntry confirmedEntry = confirmedIslandEntryGridView.get(confirmedCellX, confirmedCellZ, confirmedCellEntryIndex);
            if (confirmedEntry == null) return null;

            CachedQueryableGrid2D<ConfirmedIslandEntry>.QueriedView comparisonIslandEntryGridView = confirmedIslandEntryGridView.subQuery(
                    RoundedQueryShape.createForPoint(confirmedCellX, confirmedCellZ, confirmedEntry.jitterX(), confirmedEntry.jitterZ(), islandFalloffRadiusAtGridScale)
            );

            // Choose the largest falloff radius (not exceeding islandFalloffRadiusAtGridScale)
            // that does not cross the center of another island.
            float islandFalloffRadiusSqAtGridScaleHere = islandFalloffRadiusAtGridScale * islandFalloffRadiusAtGridScale;
            for (CachedQueryableGrid2D.GridCellInfo<ConfirmedIslandEntry> comparisonCellInfo : comparisonIslandEntryGridView) {
                ConfirmedIslandEntry comparisonEntry = comparisonCellInfo.entry();
                if (comparisonEntry == null) continue;

                // Don't yield to oneself.
                if (comparisonCellInfo.cellX() == confirmedCellX
                        && comparisonCellInfo.cellZ() == confirmedCellZ
                        && comparisonCellInfo.cellEntryIndex() == confirmedCellEntryIndex) continue;

                float undisplacedDeltaX = comparisonCellInfo.cellX() - confirmedCellX;
                float undisplacedDeltaZ = comparisonCellInfo.cellZ() - confirmedCellZ;
                float displacementDeltaX = comparisonEntry.jitterX() - confirmedEntry.jitterX();
                float displacementDeltaZ = comparisonEntry.jitterZ() - confirmedEntry.jitterZ();
                float deltaX = undisplacedDeltaX + displacementDeltaX;
                float deltaZ = undisplacedDeltaZ + displacementDeltaZ;
                float separationDistSq = Mth.square(deltaX) + Mth.square(deltaZ);

                if (separationDistSq < islandFalloffRadiusSqAtGridScaleHere) {
                    islandFalloffRadiusSqAtGridScaleHere = separationDistSq;
                }
            }

            float islandCoastCurveInputSq = islandContinentalRadiusAtGridScaleSq / islandFalloffRadiusSqAtGridScaleHere;
            float islandCurveValueAtCoastUntunedUnmappedBase = (1 - islandCoastCurveInputSq);
            float islandCurveValueAtCoastUntunedUnmapped = cube(islandCurveValueAtCoastUntunedUnmappedBase);

            return new RefinedIslandEntry(
                    confirmedEntry.jitterX(),
                    confirmedEntry.jitterZ(),
                    islandFalloffRadiusSqAtGridScaleHere,
                    1.0f / islandFalloffRadiusSqAtGridScaleHere,
                    islandCurveValueAtCoastUntunedUnmapped
            );
        });
    }

    private long hash(int cellX, int cellZ, int i) {
        long hash = seed ^ (cellX * PRIME_X) ^ (cellZ * PRIME_Z) ^ (i * PRIME_I);
        hash *= HASH_MULTIPLIER;
        hash ^= hash >> 32;
        return hash;
    }

    private static float cube(float t) {
        return (t * t) * t;
    }

    // https://www.desmos.com/calculator/iknm6tdprh
    private static float computeTSquaredMultiplierForSmoothUnitRangeBoundCurve(float inputRange) {
        return 1 - 1 / (inputRange * inputRange);
    }
    private static float smoothUnitRangeBoundCurve(float t, float tSquaredMultiplier) {
        return t / Mth.sqrt(t * t * tSquaredMultiplier + 1);
    }

    public double minValue() {
        return minValue;
    }

    public double maxValue() {
        return maxValue;
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        IslandContinentalNoise that = (IslandContinentalNoise) object;
        return seed == that.seed && Float.compare(that.targetMinValueA, targetMinValueA) == 0 &&
                Float.compare(that.targetMaxValueA, targetMaxValueA) == 0 &&
                Float.compare(that.targetMinValueB, targetMinValueB) == 0 &&
                Float.compare(that.targetMaxValueB, targetMaxValueB) == 0 &&
                Float.compare(that.islandContinentalRadiusAtGridScaleSq, islandContinentalRadiusAtGridScaleSq) == 0 &&
                Double.compare(that.gridFrequency, gridFrequency) == 0 &&
                Float.compare(that.islandFalloffRadiusAtGridScale, islandFalloffRadiusAtGridScale) == 0 &&
                domainWarpNoise.equals(that.domainWarpNoise) && rangeVariationNoise.equals(that.rangeVariationNoise);
    }

    public int hashCode() {
        return Objects.hash(super.hashCode(), seed, targetMinValueA, targetMaxValueA, targetMinValueB, targetMaxValueB, islandContinentalRadiusAtGridScaleSq, gridFrequency, islandFalloffRadiusAtGridScale, domainWarpNoise, rangeVariationNoise);
    }
}
