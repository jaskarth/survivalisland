package supercoder79.survivalisland.util;

import net.minecraft.util.math.MathHelper;

public record RoundedQueryShape(
        int cellXMin, int cellXMax,
        int cellZMin, int cellZMax,
        int baseCellX, int baseCellZ,
        float displacementX, float displacementZ,
        int subgridWidth, float circleRadius
) implements CachedQueryableGrid2D.QueryShape {

    public static RoundedQueryShape createForPoint(int baseCellX, int baseCellZ, float displacementX, float displacementZ, float circleRadius) {
        return new RoundedQueryShape(
                baseCellX - MathHelper.ceil(circleRadius - displacementX), baseCellX + MathHelper.floor(circleRadius + displacementX),
                baseCellZ - MathHelper.ceil(circleRadius - displacementZ), baseCellZ + MathHelper.floor(circleRadius + displacementZ),
                baseCellX, baseCellZ, displacementX, displacementZ, 0, circleRadius
        );
    }

    public static RoundedQueryShape createForSubgrid(int baseCellX, int baseCellZ, int subgridWidth, float extraCircleRadius) {
        return new RoundedQueryShape(
                baseCellX - MathHelper.ceil(extraCircleRadius), baseCellX + subgridWidth - 1 + MathHelper.ceil(extraCircleRadius),
                baseCellZ - MathHelper.ceil(extraCircleRadius), baseCellZ + subgridWidth - 1 + MathHelper.ceil(extraCircleRadius),
                baseCellX, baseCellZ, 0, 0, subgridWidth, extraCircleRadius
        );
    }

    public boolean intersectsRectangle(int xMin, int zMin, int xMax, int zMax) {
        float xMinAroundZero = xMin - baseCellX - subgridWidth - displacementX;
        float zMinAroundZero = zMin - baseCellZ - subgridWidth - displacementZ;
        float xMaxAroundZero = xMax - baseCellX - displacementX;
        float zMaxAroundZero = zMax - baseCellZ - displacementZ;
        float dx = MathHelper.clamp(0, xMinAroundZero, xMaxAroundZero);
        float dz = MathHelper.clamp(0, zMinAroundZero, zMaxAroundZero);
        return dx * dx + dz * dz < circleRadius * circleRadius;
    }
}
