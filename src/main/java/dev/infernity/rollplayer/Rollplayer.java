package dev.infernity.rollplayer;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Rollplayer extends ListenerAdapter {
    static void main(String[] ignoredArgs) {
        Runtime.getRuntime().addShutdownHook(new Thread(Resources.getInstance()::saveSettings));

        String token = Resources.getInstance().getConfig().getString("discord.token");
        JDABuilder.createDefault(token).addEventListeners(new Rollplayer()).build();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        var api = event.getJDA();
        Resources.getInstance().setJda(api);
        Resources.getInstance().getLogger().info("{} {} is initializing!", Resources.getInstance().getName(), Resources.getInstance().getVersion());
        var listeners = new Listeners();
        Resources.getInstance().getLogger().info("Loading {} listeners.", listeners.listeners.size());
        api.addEventListener(listeners.listeners.toArray());
        var debugServer = Resources.getInstance().getConfig().getLong("debug.testingServer", 0L);
        if (Resources.getInstance().getConfig().getBoolean("commands.update", true)) {
            if (debugServer == 0L) {
                api.updateCommands().addCommands(listeners.commands.toArray(new CommandData[0]))
                        .onSuccess(_ -> Resources.getInstance().getLogger().info("Commands initialized globally."))
                        .queue();
            } else {
                Objects.requireNonNull(api.getGuildById(debugServer)).updateCommands().addCommands(listeners.commands.toArray(new CommandData[0]))
                        .onSuccess(_ -> Resources.getInstance().getLogger().info("Commands initialized to server {}.", debugServer))
                        .queue();
            }
        } else {
            Resources.getInstance().getLogger().info("Not updating commands.");
        }
        super.onReady(event);
        Resources.getInstance().getLogger().info("Readied up!");
    }
}