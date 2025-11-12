package dev.infernity.rollplayer.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.infernity.rollplayer.Resources;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

public class SettingsManager {
    private final File settingsFile = new File("data/user_settings.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ConcurrentHashMap<Long, UserSettings> userSettings = new ConcurrentHashMap<>();

    public SettingsManager() {
        if (!settingsFile.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            settingsFile.getParentFile().mkdirs();
        }
        loadSettings();
    }

    public void loadSettings() {
        try (FileReader reader = new FileReader(settingsFile)) {
            Type type = new TypeToken<ConcurrentHashMap<Long, UserSettings>>(){}.getType();
            userSettings = gson.fromJson(reader, type);
            if (userSettings == null) {
                userSettings = new ConcurrentHashMap<>();
            } else {
                for (UserSettings settings : userSettings.values()) {
                    settings.performMigration();
                }
            }
        } catch (IOException e) {
            // File probably doesn't exist yet, which is fine.
            userSettings = new ConcurrentHashMap<>();
        }
    }

    public void saveSettings() {
        try (FileWriter writer = new FileWriter(settingsFile)) {
            gson.toJson(userSettings, writer);
        } catch (IOException e) {
            Resources.getInstance().getLogger().error("An error occurred while saving settings: ", e);
        }
    }

    public UserSettings getSettings(long userId) {
        return userSettings.computeIfAbsent(userId, UserSettings::new);
    }
}