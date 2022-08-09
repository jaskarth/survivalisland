package supercoder79.survivalisland.mixin;

import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseRouterData.class)
public interface NoiseRouterDataAccessor {
    @Invoker
    static NoiseRouter callOverworld(Registry<DensityFunction> registry, boolean bl, boolean bl2) {
        throw new UnsupportedOperationException();
    }
}
