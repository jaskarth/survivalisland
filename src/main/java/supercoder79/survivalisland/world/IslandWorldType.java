package supercoder79.survivalisland.world;

import net.minecraft.class_5317;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
import net.minecraft.world.gen.chunk.SurfaceChunkGenerator;

public class IslandWorldType extends class_5317 {
    public IslandWorldType() {
        super("island");
        class_5317.field_25052.add(this);
    }

    @Override
    protected ChunkGenerator method_29076(long l) {
        return new SurfaceChunkGenerator(new IslandBiomeSource(l), l, ChunkGeneratorType.Preset.OVERWORLD.getChunkGeneratorType());
    }
}
