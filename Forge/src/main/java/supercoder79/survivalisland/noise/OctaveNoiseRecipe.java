package supercoder79.survivalisland.noise;

import com.google.gson.annotations.SerializedName;
import net.minecraft.util.RandomSource;

public final class OctaveNoiseRecipe {
    private static final double CONFIG_FREQUENCY_COMPATIBILITY_RESCALE = 0.5;

    private int octaves;
    @SerializedName("horizontalFrequency") private double horizontalSpacing;
    @SerializedName("verticalFrequency") private double verticalSpacing;
    private double amplitude;
    private double lacunarity;
    @SerializedName("persistence") private double persistenceDivisor;

    public OctaveNoiseRecipe(int octaves, double horizontalSpacing, double verticalSpacing, double amplitude, double lacunarity, double persistenceDivisor) {
        this.octaves = octaves;
        this.horizontalSpacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
        this.amplitude = amplitude;
        this.lacunarity = lacunarity;
        this.persistenceDivisor = persistenceDivisor;
    }

    public OctaveNoise makeLive(RandomSource random) {
        return new OctaveNoise(octaves, random, CONFIG_FREQUENCY_COMPATIBILITY_RESCALE / horizontalSpacing, CONFIG_FREQUENCY_COMPATIBILITY_RESCALE / verticalSpacing, amplitude, lacunarity, (float)(1.0 / persistenceDivisor));
    }
}
