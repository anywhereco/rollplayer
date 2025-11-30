package dev.infernity.rollplayer.settings;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.internal.utils.JDALogger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;


public class DatabaseManager {
    public final int CURRENT_DATABASE_VERSION = 0;
    private Connection connection;

    private static class UserSettingsOld {
        private long userId;
        private int version;
        private String defaultRoll;
    }

    public DatabaseManager() {
        File dbFile = new File("data/database.db");
        File oldSettingsFile = new File("data/user_settings.json");

        if (!dbFile.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            dbFile.getParentFile().mkdirs();
        }

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            int databaseSchemaVersion;
            try(Statement statement = connection.createStatement()) {
                try(ResultSet rs = statement.executeQuery("PRAGMA user_version;")) {
                    if (!(rs.isClosed())) {
                        databaseSchemaVersion = rs.getInt(1);
                    } else {
                        databaseSchemaVersion = 0;
                    }
                }
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS user_settings (
                        user_id LONG PRIMARY KEY,
                        version INTEGER NOT NULL,
                        default_roll TEXT NOT NULL
                        )
                        """);
            }
        } catch (SQLException e) {
            JDALogger.getLog("DatabaseManager").error("An error occurred while initializing the database: ", e);
        }

        if (oldSettingsFile.exists()) {
            migrateSettingsFromOldJson(oldSettingsFile);
        }

    }

    private void migrateSettingsFromOldJson(File oldSettingsFile) {
        JDALogger.getLog("DatabaseManager").info("Migrating settings from user_settings.json to the database...");
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(oldSettingsFile)) {
            Type type = new TypeToken<ConcurrentHashMap<Long, UserSettingsOld>>(){}.getType();
            ConcurrentHashMap<Long, UserSettingsOld> oldSettings = gson.fromJson(reader, type);
            if (oldSettings != null) {
                for (UserSettingsOld oldSetting : oldSettings.values()) {
                    UserSettings newSetting = new UserSettings(oldSetting.userId);
                    newSetting.setVersion(oldSetting.version);
                    newSetting.setDefaultRoll(oldSetting.defaultRoll);
                    saveSettings(newSetting);
                    JDALogger.getLog("DatabaseManager").info("Migrated {}.", oldSetting.userId);
                }
                JDALogger.getLog("DatabaseManager").info("Migrated {} users' settings.", oldSettings.size());
            }
        } catch (IOException e) {
            JDALogger.getLog("DatabaseManager").error("An error occurred while migrating settings from JSON: ", e);
            return;
        }

        File migratedFile = new File("data/user_settings.json.migrated");
        if (oldSettingsFile.renameTo(migratedFile)) {
            JDALogger.getLog("DatabaseManager").info("Renamed user_settings.json to user_settings.json.migrated");
        } else {
            JDALogger.getLog("DatabaseManager").warn("Failed to rename user_settings.json.");
        }
    }

    public void saveSettings(UserSettings settings) {
        try (var preparedStatement = connection.prepareStatement("INSERT OR REPLACE INTO user_settings VALUES (?, ?, ?)")){
            preparedStatement.setLong(1, settings.getUserId());
            preparedStatement.setInt(2, settings.getVersion());
            preparedStatement.setString(3, settings.getDefaultRoll());
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UserSettings getSettings(long userId) {
        try (var preparedStatement = connection.prepareStatement("SELECT * FROM user_settings WHERE user_id = ?")) {
            preparedStatement.setLong(1, userId);
            var resultSet = preparedStatement.executeQuery();
            if (resultSet.isClosed()) {
                var settings = new UserSettings(userId);
                saveSettings(settings);
                return settings;
            } else {
                // TODO
                return new UserSettings(userId);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}