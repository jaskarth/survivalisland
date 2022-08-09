package supercoder79.survivalisland;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import supercoder79.survivalisland.config.ConfigData;
import supercoder79.survivalisland.config.ConfigSerializer;
import supercoder79.survivalisland.world.IslandDensityFunctions;
import supercoder79.survivalisland.world.IslandNoiseGenSettings;
import supercoder79.survivalisland.world.IslandWorldPreset;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SurvivalIsland.MODID)
public class SurvivalIsland {
    public static ConfigData CONFIG;

    // Define mod id in a common place for everything to reference
    public static final String MODID = "survivalisland";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public SurvivalIsland() {
        CONFIG = ConfigSerializer.init();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerFunctions);

        IslandDensityFunctions.REGISTER2.register(modEventBus);
        IslandDensityFunctions.init();

        IslandNoiseGenSettings.REGISTER.register(modEventBus);
        IslandNoiseGenSettings.init();

        IslandWorldPreset.REGISTER.register(modEventBus);
        IslandWorldPreset.init();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void registerFunctions(RegisterEvent event) {
        event.register(Registry.DENSITY_FUNCTION_REGISTRY, rh -> {
            IslandDensityFunctions.post(rh);
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
    }
}
