package dev.infernity.rollplayer.framework;

import dev.infernity.rollplayer.framework.files.JarPather;
import dev.infernity.rollplayer.framework.types.interactions.Interaction;
import dev.infernity.rollplayer.framework.types.interactions.InteractionType;
import io.fusionauth.http.server.HTTPRequest;
import io.fusionauth.http.server.HTTPResponse;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

public class Bot {
    private final String USER_AGENT;
    HttpClient client;
    private final FileBasedConfiguration config;

    private final String version;
    private final String name;
    private final String timestamp;

    public Bot() {
        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        var pather = new JarPather<Bot>();
        Parameters params = new Parameters();

        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class).configure(params.properties()
                        .setBasePath(pather.getFolderWithJarFile(Bot.class))
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

        USER_AGENT = String.format("DiscordBot (https://github.com/infernitydev/rollplayer/, %s)", version);
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

    public boolean verify(String signatureHex, String timestamp, String body) {
        if (Objects.isNull(signatureHex) || Objects.isNull(timestamp)) {
            return false;
        }
        byte[] publicKeyBytes = hexToBytes(config.getString("discord.publickey"));
        byte[] signatureBytes = hexToBytes(signatureHex);

        // Message = timestamp + body
        byte[] message = (timestamp + body).getBytes(StandardCharsets.UTF_8);

        // Load public key
        Ed25519PublicKeyParameters publicKey =
                new Ed25519PublicKeyParameters(publicKeyBytes, 0);

        // Verify signature
        Ed25519Signer verifier = new Ed25519Signer();
        verifier.init(false, publicKey);
        verifier.update(message, 0, message.length);

        return verifier.verifySignature(signatureBytes);
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] =
                    (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                            + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public boolean jsonContent(String body, HTTPRequest req, HTTPResponse res) {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode rootNode = objectMapper.readTree(body);

        var type = InteractionType.fromId(rootNode.get("type").asInt(-1));

        res.setContentType("application/json");
        res.setHeader("User-Agent", USER_AGENT);
        switch (type) {
            case InteractionType.PING -> {
                try {
                    res.getOutputStream().write("{type:1}".getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
            case null, default -> {
                throw new RuntimeException("Waaaaaa this isnt implemented yet Waaaaaa");
            }
        }
    }
}
