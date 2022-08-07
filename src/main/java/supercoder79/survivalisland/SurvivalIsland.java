package supercoder79.survivalisland;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import supercoder79.survivalisland.config.ConfigData;
import supercoder79.survivalisland.config.ConfigSerializer;

public class SurvivalIsland implements ModInitializer {
	public static ConfigData CONFIG;

	@Override
	public void onInitialize() {
		CONFIG = ConfigSerializer.init();
	}
}
