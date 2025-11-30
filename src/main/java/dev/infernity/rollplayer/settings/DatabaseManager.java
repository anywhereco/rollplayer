package dev.infernity.rollplayer.settings;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.infernity.rollplayer.Resources;
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
import java.util.concurrent.ForkJoinPool;

public class DatabaseManager {
    private Connection connection;

    @SuppressWarnings("unused")
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

            @SuppressWarnings("unused") int databaseSchemaVersion;
            try(Statement statement = connection.createStatement()) {
                try(ResultSet rs = statement.executeQuery("PRAGMA user_version;")) {
                    if (!(rs.isClosed())) {
                        //noinspection UnusedAssignment
                        databaseSchemaVersion = rs.getInt(1);
                    } else {
                        //noinspection UnusedAssignment
                        databaseSchemaVersion = 0;
                    }
                }
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS users (
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

        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));

    }

    private void cleanup() {
        Resources.getInstance().getLogger().info("Database is cleaning up.");
        try {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void migrateSettingsFromOldJson(File oldSettingsFile) {
        JDALogger.getLog("DatabaseManager").info("Migrating settings from user_settings.json to the database...");
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(oldSettingsFile)) {
            Type type = new TypeToken<ConcurrentHashMap<Long, UserSettingsOld>>(){}.getType();
            ConcurrentHashMap<Long, UserSettingsOld> oldSettings = gson.fromJson(reader, type);

            if (oldSettings != null && !oldSettings.isEmpty()) {
                connection.setAutoCommit(false);

                String sql = "INSERT INTO users VALUES (?, ?, ?)";

                try (var preparedStatement = connection.prepareStatement(sql)) {
                    for (UserSettingsOld oldSetting : oldSettings.values()) {
                        preparedStatement.setLong(1, oldSetting.userId);
                        preparedStatement.setInt(2, oldSetting.version);
                        preparedStatement.setString(3, oldSetting.defaultRoll);

                        preparedStatement.addBatch();
                    }

                    preparedStatement.executeBatch();

                    connection.commit();

                    JDALogger.getLog("DatabaseManager").info("Migrated {} users' settings in a batch.", oldSettings.size());
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        } catch (IOException | SQLException e) {
            JDALogger.getLog("DatabaseManager").error("An error occurred while migrating settings: ", e);
            return;
        }

        File migratedFile = new File("data/user_settings.json.migrated");
        if (oldSettingsFile.renameTo(migratedFile)) {
            JDALogger.getLog("DatabaseManager").info("Renamed user_settings.json to user_settings.json.migrated");
        } else {
            JDALogger.getLog("DatabaseManager").warn("Failed to rename user_settings.json.");
        }
    }

    public void saveUserData(UserData data) {
        try (ForkJoinPool pool = ForkJoinPool.commonPool()) {
            pool.execute(() -> {
                try (var preparedStatement = connection.prepareStatement("INSERT OR REPLACE INTO user_settings VALUES (?, ?, ?)")) {
                    preparedStatement.setLong(1, data.getUserId());
                    preparedStatement.setInt(2, data.getVersion());
                    preparedStatement.setString(3, data.getDefaultRoll());
                    preparedStatement.execute();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public UserData getUserData(long userId) {
        try (var preparedStatement = connection.prepareStatement("SELECT * FROM users WHERE user_id = ?")) {
            preparedStatement.setLong(1, userId);
            ResultSet rs = preparedStatement.executeQuery();
            var _ = 1;
            if (rs.next()) {
                var data = new UserData(userId);
                data.setDefaultRoll(rs.getString("default_roll"));
                data.setVersion(rs.getInt("version"));
                return data;
            } else {
                var data = new UserData(userId);
                data.save();
                return data;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}