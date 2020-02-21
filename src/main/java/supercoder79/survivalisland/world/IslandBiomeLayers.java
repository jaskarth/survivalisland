package supercoder79.survivalisland.world;

import supercoder79.survivalisland.SurvivalIsland;
import supercoder79.survivalisland.layer.LandDistributionLayer;
import supercoder79.survivalisland.layer.SeperateIslandsLayer;
import net.minecraft.world.biome.layer.*;
import net.minecraft.world.biome.layer.type.ParentedLayer;
import net.minecraft.world.biome.layer.util.*;
import net.minecraft.world.biome.source.BiomeLayerSampler;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;
import net.minecraft.world.level.LevelGeneratorType;

import java.util.function.LongFunction;

public class IslandBiomeLayers {
    private static <T extends LayerSampler, C extends LayerSampleContext<T>> LayerFactory<T> stack(long seed, ParentedLayer layer, LayerFactory<T> parent, int count, LongFunction<C> contextProvider) {
        LayerFactory<T> layerFactory = parent;

        for(int i = 0; i < count; ++i) {
            layerFactory = layer.create(contextProvider.apply(seed + (long)i), layerFactory);
        }

        return layerFactory;
    }

    public static <T extends LayerSampler, C extends LayerSampleContext<T>> LayerFactory<T> build(LevelGeneratorType generatorType, OverworldChunkGeneratorConfig settings, LongFunction<C> contextProvider) {
        LayerFactory<T> layerFactory = LandDistributionLayer.INSTANCE.create(contextProvider.apply(1L));

        if (SurvivalIsland.CONFIG.seperateBiomes)
            layerFactory = SeperateIslandsLayer.INSTANCE.create(contextProvider.apply(3L), layerFactory);

        layerFactory = stack(2001L, ScaleLayer.NORMAL, layerFactory, SurvivalIsland.CONFIG.islandSize, contextProvider);

        if (SurvivalIsland.CONFIG.generateBeaches)
            layerFactory = AddEdgeBiomesLayer.INSTANCE.create(contextProvider.apply(4L), layerFactory);

        layerFactory = ScaleLayer.NORMAL.create(contextProvider.apply(7L), layerFactory);


        return layerFactory;
    }

    public static BiomeLayerSampler build(long seed, LevelGeneratorType generatorType, OverworldChunkGeneratorConfig settings) {
        LayerFactory<CachingLayerSampler> layerFactory = build(generatorType, settings, (salt) -> new CachingLayerContext(25, seed, salt));
        return new BiomeLayerSampler(layerFactory);
    }
}
