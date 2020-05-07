package supercoder79.survivalisland.world;

import java.util.function.Supplier;

import net.minecraft.world.IWorld;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;

public class WorldGeneratorType extends ChunkGeneratorType<OverworldChunkGeneratorConfig, OverworldChunkGenerator> {

    public WorldGeneratorType(boolean buffetScreen, Supplier<OverworldChunkGeneratorConfig> configSupplier) {
        super(null, buffetScreen, configSupplier);
    }

    public static void init() {
        // NO-OP
    }

    @Override
    public OverworldChunkGenerator create(IWorld world, BiomeSource biomeSource, OverworldChunkGeneratorConfig config) {
        return new OverworldChunkGenerator(world, biomeSource, config);
    }
}