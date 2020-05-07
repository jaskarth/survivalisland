package supercoder79.survivalisland.world;

import com.mojang.datafixers.Dynamic;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSourceConfig;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;
import net.minecraft.world.level.LevelGeneratorOptions;
import net.minecraft.world.level.LevelGeneratorType;

public class LevelGenUtil {
    public static LevelGeneratorOptions makeChunkGenerator(LevelGeneratorType levelGeneratorType, Dynamic<?> dynamic) {
        OverworldChunkGeneratorConfig chunkGeneratorConfig = new OverworldChunkGeneratorConfig();
        return new LevelGeneratorOptions(levelGeneratorType, dynamic, (world) -> new OverworldChunkGenerator(world, new IslandBiomeSource(new VanillaLayeredBiomeSourceConfig(world.getSeed()).setGeneratorType(levelGeneratorType).setGeneratorConfig(chunkGeneratorConfig)), chunkGeneratorConfig));
    }
}
