package supercoder79.survivalisland.config;

import supercoder79.survivalisland.noise.OctaveNoiseRecipe;

public class ConfigData {
    public double islandSize = 34.0;
    public int islandSeperation = 180;

    //public OctaveNoiseRecipe islandCutoffNoise = new OctaveNoiseRecipe(2, 50, 50, 0.65, 1.9, 1.2);
    //public OctaveNoiseRecipe radiusModifyNoise = new OctaveNoiseRecipe(2, 24, 24, 14, 1.9, 1.9);
    public OctaveNoiseRecipe domainWarpNoise = new OctaveNoiseRecipe(1, 24, 24, 14, 1.9, 1.9);

    public boolean hardcoreMode = false;
}
