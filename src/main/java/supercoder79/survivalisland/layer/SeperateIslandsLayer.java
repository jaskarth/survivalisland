package supercoder79.survivalisland.layer;

import net.minecraft.world.biome.layer.type.CrossSamplingLayer;
import net.minecraft.world.biome.layer.util.LayerRandomnessSource;

public enum SeperateIslandsLayer implements CrossSamplingLayer {
    INSTANCE;

    @Override
    public int sample(LayerRandomnessSource context, int n, int e, int s, int w, int center) {
        if (!(center == 0)) {
            if (!(n == 0) || !(e == 0) || !(s == 0) || !(w == 0)) return 0;
        }
        return center;
    }
}