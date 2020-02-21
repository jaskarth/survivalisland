package supercoder79.survivalisland;

import net.fabricmc.api.ModInitializer;
import supercoder79.survivalisland.config.ConfigData;
import supercoder79.survivalisland.config.ConfigSerializer;
import supercoder79.survivalisland.world.WorldGeneratorType;
import supercoder79.survivalisland.world.WorldType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;

public class SurvivalIsland implements ModInitializer {
	public static ConfigData CONFIG;
	public static WorldGeneratorType WORLDGEN_TYPE;
	static WorldType<?> loadMeOnClientPls;

	@Override
	public void onInitialize() {
		CONFIG = ConfigSerializer.init();

		loadMeOnClientPls = WorldType.SURVIVAL_ISLAND;

		WORLDGEN_TYPE = Registry.register(Registry.CHUNK_GENERATOR_TYPE, new Identifier("islandtype", "island"), new WorldGeneratorType(false, OverworldChunkGeneratorConfig::new));
	}
}
