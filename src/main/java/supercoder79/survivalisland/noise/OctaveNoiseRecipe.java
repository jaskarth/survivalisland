package supercoder79.survivalisland.noise;

import java.util.Random;

public record OctaveNoiseRecipe(int octaves, double horizontalFrequency, double verticalFrequency, double amplitude, double lacunarity, double persistence) {
    public OctaveNoise makeLive(Random random) {
        return new OctaveNoise(octaves, random, horizontalFrequency, verticalFrequency, amplitude, lacunarity, persistence);
    }
}
