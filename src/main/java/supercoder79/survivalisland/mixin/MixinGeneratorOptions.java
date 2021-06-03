package supercoder79.survivalisland.mixin;

import com.google.common.base.MoreObjects;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import supercoder79.survivalisland.SurvivalIsland;
import supercoder79.survivalisland.config.ConfigSerializer;
import supercoder79.survivalisland.world.IslandBiomeSource;

import java.util.Properties;
import java.util.Random;

@Mixin(GeneratorOptions.class)
public class MixinGeneratorOptions {
    @Inject(method = "fromProperties", at = @At("HEAD"), cancellable = true)
    private static void injectIsland(DynamicRegistryManager dynamicRegistryManager, Properties properties, CallbackInfoReturnable<GeneratorOptions> cir) {
        if (properties.get("level-type") == null) {
            return;
        }

        if (properties.get("level-type").toString().trim().toLowerCase().equals("island")) {
            SurvivalIsland.CONFIG = ConfigSerializer.init();
            String seed = (String) MoreObjects.firstNonNull(properties.get("level-seed"), "");
            long l = new Random().nextLong();
            if (!seed.isEmpty()) {
                try {
                    long m = Long.parseLong(seed);
                    if (m != 0L) {
                        l = m;
                    }
                } catch (NumberFormatException exception) {
                    l = seed.hashCode();
                }
            }

            String generate_structures = (String)properties.get("generate-structures");
            boolean generateStructures = generate_structures == null || Boolean.parseBoolean(generate_structures);
            Registry<DimensionType> dimensionTypes = dynamicRegistryManager.get(Registry.DIMENSION_TYPE_KEY);
            Registry<Biome> biomes = dynamicRegistryManager.get(Registry.BIOME_KEY);
            Registry<ChunkGeneratorSettings> chunkGeneratorSettings = dynamicRegistryManager.get(Registry.CHUNK_GENERATOR_SETTINGS_KEY);
            SimpleRegistry<DimensionOptions> dimensionOptions = DimensionType.createDefaultDimensionOptions(dimensionTypes, biomes, chunkGeneratorSettings, l);
            cir.setReturnValue(new GeneratorOptions(l, generateStructures, false, GeneratorOptions.getRegistryWithReplacedOverworldGenerator(dimensionTypes, dimensionOptions, new NoiseChunkGenerator(new IslandBiomeSource(biomes, l), l, () -> chunkGeneratorSettings.get(ChunkGeneratorSettings.OVERWORLD)))));
        }
    }
}
