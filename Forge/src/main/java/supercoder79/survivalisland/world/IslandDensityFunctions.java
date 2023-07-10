package supercoder79.survivalisland.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegisterEvent;
import supercoder79.survivalisland.world.density.IslandContinentalNoiseFunction;

import java.util.function.Supplier;

public final class IslandDensityFunctions {
    public static final DeferredRegister<Codec<? extends DensityFunction>> REGISTER2 = DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, "survivalisland");

    public static void init() {
        REGISTER2.register("islandcont", () -> IslandContinentalNoiseFunction.UCODEC);
    }
}
