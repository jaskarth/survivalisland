package supercoder79.survivalisland.util;

import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Iterator;

public class CachedQueryableGrid2D<TEntry> {

    private final int subgridWidthExponent;
    private final int subgridWidth;
    private final int entryCountPerCell;
    private final Class<TEntry> entryClass;
    private final ConcurrentLinkedHashCache<Vector, TEntry[]> subgridCache;
    private final SubgridGenerator<TEntry> subgridGenerator;

    public CachedQueryableGrid2D(Class<TEntry> entryClass, int subgridWidthExponent, int entryCountPerCell, SubgridGenerator<TEntry> subgridGenerator, int subgridCacheMinEntries, int subgridCacheMaxEntries, int subgridCachePruningFactor) {
        this.entryClass = entryClass;
        this.subgridWidthExponent = subgridWidthExponent;
        this.subgridWidth = 1 << subgridWidthExponent;
        this.entryCountPerCell = entryCountPerCell;
        this.subgridCache = new ConcurrentLinkedHashCache<>(subgridCacheMinEntries, subgridCacheMaxEntries, subgridCachePruningFactor);
        this.subgridGenerator = subgridGenerator;
    }

    public QueriedView query(QueryShape queryShape) {
        return new QueriedView(queryShape);
    }

    public class QueriedView implements Iterable<GridCellInfo<TEntry>> {

        private final TEntry[][] localGridOfSubgrids;
        private final QueryShape queryShape;
        private final int localGridOfSubgridsSizeX;
        private final int localGridOfSubgridsSizeZ;
        private final int firstSubgridBaseX;
        private final int firstSubgridBaseZ;

        private final int localGridOfSubgridsIndexStartX;
        private final int localGridOfSubgridsIndexStartZ;
        private final int localGridOfSubgridsIndexEndX;
        private final int localGridOfSubgridsIndexEndZ;

        private QueriedView(QueryShape queryShape) {
            this.queryShape = queryShape;

            firstSubgridBaseX     = queryShape.cellXMin() & -subgridWidth;
            firstSubgridBaseZ     = queryShape.cellZMin() & -subgridWidth;
            int lastSubgridBaseX  = queryShape.cellXMax() & -subgridWidth;
            int lastSubgridBaseZ  = queryShape.cellZMax() & -subgridWidth;

            localGridOfSubgridsIndexStartX = 0;
            localGridOfSubgridsIndexStartZ = 0;
            localGridOfSubgridsIndexEndX = (lastSubgridBaseX - firstSubgridBaseX) >> subgridWidthExponent;
            localGridOfSubgridsIndexEndZ = (lastSubgridBaseZ - firstSubgridBaseZ) >> subgridWidthExponent;

            localGridOfSubgridsSizeX = localGridOfSubgridsIndexEndX + 1;
            localGridOfSubgridsSizeZ = localGridOfSubgridsIndexEndZ + 1;
            localGridOfSubgrids = (TEntry[][]) Array.newInstance(entryClass.arrayType(), localGridOfSubgridsSizeX * localGridOfSubgridsSizeZ);

            for (
                    int localGridOfSubgridsIndexZ = 0, localGridOfSubgridsIndex = 0;
                    localGridOfSubgridsIndexZ < localGridOfSubgridsSizeZ;
                    localGridOfSubgridsIndexZ++) {
                for (
                        int localGridOfSubgridsIndexX = 0;
                        localGridOfSubgridsIndexX < localGridOfSubgridsSizeX;
                        localGridOfSubgridsIndexX++, localGridOfSubgridsIndex++) {
                    int gridBaseX = firstSubgridBaseX + (localGridOfSubgridsIndexX << subgridWidthExponent);
                    int gridBaseZ = firstSubgridBaseZ + (localGridOfSubgridsIndexZ << subgridWidthExponent);

                    if (!queryShape.intersectsRectangle(gridBaseX, gridBaseZ, gridBaseX + subgridWidth, gridBaseZ + subgridWidth)) {
                        continue;
                    }

                    localGridOfSubgrids[localGridOfSubgridsIndex] = subgridCache.computeIfAbsent(new Vector(gridBaseX, gridBaseZ), position -> {
                        TEntry[] subgrid = (TEntry[]) Array.newInstance(entryClass, entryCountPerCell << (2 * subgridWidthExponent));
                        subgridGenerator.generate(position, new SubgridGenerationEndpoint(position.x(), position.z(), subgrid));
                        return subgrid;
                    });
                }
            }
        }

