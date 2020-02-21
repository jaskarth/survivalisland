package supercoder79.survivalisland.world;

import java.util.Map;

import com.google.common.collect.Maps;
import supercoder79.survivalisland.SurvivalIsland;
import supercoder79.survivalisland.mixin.AccessorLevelGeneratorType;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSourceConfig;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;
import net.minecraft.world.level.LevelGeneratorType;

public class WorldType<T extends ChunkGenerator<?>> {
    public static final Map<LevelGeneratorType, WorldType<?>> LGT_TO_WT_MAP = Maps.newHashMap();
    public static final Map<String, WorldType<?>> STR_TO_WT_MAP = Maps.newHashMap();

    public final LevelGeneratorType generatorType;
    public final WorldTypeChunkGeneratorFactory<T> chunkGenSupplier;

    public WorldType(String name, WorldTypeChunkGeneratorFactory<T> chunkGenSupplier) {
        this.generatorType = AccessorLevelGeneratorType.create(11, name);
        generatorType.setCustomizable(false);

        this.chunkGenSupplier = chunkGenSupplier;

        if (this.generatorType == null) {
            throw new NullPointerException("An old world type has a null generator type: " + name + "!");
        }

        LGT_TO_WT_MAP.put(generatorType, this);
        STR_TO_WT_MAP.put(name, this);
    }

    public static final WorldType<OverworldChunkGenerator> SURVIVAL_ISLAND = new WorldType<>("survivalisland", (world) -> {
        OverworldChunkGeneratorConfig chunkGenConfig = new OverworldChunkGeneratorConfig();
        VanillaLayeredBiomeSourceConfig biomeSourceConfig = new VanillaLayeredBiomeSourceConfig(world.getLevelProperties()).setGeneratorSettings(chunkGenConfig);

        return SurvivalIsland.WORLDGEN_TYPE.create(world, new IslandBiomeSource(biomeSourceConfig), chunkGenConfig);
    });

    public interface WorldTypeChunkGeneratorFactory<T extends ChunkGenerator<?>> {
        T create(World world);
    }
}