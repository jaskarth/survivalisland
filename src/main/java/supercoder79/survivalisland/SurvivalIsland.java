package supercoder79.survivalisland;

import net.fabricmc.api.ModInitializer;
import supercoder79.survivalisland.config.ConfigData;
import supercoder79.survivalisland.config.ConfigSerializer;
import supercoder79.survivalisland.world.IslandDensityFunctions;

public class SurvivalIsland implements ModInitializer {
	public static ConfigData CONFIG;

	@Override
	public void onInitialize() {
		CONFIG = ConfigSerializer.init();

		IslandDensityFunctions.init();
	}
}
