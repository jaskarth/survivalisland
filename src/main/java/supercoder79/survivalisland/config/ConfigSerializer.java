package supercoder79.survivalisland.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigSerializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ConfigData init() {
        ConfigData configData = null;
        try {

            Path configDir = Paths.get("", "config", "survivalisland.json");
            if (Files.exists(configDir)) {
                configData = GSON.fromJson(new FileReader(configDir.toFile()), ConfigData.class);
            } else {
                configData = new ConfigData();
                Paths.get("", "config").toFile().mkdirs();
                BufferedWriter writer = new BufferedWriter(new FileWriter(configDir.toFile()));
                writer.write(GSON.toJson(configData));

                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configData;
    }
}
