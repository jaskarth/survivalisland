package supercoder79.survivalisland.noise;

import java.util.Random;

public final class OctaveNoiseRecipe {
    private int octaves;
    private double horizontalFrequency;
    private double verticalFrequency;
    private double amplitude;
    private double lacunarity;
    private double persistence;

    public OctaveNoiseRecipe(int octaves, double horizontalFrequency, double verticalFrequency, double amplitude, double lacunarity, double persistence) {
        this.octaves = octaves;
        this.horizontalFrequency = horizontalFrequency;
        this.verticalFrequency = verticalFrequency;
        this.amplitude = amplitude;
        this.lacunarity = lacunarity;
        this.persistence = persistence;
    }

    public OctaveNoise makeLive(Random random) {
        return new OctaveNoise(octaves, random, horizontalFrequency, verticalFrequency, amplitude, lacunarity, persistence);
    }
}
