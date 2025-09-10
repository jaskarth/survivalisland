package com.jaskarth.survivalisland;

import com.jaskarth.survivalisland.config.ConfigData;
import com.jaskarth.survivalisland.config.ConfigSerializer;

public class SurvivalIsland {
	public static final String ID = "survivalisland";

	public static ConfigData CONFIG;

	public void init() {
		CONFIG = ConfigSerializer.init();
	}
}
