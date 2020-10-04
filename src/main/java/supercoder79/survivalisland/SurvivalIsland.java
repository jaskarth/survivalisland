package supercoder79.survivalisland;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import supercoder79.survivalisland.client.GoVote;
import supercoder79.survivalisland.config.ConfigData;
import supercoder79.survivalisland.config.ConfigSerializer;
import supercoder79.survivalisland.world.IslandBiomeSource;
import supercoder79.survivalisland.world.IslandWorldType;

public class SurvivalIsland implements ModInitializer {
	public static ConfigData CONFIG;
	private static IslandWorldType worldType;

	@Override
	public void onInitialize() {
		CONFIG = ConfigSerializer.init();
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			worldType = new IslandWorldType();
			GoVote.init();
		}

		Registry.register(Registry.BIOME_SOURCE, new Identifier("survivalisland", "island"), IslandBiomeSource.CODEC);
	}
}
