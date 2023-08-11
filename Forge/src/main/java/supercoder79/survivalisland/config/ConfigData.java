package supercoder79.survivalisland.config;

import supercoder79.survivalisland.noise.OctaveNoiseRecipe;
import supercoder79.survivalisland.util.FloatRange;

public class ConfigData {
    public double islandSize = 128.0;
    public int islandSeperation = 800;

    public FloatRange continentalTargetRangeA = new FloatRange(-0.25f, 0.7f);
    public FloatRange continentalTargetRangeB = new FloatRange(-1.0f, 1.4f);
    public double islandUnderwaterFalloffDistanceMultiplier = 9;

    public OctaveNoiseRecipe domainWarpNoise = new OctaveNoiseRecipe(1, 28, 24, 22, 1.732, 1.732);
    public OctaveNoiseRecipe rangeVariationNoise = new OctaveNoiseRecipe(2, 344, 24, 1.2, 1.732, 1.732);

    public boolean hardcoreMode = false;
}
