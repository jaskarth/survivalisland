package supercoder79.survivalisland.noise;

import net.minecraft.util.math.random.Random;

public final class OctaveNoiseRecipe2D {
    private static final double CONFIG_FREQUENCY_STANDARDIZATION_RESCALE = 0.5;

    private int octaves;
    private double horizontalSpacing;
    private double amplitude;
    private double lacunarity;
    private double persistenceDivisor;

    public OctaveNoiseRecipe2D(int octaves, double horizontalSpacing, double amplitude, double lacunarity, double persistenceDivisor) {
        this.octaves = octaves;
        this.horizontalSpacing = horizontalSpacing;
        this.amplitude = amplitude;
        this.lacunarity = lacunarity;
        this.persistenceDivisor = persistenceDivisor;
    }

    public OctaveNoise makeLive(Random random) {
        return new OctaveNoise(octaves, random, CONFIG_FREQUENCY_STANDARDIZATION_RESCALE / horizontalSpacing, 0.0, amplitude, lacunarity, (float)(1.0 / persistenceDivisor));
    }
}
