package supercoder79.survivalisland.world;


import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraftforge.registries.DeferredRegister;

import java.util.Map;

public final class IslandWorldPreset {
    public static final DeferredRegister<WorldPreset> REGISTER = DeferredRegister.create(Registry.WORLD_PRESET_REGISTRY, "survivalisland");
    public static final ResourceKey<WorldPreset> ISLAND = of(new ResourceLocation("survivalisland", "survivalisland"));

    private static ResourceKey<WorldPreset> of(ResourceLocation id) {
        return ResourceKey.create(Registry.WORLD_PRESET_REGISTRY, id);
    }

    private static final Holder<DimensionType> theNetherDimensionType = BuiltinRegistries.DIMENSION_TYPE.getOrCreateHolderOrThrow(BuiltinDimensionTypes.NETHER);
    private static final Holder<NoiseGeneratorSettings> netherChunkGeneratorSettings = BuiltinRegistries.NOISE_GENERATOR_SETTINGS
            .getOrCreateHolderOrThrow(NoiseGeneratorSettings.NETHER);
    private static final LevelStem netherLevelStem = new LevelStem(
            theNetherDimensionType,
            new NoiseBasedChunkGenerator(
                    BuiltinRegistries.STRUCTURE_SETS,
                    BuiltinRegistries.NOISE,
                    MultiNoiseBiomeSource.Preset.NETHER.biomeSource(BuiltinRegistries.BIOME),
                    netherChunkGeneratorSettings
            )
    );
    private static final Holder<DimensionType> theEndDimensionType = BuiltinRegistries.DIMENSION_TYPE.getOrCreateHolderOrThrow(BuiltinDimensionTypes.END);
    private static final Holder<NoiseGeneratorSettings> endChunkGeneratorSettings = BuiltinRegistries.NOISE_GENERATOR_SETTINGS
            .getOrCreateHolderOrThrow(NoiseGeneratorSettings.END);
    private static final LevelStem endLevelStem = new LevelStem(
            theEndDimensionType,
            new NoiseBasedChunkGenerator(BuiltinRegistries.STRUCTURE_SETS, BuiltinRegistries.NOISE, new TheEndBiomeSource(BuiltinRegistries.BIOME), endChunkGeneratorSettings)
    );


    public static void init() {
        MultiNoiseBiomeSource biomes = MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(BuiltinRegistries.BIOME);
        Holder<NoiseGeneratorSettings> islandSettings = BuiltinRegistries.NOISE_GENERATOR_SETTINGS.getOrCreateHolderOrThrow(IslandNoiseGenSettings.ISLAND_SETTINGS);

        REGISTER.register("survivalisland", () -> createPreset(createChunkGenerator(
                new NoiseBasedChunkGenerator(
                        BuiltinRegistries.STRUCTURE_SETS,
                        BuiltinRegistries.NOISE,
                        biomes,
                        islandSettings
                )
        )));
    }

    private static WorldPreset createPreset(LevelStem stem) {
        return new WorldPreset(
                Map.of(LevelStem.OVERWORLD, stem, LevelStem.NETHER, netherLevelStem, LevelStem.END, endLevelStem)
        );
    }

    private static LevelStem createChunkGenerator(ChunkGenerator chunkGenerator) {
        return new LevelStem(BuiltinRegistries.DIMENSION_TYPE.getOrCreateHolderOrThrow(BuiltinDimensionTypes.OVERWORLD), chunkGenerator);
    }

//    @Override
//    protected ChunkGenerator getChunkGenerator(DynamicRegistryManager registryManager, long seed) {
//        return new EcotonesChunkGenerator(registryManager.get(Registry.STRUCTURE_SET_KEY), new EcotonesBiomeSource(registryManager.get(Registry.BIOME_KEY), seed), seed);
//    }
}
