package dev.infernity.rollplayer;

import dev.infernity.rollplayer.files.JarPather;
import dev.infernity.rollplayer.listeners.managers.PaginationManager;
import dev.infernity.rollplayer.settings.DatabaseManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/// A singleton holding application resources.
public class Resources {
    private static final Resources INSTANCE = new Resources();
    private static final String ALPHANUMERIC_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final Logger logger;
    private final FileBasedConfiguration config;
    private final String version;
    private final String name;
    private final String timestamp;
    private final DatabaseManager databaseManager;
    private JDA jda;
    private TextChannel debugChannel;
    private PaginationManager paginationManager;
    private final boolean inDebugMode;

    private Resources() {
        this.logger = LoggerFactory.getLogger("Rollplayer");

        var pather = new JarPather<Resources>();
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class).configure(params.properties()
                        .setBasePath(pather.getFolderWithJarFile(Resources.class))
                        .setFileName("rollplayer.properties"));
        try {
            this.config = builder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new RuntimeException("The configuration file (rollplayer.properties) was not found.", e);
        }
        String _version, _name, _timestamp;
        try (InputStream stream = getClass().getResourceAsStream("/application-details.properties")) {
            Objects.requireNonNull(stream);
            Properties props = new Properties();
            props.load(stream);
            _version = initializeVersion(props);
            _name = initializeName(props);
            _timestamp = initializeTimestamp(props);
        } catch (IOException | NullPointerException e) {
            _version = "(unknown version)";
            _name = "(unknown name)";
            _timestamp = "(unknown timestamp)";
        }
        version = _version;
        name = _name;
        timestamp = _timestamp;

        this.databaseManager = new DatabaseManager();

        this.inDebugMode = config.getBoolean("debug.isDebug", false);
    }

    public static Resources getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("SameParameterValue")
    private static String generateRandomAlphanumericString(int length) {
        var random = new Random();
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative.");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(ALPHANUMERIC_CHARS.length());
            sb.append(ALPHANUMERIC_CHARS.charAt(randomIndex));
        }
        return sb.toString();
    }

    private String initializeVersion(Properties properties) {
        var version = properties.get("application.version");
        if (version == null) {
            return "(unknown version)";
        }
        return (String) version;
    }

    private String initializeName(Properties properties) {
        var name = properties.get("application.name");
        if (name == null) {
            return "(unknown name)";
        }
        return (String) name;
    }

    private String initializeTimestamp(Properties properties) {
        var timestamp = properties.get("application.buildtime");
        if (timestamp == null) {
            return "(unknown timestamp)";
        }
        return (String) timestamp;
    }

    public boolean isDebug() {
        return inDebugMode;
    }

    public Logger getLogger() {
        return logger;
    }

    public FileBasedConfiguration getConfig() {
        return config;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @SuppressWarnings("unused")
    public JDA getJda() {
        return jda;
    }

    public void setJda(JDA jda) {
        this.jda = jda;
        this.debugChannel = jda.getTextChannelById(this.getConfig().getLong("debug.loggingChannel", 0L));
    }

    public PaginationManager getPaginationManager() {
        return paginationManager;
    }

    public void setPaginationManager(PaginationManager pm) {
        this.paginationManager = pm;
    }

    @SuppressWarnings("unused")
    @Nullable
    public TextChannel getDebugChannel() {
        return debugChannel;
    }

    /// Logs an exception to the debug channel.
    /// @param e The exception to log. May be null.
    /// @param extras Any extra components to append.
    /// @return An error code to show to the user.
    public String tryLogException(Throwable e, ContainerChildComponent... extras){
        return tryLogException(e, Arrays.asList(extras));
    }

    /// Logs an exception to the debug channel.
    /// @param e The exception to log. May be null.
    /// @param extras Any extra components to append.
    /// @return An error code to show to the user.
    public String tryLogException(Throwable e, Collection<ContainerChildComponent> extras){
        String trace;
        String msg;
        String name;
        String errcode = generateRandomAlphanumericString(8);
        if (Objects.nonNull(e)){
            trace = Arrays.toString(e.getStackTrace());
            trace = trace.substring(1, trace.length() - 1);
            trace = trace.substring(0, Math.min(trace.length(), 2000));
            if (trace.length() == 2000) {
                trace += "...";
            }
            msg = e.getMessage();
            if (msg == null) {
                msg = "<no message>";
            }
            name = e.getClass().getName();
        } else {
            trace = "<no trace>";
            msg = "<no message>";
            name = "error";
        }
        ArrayList<ContainerChildComponent> els = new ArrayList<>();
        els.add(TextDisplay.ofFormat("## A(n) %s occurred!", name));
        els.add(TextDisplay.of(msg));
        if (Objects.nonNull(extras)) {
            els.addAll(extras);
        }
        els.add(TextDisplay.ofFormat("```\n%s```", trace));
        els.add(TextDisplay.ofFormat("-# error code: %s", errcode));
        debugChannel.sendMessageComponents(Container.of(els)).useComponentsV2().queue();
        return errcode;
    }
}