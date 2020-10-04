package supercoder79.survivalisland.world;

import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import supercoder79.survivalisland.SurvivalIsland;
import supercoder79.survivalisland.layer.LandDistributionLayer;
import supercoder79.survivalisland.layer.SeperateIslandsLayer;
import net.minecraft.world.biome.layer.*;
import net.minecraft.world.biome.layer.type.ParentedLayer;
import net.minecraft.world.biome.layer.util.*;
import net.minecraft.world.biome.source.BiomeLayerSampler;

import java.util.function.LongFunction;

public class IslandBiomeLayers {
    private static <T extends LayerSampler, C extends LayerSampleContext<T>> LayerFactory<T> stack(long seed, ParentedLayer layer, LayerFactory<T> parent, int count, LongFunction<C> contextProvider) {
        LayerFactory<T> layerFactory = parent;

        for(int i = 0; i < count; ++i) {
            layerFactory = layer.create(contextProvider.apply(seed + (long)i), layerFactory);
        }

        return layerFactory;
    }

    public static <T extends LayerSampler, C extends LayerSampleContext<T>> LayerFactory<T> build(LongFunction<C> contextProvider, Registry<Biome> biomeRegistry) {
        LayerFactory<T> layerFactory = new LandDistributionLayer(biomeRegistry).create(contextProvider.apply(1L));

        if (SurvivalIsland.CONFIG.seperateBiomes) {
            layerFactory = SeperateIslandsLayer.INSTANCE.create(contextProvider.apply(3L), layerFactory);
        }

        layerFactory = AddDeepOceanLayer.INSTANCE.create(contextProvider.apply(50L), layerFactory);

        layerFactory = stack(2001L, ScaleLayer.NORMAL, layerFactory, SurvivalIsland.CONFIG.islandSize, contextProvider);

        if (SurvivalIsland.CONFIG.generateBeaches) {
            layerFactory = AddEdgeBiomesLayer.INSTANCE.create(contextProvider.apply(4L), layerFactory);
        }

        LayerFactory<T> tempLayer = OceanTemperatureLayer.INSTANCE.create(contextProvider.apply(5L));
        tempLayer = stack(2001L, ScaleLayer.NORMAL, tempLayer, 6, contextProvider);
        layerFactory = ApplyOceanTemperatureLayer.INSTANCE.create(contextProvider.apply(48L), layerFactory, tempLayer);

        return layerFactory;
    }

    public static BiomeLayerSampler build(long seed, Registry<Biome> biomeRegistry) {
        LayerFactory<CachingLayerSampler> layerFactory = build(salt -> new CachingLayerContext(25, seed, salt), biomeRegistry);
        return new BiomeLayerSampler(layerFactory);
    }
}