        private QueriedView(QueriedView original, QueryShape innerQueryShape) {
            this.localGridOfSubgrids = original.localGridOfSubgrids;
            this.localGridOfSubgridsSizeX = original.localGridOfSubgridsSizeX;
            this.localGridOfSubgridsSizeZ = original.localGridOfSubgridsSizeZ;
            this.firstSubgridBaseX = original.firstSubgridBaseX;
            this.firstSubgridBaseZ = original.firstSubgridBaseZ;

            this.queryShape = innerQueryShape;

            int newFirstSubgridBaseX = queryShape.cellXMin() & -subgridWidth;
            int newFirstSubgridBaseZ = queryShape.cellZMin() & -subgridWidth;
            int newLastSubgridBaseX  = queryShape.cellXMax() & -subgridWidth;
            int newLastSubgridBaseZ  = queryShape.cellZMax() & -subgridWidth;

            this.localGridOfSubgridsIndexStartX = (newFirstSubgridBaseX - firstSubgridBaseX) >> subgridWidthExponent;
            this.localGridOfSubgridsIndexStartZ = (newFirstSubgridBaseZ - firstSubgridBaseZ) >> subgridWidthExponent;
            this.localGridOfSubgridsIndexEndX   = (newLastSubgridBaseX  - firstSubgridBaseX) >> subgridWidthExponent;
            this.localGridOfSubgridsIndexEndZ   = (newLastSubgridBaseZ  - firstSubgridBaseZ) >> subgridWidthExponent;
        }

        public QueriedView subQuery(QueryShape innerQueryShape) {
            return new QueriedView(this, innerQueryShape);
        }

        public TEntry get(int cellX, int cellZ, int entryIndex) {
            int viewCellX = cellX - firstSubgridBaseX;
            int viewCellZ = cellZ - firstSubgridBaseZ;

            int localGridOfSubgridsIndexX = viewCellX >> subgridWidthExponent;
            int localGridOfSubgridsIndexZ = viewCellZ >> subgridWidthExponent;
            int localGridOfSubgridsIndex = localGridOfSubgridsIndexZ * localGridOfSubgridsSizeX + localGridOfSubgridsIndexX;

            int subgridCellX = cellX & (subgridWidth - 1);
            int subgridCellZ = cellZ & (subgridWidth - 1);
            int cellIndex = ((subgridCellZ << subgridWidthExponent) | subgridCellX) * entryCountPerCell + entryIndex;

            return localGridOfSubgrids[localGridOfSubgridsIndex][cellIndex];
        }

        @NotNull
        @Override
        public Iterator<GridCellInfo<TEntry>> iterator() {
            return new CellEntryIterator();
        }

        private class CellEntryIterator implements Iterator<GridCellInfo<TEntry>> {
            private int cellEntryIndex;
            private int cellX;
            private int cellZ;
            private int subgridCellX;
            private int subgridCellZ;
            private int subgridCellIndex;
            private int localGridOfSubgridsIndexX;
            private int localGridOfSubgridsIndexZ;
            private int subgridCellStartX;
            private int subgridCellStartZ;
            private int subgridCellEndX;
            private int subgridCellEndZ;
            TEntry[] subgrid;

            public CellEntryIterator() {
                cellEntryIndex = 0;
                localGridOfSubgridsIndexX = localGridOfSubgridsIndexStartX;
                localGridOfSubgridsIndexZ = localGridOfSubgridsIndexStartZ;
                updateSubgrid();
                skipCellsOutOfRange();
            }

            @Override
            public boolean hasNext() {
                return (localGridOfSubgridsIndexZ <= localGridOfSubgridsIndexEndZ);
            }

            @Override
            public GridCellInfo<TEntry> next() {
                GridCellInfo<TEntry> result = new GridCellInfo<>(cellX, cellZ, cellEntryIndex, subgrid[subgridCellIndex]);
                updateIndex();
                skipCellsOutOfRange();
                return result;
            }

            private void skipCellsOutOfRange() {
                while (hasNext() && !queryShape.intersectsRectangle(cellX, cellZ, cellX + 1, cellZ + 1)) {
                    updateIndex();
                }
            }

