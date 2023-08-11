package supercoder79.survivalisland.config;

import supercoder79.survivalisland.noise.OctaveNoiseRecipe2D;
import supercoder79.survivalisland.util.FloatRange;

public class ConfigData {
    public double islandSize = 128.0;
    public int islandSeperation = 800;

    public FloatRange continentalTargetRangeA = new FloatRange(-0.25f, 0.7f);
    public FloatRange continentalTargetRangeB = new FloatRange(-1.0f, 1.4f);
    public double islandUnderwaterFalloffDistanceMultiplier = 9;

    public OctaveNoiseRecipe2D domainWarpNoise = new OctaveNoiseRecipe2D(1, 28, 22, 1.732, 1.732);
    public OctaveNoiseRecipe2D rangeVariationNoise = new OctaveNoiseRecipe2D(2, 344, 1.2, 1.732, 1.732);

    public boolean hardcoreMode = false;
}
