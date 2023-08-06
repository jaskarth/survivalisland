package supercoder79.survivalisland.util;

import net.minecraft.util.Mth;

public record RoundedQueryShape(
        int cellXMin, int cellXMax,
        int cellZMin, int cellZMax,
        int baseCellX, int baseCellZ,
        float displacementX, float displacementZ,
        int subgridWidth, float circleRadius
) implements CachedQueryableGrid2D.QueryShape {

    public static RoundedQueryShape createForPoint(int baseCellX, int baseCellZ, float displacementX, float displacementZ, float circleRadius) {
        return new RoundedQueryShape(
                baseCellX - Mth.ceil(circleRadius - displacementX), baseCellX + Mth.floor(circleRadius + displacementX),
                baseCellZ - Mth.ceil(circleRadius - displacementZ), baseCellZ + Mth.floor(circleRadius + displacementZ),
                baseCellX, baseCellZ, displacementX, displacementZ, 0, circleRadius
        );
    }

    public static RoundedQueryShape createForSubgrid(int baseCellX, int baseCellZ, int subgridWidth, float extraCircleRadius) {
        return new RoundedQueryShape(
                baseCellX - Mth.ceil(extraCircleRadius), baseCellX + subgridWidth - 1 + Mth.ceil(extraCircleRadius),
                baseCellZ - Mth.ceil(extraCircleRadius), baseCellZ + subgridWidth - 1 + Mth.ceil(extraCircleRadius),
                baseCellX, baseCellZ, 0, 0, subgridWidth, extraCircleRadius
        );
    }

    public boolean intersectsRectangle(int xMin, int zMin, int xMax, int zMax) {
        float xMinAroundZero = xMin - baseCellX - subgridWidth - displacementX;
        float zMinAroundZero = zMin - baseCellZ - subgridWidth - displacementZ;
        float xMaxAroundZero = xMax - baseCellX - displacementX;
        float zMaxAroundZero = zMax - baseCellZ - displacementZ;
        float dx = Mth.clamp(0, xMinAroundZero, xMaxAroundZero);
        float dz = Mth.clamp(0, zMinAroundZero, zMaxAroundZero);
        return dx * dx + dz * dz < circleRadius * circleRadius;
    }
}
