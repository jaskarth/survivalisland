package supercoder79.survivalisland.layer;

import net.minecraft.world.biome.BiomeKeys;
import supercoder79.survivalisland.SurvivalIsland;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.layer.type.InitLayer;
import net.minecraft.world.biome.layer.util.LayerFactory;
import net.minecraft.world.biome.layer.util.LayerRandomnessSource;
import net.minecraft.world.biome.layer.util.LayerSampleContext;
import net.minecraft.world.biome.layer.util.LayerSampler;

public class LandDistributionLayer implements InitLayer {
    private final Registry<Biome> biomeRegistry;
    private Biome[] biomes;

    public LandDistributionLayer(Registry<Biome> biomeRegistry) {
        this.biomeRegistry = biomeRegistry;
    }

    public <R extends LayerSampler> LayerFactory<R> create(LayerSampleContext<R> context) {
        biomes = biomeRegistry.stream()
                .filter(p -> p.getCategory() != Biome.Category.NETHER && p.getCategory() != Biome.Category.THEEND && p.getCategory() != Biome.Category.RIVER && p.getCategory() != Biome.Category.NONE)
                .toArray(Biome[]::new);

        return () -> context.createSampler((x, z) -> {
            context.initSeed(x, z);
            return this.sample(context, x, z);
        });
    }

    @Override
    public int sample(LayerRandomnessSource context, int x, int z) {
        if (x == 0 && z == 0) {
            Biome b = biomeRegistry.get(new Identifier(SurvivalIsland.CONFIG.startingBiome));
            if (b == null) b = biomeRegistry.get(BiomeKeys.FOREST);
            return biomeRegistry.getRawId(b);
        }

        if (!SurvivalIsland.CONFIG.hardcoreMode) {
            if (context.nextInt(SurvivalIsland.CONFIG.islandRarity) == 0) {
                return biomeRegistry.getRawId(biomes[context.nextInt(biomes.length)]);
            }
        }

        return 0;
    }
}
