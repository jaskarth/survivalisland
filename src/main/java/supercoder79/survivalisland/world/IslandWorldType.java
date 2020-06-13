package supercoder79.survivalisland.world;

import net.minecraft.client.world.GeneratorType;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
import net.minecraft.world.gen.chunk.SurfaceChunkGenerator;

public class IslandWorldType extends GeneratorType {
    public IslandWorldType() {
        super("island");
        GeneratorType.VALUES.add(this);
    }

    @Override
    protected ChunkGenerator method_29076(long l) {
        return new SurfaceChunkGenerator(new IslandBiomeSource(l), l, ChunkGeneratorType.Preset.OVERWORLD.getChunkGeneratorType());
    }
}
