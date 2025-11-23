package dev.infernity.rollplayer.listeners.managers;

import com.posthog.server.PostHogCaptureOptions;
import dev.infernity.rollplayer.Resources;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import com.posthog.server.PostHog;
import com.posthog.server.PostHogConfig;
import com.posthog.server.PostHogInterface;

import java.util.stream.Collectors;

public class MetricsManager implements EventListener {
    private static final String POSTHOG_HOST = "https://us.i.posthog.com";
    PostHogInterface posthog;

    public MetricsManager(){
        String token = Resources.getInstance().getConfig().getString("posthog.token");
        if (token != null){
            PostHogConfig config = PostHogConfig
                    .builder(token)
                    .host(POSTHOG_HOST)
                    .debug(Resources.getInstance().isDebug())
                    .build();
            posthog = PostHog.with(config);

            Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
        } else {
            Resources.getInstance().getLogger().info("Metrics disabled (if you want them enabled, set the posthog.token property)");
        }
    }

    public void cleanup(){
        Resources.getInstance().getLogger().info("Metrics disabled (if you want them enabled, set the posthog.token property)");
        posthog.close();
    }

    @Override
    public void onEvent(@NotNull GenericEvent genericEvent) {
        if (genericEvent instanceof SlashCommandInteractionEvent event) {
            var user = event.getUser();
            posthog.capture(user.getId(),
                    String.format("command:%s", event.getFullCommandName()),
                    PostHogCaptureOptions.builder()
                            .properties(event.getOptions().stream().collect(Collectors.toMap(s -> String.format("arg:%s", s.getName()), OptionMapping::getAsString)))
                            .property("is_debug", Resources.getInstance().isDebug())
                            .userProperty("username", user.getName())
                            .userProperty("display_name", user.getEffectiveName())
                            .build()
            );
        }
        if (Resources.getInstance().isDebug()) {
            posthog.flush();
        }
    }
}
