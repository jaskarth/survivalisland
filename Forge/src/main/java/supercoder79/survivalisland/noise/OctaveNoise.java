package supercoder79.survivalisland.noise;

import net.minecraft.util.RandomSource;

public final class OctaveNoise {
    private static final double IDEAL_SLICE_OFFSET = Math.sqrt(3) / 6.0;

    private final long[] octaveSeeds;
    private final double horizontalFrequency;
    private final double verticalFrequency;
    private final float amplitude;
    private final double lacunarity;
    private final float persistence;

    public OctaveNoise(int octaves, RandomSource random, double horizontalFrequency, double verticalFrequency, double amplitude, double lacunarity, float persistence) {
        this.horizontalFrequency = Double.isFinite(horizontalFrequency) ? horizontalFrequency : 0;
        this.verticalFrequency = Double.isFinite(verticalFrequency) ? verticalFrequency : 0;
        this.amplitude = (float)amplitude;
        this.lacunarity = lacunarity;
        this.persistence = persistence;

        this.octaveSeeds = new long[octaves];
        for (int i = 0; i < octaves; i++) {
            this.octaveSeeds[i] = random.nextLong();
        }
    }

    public float sample(double x, double z) {
        return sample(x, IDEAL_SLICE_OFFSET, z);
    }

    public float sample(double x, double y, double z) {
        float value = 0;

        x *= this.horizontalFrequency;
        y *= this.verticalFrequency;
        z *= this.horizontalFrequency;

        float amplitude = this.amplitude;

        for (long octaveSeed : this.octaveSeeds) {
            value += OpenSimplex2S.noise3_ImproveXZ(octaveSeed, x, y, z) * amplitude;

            amplitude *= this.persistence;

            x *= this.lacunarity;
            y *= this.lacunarity;
            z *= this.lacunarity;
        }

        return value;
    }

    public void sampleVector3(double x, double z, OpenSimplex2S.Vec3 destination) {
        sampleVector3(x, IDEAL_SLICE_OFFSET, z, destination);
    }

    public void sampleVector3(double x, double y, double z, OpenSimplex2S.Vec3 destination) {
        float resultX = 0;
        float resultY = 0;
        float resultZ = 0;

        x *= this.horizontalFrequency;
        y *= this.verticalFrequency;
        z *= this.horizontalFrequency;

        float amplitude = this.amplitude;

        for (long octaveSeed : this.octaveSeeds) {
            OpenSimplex2S.vectorValuedNoise3_ImproveXZ(octaveSeed, x, y, z, destination);
            resultX += destination.x * amplitude;
            resultY += destination.y * amplitude;
            resultZ += destination.z * amplitude;

            amplitude *= this.persistence;

            x *= this.lacunarity;
            y *= this.lacunarity;
            z *= this.lacunarity;
        }

        destination.set(resultX, resultY, resultZ);
    }
}