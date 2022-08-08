package supercoder79.survivalisland.noise;

import java.util.Random;

public final class OctaveNoise {
    private final OpenSimplexNoise[] octaves;
    private final double horizontalFrequency;
    private final double verticalFrequency;
    private final double amplitude;
    private final double lacunarity;
    private final double persistence;

    public OctaveNoise(int octaves, Random random, double horizontalFrequency, double verticalFrequency, double amplitude, double lacunarity, double persistence) {
        this.horizontalFrequency = horizontalFrequency;
        this.verticalFrequency = verticalFrequency;
        this.amplitude = amplitude;
        this.lacunarity = lacunarity;
        this.persistence = persistence;

        this.octaves = new OpenSimplexNoise[octaves];
        for (int i = 0; i < octaves; i++) {
            this.octaves[i] = new OpenSimplexNoise(random.nextLong());
        }
    }

    public double sample(double x, double z) {
        return this.sample(x, 0, z);
    }

    public double sample(double x, double y, double z) {
        double sum = 0;

        x /= this.horizontalFrequency;
        y /= this.verticalFrequency;
        z /= this.horizontalFrequency;

        double amplitude = this.amplitude;

        for (OpenSimplexNoise octave : this.octaves) {
            sum += octave.sample(x, y, z) * amplitude;

            amplitude /= this.persistence;

            x *= this.lacunarity;
            y *= this.lacunarity;
            z *= this.lacunarity;
        }

        return sum;
    }
}