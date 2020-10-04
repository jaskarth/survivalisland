package supercoder79.survivalisland.world;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryLookupCodec;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeLayerSampler;
import net.minecraft.world.biome.source.BiomeSource;

public class IslandBiomeSource extends BiomeSource {
    public static final Codec<IslandBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryLookupCodec.of(Registry.BIOME_KEY).forGetter(source -> source.biomeRegistry),
            Codec.LONG.fieldOf("seed").stable().forGetter(source -> source.seed))
            .apply(instance, instance.stable(IslandBiomeSource::new)));
    private final BiomeLayerSampler biomeSampler;
    private final long seed;
    private final Registry<Biome> biomeRegistry;

    public IslandBiomeSource(Registry<Biome> biomeRegistry, long seed) {
        super(ImmutableList.of());
        this.biomeRegistry = biomeRegistry;
        this.biomeSampler = IslandBiomeLayers.build(seed, biomeRegistry);
        this.seed = seed;
    }

    public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
        return this.biomeSampler.sample(this.biomeRegistry, biomeX, biomeZ);
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    public BiomeSource withSeed(long seed) {
        return new IslandBiomeSource(this.biomeRegistry, seed);
    }
}
