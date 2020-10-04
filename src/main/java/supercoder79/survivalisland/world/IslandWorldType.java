package supercoder79.survivalisland.world;

import net.minecraft.client.world.GeneratorType;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;

public class IslandWorldType extends GeneratorType {
    public IslandWorldType() {
        super("island");
        GeneratorType.VALUES.add(this);
    }

    @Override
    protected ChunkGenerator getChunkGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> settingsRegistry, long seed) {
        return new NoiseChunkGenerator(new IslandBiomeSource(biomeRegistry, seed), seed, () -> settingsRegistry.get(ChunkGeneratorSettings.OVERWORLD));
    }
}
