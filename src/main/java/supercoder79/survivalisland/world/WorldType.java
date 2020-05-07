package supercoder79.survivalisland.world;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.BiFunction;

import com.google.common.collect.Maps;
import com.mojang.datafixers.Dynamic;
import net.minecraft.world.level.LevelGeneratorOptions;
import supercoder79.survivalisland.SurvivalIsland;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSourceConfig;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;
import net.minecraft.world.level.LevelGeneratorType;

public class WorldType<T extends ChunkGenerator<?>> {
    public static final Map<LevelGeneratorType, WorldType<?>> LGT_TO_WT_MAP = Maps.newHashMap();
    public static final Map<String, WorldType<?>> STR_TO_WT_MAP = Maps.newHashMap();

    public static LevelGeneratorType generatorType;
    public final WorldTypeChunkGeneratorFactory<T> chunkGenSupplier;

    public WorldType(String name, WorldTypeChunkGeneratorFactory<T> chunkGenSupplier) {
        Constructor<LevelGeneratorType> constructor = null;
        try {
            constructor = LevelGeneratorType.class.getDeclaredConstructor(int.class, String.class, BiFunction.class);
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        try {
            generatorType = constructor.newInstance(13, name, (BiFunction<LevelGeneratorType, Dynamic<?>, LevelGeneratorOptions>) LevelGenUtil::makeChunkGenerator);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            System.out.println("concarrn");
            e.printStackTrace();
        }
        generatorType.setCustomizable(false);

        this.chunkGenSupplier = chunkGenSupplier;

        if (generatorType == null) {
            throw new NullPointerException("An old world type has a null generator type: " + name + "!");
        }

        LGT_TO_WT_MAP.put(generatorType, this);
        STR_TO_WT_MAP.put(name, this);
    }

    public static final WorldType<OverworldChunkGenerator> SURVIVAL_ISLAND = new WorldType<>("survivalisland", (world) -> {
        OverworldChunkGeneratorConfig chunkGenConfig = new OverworldChunkGeneratorConfig();
        VanillaLayeredBiomeSourceConfig biomeSourceConfig = new VanillaLayeredBiomeSourceConfig(world.getSeed()).setGeneratorConfig(chunkGenConfig);

        return SurvivalIsland.WORLDGEN_TYPE.create(world, new IslandBiomeSource(biomeSourceConfig), chunkGenConfig);
    });

    public interface WorldTypeChunkGeneratorFactory<T extends ChunkGenerator<?>> {
        T create(World world);
    }
}