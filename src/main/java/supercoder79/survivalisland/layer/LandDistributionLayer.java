package supercoder79.survivalisland.layer;

import supercoder79.survivalisland.SurvivalIsland;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.layer.type.InitLayer;
import net.minecraft.world.biome.layer.util.LayerFactory;
import net.minecraft.world.biome.layer.util.LayerRandomnessSource;
import net.minecraft.world.biome.layer.util.LayerSampleContext;
import net.minecraft.world.biome.layer.util.LayerSampler;

public enum LandDistributionLayer implements InitLayer {
    INSTANCE;

    private Biome[] biomes;

    public <R extends LayerSampler> LayerFactory<R> create(LayerSampleContext<R> context) {
        biomes = Registry.BIOME.stream()
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
            Biome b = Registry.BIOME.get(new Identifier(SurvivalIsland.CONFIG.startingBiome));
            if (b == null) b = Biomes.FOREST;
            return Registry.BIOME.getRawId(b);
        }

        if (!SurvivalIsland.CONFIG.hardcoreMode) {
            if (context.nextInt(SurvivalIsland.CONFIG.islandRarity) == 0) {
                return Registry.BIOME.getRawId(biomes[context.nextInt(biomes.length)]);
            }
        }

        return 0;
    }
}
