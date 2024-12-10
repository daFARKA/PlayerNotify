package net.dafarka.player_notify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

public class TargetConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_PATH = "config/targets.json";

    public ArrayList<String> targetPlayersName = new ArrayList<>();

    public TargetConfig() { }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static TargetConfig load() {
        File configFile = new File(CONFIG_PATH);

        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                return GSON.fromJson(reader, TargetConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        TargetConfig defaultConfig = new TargetConfig();
        defaultConfig.save();
        return defaultConfig;
    }
}
