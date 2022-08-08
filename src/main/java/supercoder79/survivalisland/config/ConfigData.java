package supercoder79.survivalisland.config;

import supercoder79.survivalisland.noise.OctaveNoiseRecipe;

public class ConfigData {
    public double islandSize = 32.0;
    public int islandSpacing = 240;
    public int islandSeperation = 120;

    public OctaveNoiseRecipe islandCutoffNoise = new OctaveNoiseRecipe(2, 50, 50, 0.65, 1.9, 1.2);
    public OctaveNoiseRecipe radiusModifyNoise = new OctaveNoiseRecipe(2, 24, 24, 14, 1.9, 1.9);
    public OctaveNoiseRecipe centerXShiftNoise = new OctaveNoiseRecipe(1, 24, 24, 14, 1.9, 1.9);
    public OctaveNoiseRecipe centerZShiftNoise = new OctaveNoiseRecipe(1, 24, 24, 14, 1.9, 1.9);

    public boolean hardcoreMode = false;
}
