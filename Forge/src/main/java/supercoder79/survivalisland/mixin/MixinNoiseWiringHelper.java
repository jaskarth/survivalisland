package supercoder79.survivalisland.mixin;

import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import supercoder79.survivalisland.world.util.SeedStealer;

@Mixin(targets = {"net/minecraft/world/level/levelgen/RandomState$1NoiseWiringHelper"})
public class MixinNoiseWiringHelper implements SeedStealer {
    private long seed;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void captureCtorForIsland(RandomState state, long l, boolean b, CallbackInfo ci) {
        this.seed = l;
    }

    @Override
    public long steal() {
        return this.seed;
    }
}