            private void updateIndex() {
                subgridCellIndex++;

                cellEntryIndex++;
                if (cellEntryIndex < entryCountPerCell) return;
                cellEntryIndex = 0;

                subgridCellX++;
                cellX++;
                if (subgridCellX <= subgridCellEndX) return;
                cellX -= (subgridCellX - subgridCellStartX);
                subgridCellX = subgridCellStartX;

                subgridCellZ++;
                cellZ++;
                if (subgridCellZ <= subgridCellEndZ) {
                    subgridCellIndex = ((subgridCellZ << subgridWidthExponent) | subgridCellX) * entryCountPerCell;
                    return;
                }
                cellZ -= (subgridCellZ - subgridCellStartZ);
                subgridCellZ = subgridCellStartZ;

                do {
                    localGridOfSubgridsIndexX++;
                    if (localGridOfSubgridsIndexX <= localGridOfSubgridsIndexEndX) {
                        updateSubgrid();
                        continue;
                    }
                    localGridOfSubgridsIndexX = localGridOfSubgridsIndexStartX;

                    localGridOfSubgridsIndexZ++;
                    if (localGridOfSubgridsIndexZ <= localGridOfSubgridsIndexEndZ) {
                        updateSubgrid();
                        continue;
                    }
                    break;
                } while (subgrid == null);
            }

            private void updateSubgrid() {
                int localGridOfSubgridsOffsetX = localGridOfSubgridsIndexX << subgridWidthExponent;
                int localGridOfSubgridsOffsetZ = localGridOfSubgridsIndexZ << subgridWidthExponent;
                int subgridBaseX = firstSubgridBaseX + localGridOfSubgridsOffsetX;
                int subgridBaseZ = firstSubgridBaseZ + localGridOfSubgridsOffsetZ;

                subgridCellStartX = MathHelper.clamp(queryShape.cellXMin() - subgridBaseX, 0, subgridWidth - 1);
                subgridCellStartZ = MathHelper.clamp(queryShape.cellZMin() - subgridBaseZ, 0, subgridWidth - 1);
                subgridCellEndX   = MathHelper.clamp(queryShape.cellXMax() - subgridBaseX, 0, subgridWidth - 1);
                subgridCellEndZ   = MathHelper.clamp(queryShape.cellZMax() - subgridBaseZ, 0, subgridWidth - 1);

                subgridCellX = subgridCellStartX;
                subgridCellZ = subgridCellStartZ;
                cellX = subgridBaseX | subgridCellX;
                cellZ = subgridBaseZ | subgridCellZ;
                subgridCellIndex = ((subgridCellZ << subgridWidthExponent) | subgridCellX) * entryCountPerCell;

                int localGridOfSubgridsIndex = localGridOfSubgridsIndexZ * localGridOfSubgridsSizeX + localGridOfSubgridsIndexX;
                subgrid = localGridOfSubgrids[localGridOfSubgridsIndex];
            }
        }
    }

    public int subgridWidthExponent() {
        return subgridWidthExponent;
    }

    public int subgridWidth() {
        return subgridWidth;
    }

    public int entryCountPerCell() {
        return entryCountPerCell;
    }

    public interface QueryShape {
        int cellXMin();
        int cellXMax();
        int cellZMin();
        int cellZMax();
        boolean intersectsRectangle(int xMin, int zMin, int xMax, int zMax);
    }

    public record Vector(int x, int z) { }

    public record GridCellInfo<TEntry>(int cellX, int cellZ, int cellEntryIndex, TEntry entry) { }

    @FunctionalInterface
    public interface SubgridGenerator<TEntry> {
        void generate(Vector subgridBasePosition, CachedQueryableGrid2D<TEntry>.SubgridGenerationEndpoint generationEndpoint);
    }

    @FunctionalInterface
    public interface CellEntryGenerator<TEntry> {
        TEntry generate(int cellX, int cellZ, int cellEntryIndex);
    }

    public class SubgridGenerationEndpoint {
        private final int subgridBaseX;
        private final int subgridBaseZ;
        private final TEntry[] subgrid;

        public SubgridGenerationEndpoint(int subgridBaseX, int subgridBaseZ, TEntry[] subgrid) {
            this.subgridBaseX = subgridBaseX;
            this.subgridBaseZ = subgridBaseZ;
            this.subgrid = subgrid;
        }

        public int subgridWidthExponent() {
            return subgridWidthExponent;
        }

        public int subgridWidth() {
            return subgridWidth;
        }

        public int entryCountPerCell() {
            return entryCountPerCell;
        }

        public void populateCells(CellEntryGenerator<TEntry> cellEntryGenerator) {
            for (int subgridCellZ = 0, cellIndex = 0; subgridCellZ < subgridWidth; subgridCellZ++) {
                for (int subgridCellX = 0; subgridCellX < subgridWidth; subgridCellX++) {
                    int cellX = subgridBaseX | subgridCellX;
                    int cellZ = subgridBaseZ | subgridCellZ;
                    for (int cellEntryIndex = 0; cellEntryIndex < entryCountPerCell; cellEntryIndex++, cellIndex++) {
                        subgrid[cellIndex] = cellEntryGenerator.generate(cellX, cellZ, cellEntryIndex);
                    }
                }
            }
        }
    }
}
