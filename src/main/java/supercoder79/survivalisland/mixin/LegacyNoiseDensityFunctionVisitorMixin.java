package supercoder79.survivalisland.mixin;

import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import supercoder79.survivalisland.world.util.SeedStealer;

@Mixin(targets = "net.minecraft.world.gen.noise.NoiseConfig$1LegacyNoiseDensityFunctionVisitor")
public class LegacyNoiseDensityFunctionVisitorMixin implements SeedStealer {
    @Unique
    private long seed;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void captureCtorForIsland(ChunkGeneratorSettings chunkGeneratorSettings, RegistryEntryLookup noiseParametersLookup, long seed, CallbackInfo ci) {
        this.seed = seed;
    }

    @Override
    public long steal() {
        return this.seed;
    }
}
