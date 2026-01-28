package dev.infernity.rollplayer;

import dev.infernity.rollplayer.framework.Bot;
import io.fusionauth.http.server.HTTPHandler;
import io.fusionauth.http.server.HTTPListenerConfiguration;
import io.fusionauth.http.server.HTTPServer;

import java.nio.charset.StandardCharsets;

public class Main {
    static Bot bot;

    static void main(String[] args) {
        bot = new Bot();

        HTTPHandler handler = (req, res) -> {
            var body = new String(req.getBodyBytes(), StandardCharsets.UTF_8);
            var valid = bot.verify(
                    req.getHeader("X-Signature-Ed25519"),
                    req.getHeader("X-Signature-Timestamp"),
                    body
            );

            if (!valid) {
                res.setStatus(401);
                res.setStatusMessage("invalid request signature");
                return;
            }

            if (req.getContentType().equals("application/json")){
                boolean shouldReturn = bot.jsonContent(body, req, res);
                if (shouldReturn) {
                    return;
                }
            }

            IO.println(req.getPath());
            IO.println(res.toString());
        };

        try (HTTPServer server = new HTTPServer().withHandler(handler)
                                    .withListener(new HTTPListenerConfiguration(9999))) {
            server.start();
            boolean exit = false;
            while (!exit){
                var cmd = IO.readln();
                switch (cmd) {
                    case "quit", "exit" -> exit = true;
                }
            }
        }
        System.exit(0);
    }
}
