package dev.infernity.rollplayer;

import dev.infernity.rollplayer.eventmanager.RollplayerEventManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Rollplayer extends ListenerAdapter {
    static void main(String[] ignoredArgs) {
        Runtime.getRuntime().addShutdownHook(new Thread(Resources.getInstance()::saveSettings));

        String token = Resources.getInstance().getConfig().getString("discord.token");
        JDABuilder.createDefault(token).addEventListeners(new Rollplayer()).setEventManager(new RollplayerEventManager()).build();
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

        String updateStrategy = Resources.getInstance().getConfig().getString("commands.update", "on").toLowerCase();

        switch (updateStrategy) {
            case "off" -> Resources.getInstance().getLogger().info("Not updating commands.");
            case "wipe" -> {
                if (debugServer == 0L) {
                    api.updateCommands()
                            .onSuccess(_ -> Resources.getInstance().getLogger().info("Commands wiped."))
                            .queue();
                } else {
                    var guild = api.getGuildById(debugServer);
                    if (guild == null) {
                        Resources.getInstance().getLogger().error("Debug server with ID {} not found.", debugServer);
                    } else {
                        guild.updateCommands()
                                .onSuccess(_ -> Resources.getInstance().getLogger().info("Commands wiped on server {}.", debugServer))
                                .queue();
                    }
                }
            }
            case "force" -> {
                Resources.getInstance().getLogger().info("Forcing update of all commands.");
                if (debugServer == 0L) {
                    api.updateCommands().addCommands(listeners.commands)
                            .onSuccess(_ -> Resources.getInstance().getLogger().info("Commands initialized globally."))
                            .queue();
                } else {
                    var guild = api.getGuildById(debugServer);
                    if (guild == null) {
                        Resources.getInstance().getLogger().error("Debug server with ID {} not found.", debugServer);
                    } else {
                        guild.updateCommands().addCommands(listeners.commands)
                                .onSuccess(_ -> Resources.getInstance().getLogger().info("Commands initialized to server {}.", debugServer))
                                .queue();
                    }
                }
            }
            default -> {
                if (!updateStrategy.equals("on")) {
                    Resources.getInstance().getLogger().warn("Unknown command update strategy '{}' (options are off, on, wipe, and force). Defaulting to 'on'.", updateStrategy);
                }
                if (debugServer == 0L) {
                    api.retrieveCommands().queue(remoteCommands ->
                            updateGlobalCommandsIncrementally(api, listeners.commands, remoteCommands)
                    );
                } else {
                    var guild = api.getGuildById(debugServer);
                    if (guild == null) {
                        Resources.getInstance().getLogger().error("Debug server with ID {} not found.", debugServer);
                    } else {
                        guild.retrieveCommands().queue(remoteCommands ->
                                updateGuildCommandsIncrementally(guild, listeners.commands, remoteCommands)
                        );
                    }
                }
            }
        }

        super.onReady(event);
        Resources.getInstance().getLogger().info("Readied up!");
    }

    private void updateGlobalCommandsIncrementally(JDA api, List<CommandData> localCommands, List<Command> remoteCommands) {
        var remoteCommandMap = remoteCommands.stream()
                .collect(Collectors.toMap(Command::getName, Function.identity()));

        var localCommandNames = localCommands.stream()
                .map(CommandData::getName)
                .collect(Collectors.toSet());

        int upserts = 0;
        // 1. Upsert new and changed commands
        for (CommandData localCommand : localCommands) {
            var remoteCommand = remoteCommandMap.get(localCommand.getName());
            boolean changed = false;
            if (remoteCommand == null) {
                changed = true;
            } else {
                // Contexts and integration types don't play well in compares so we override them for this
                var localCommandForComparison = localCommand.setContexts(InteractionContextType.ALL).setIntegrationTypes(IntegrationType.ALL);
                CommandData remoteCommandData = CommandData.fromCommand(remoteCommand).setContexts(InteractionContextType.ALL).setIntegrationTypes(IntegrationType.ALL);
                if (!localCommandForComparison.toData().equals(remoteCommandData.toData())) {
                    changed = true;
                }
            }
            if (changed) {
                upserts++;
                api.upsertCommand(localCommand).queue();
            }
        }

        int deletions = 0;
        // 2. Delete old commands
        for (Command remoteCommand : remoteCommands) {
            if (!localCommandNames.contains(remoteCommand.getName())) {
                deletions++;
                api.deleteCommandById(remoteCommand.getId()).queue();
            }
        }
        Resources.getInstance().getLogger().info("{} upserts and {} deletions.", upserts, deletions);
    }

    private void updateGuildCommandsIncrementally(Guild guild, List<CommandData> localCommands, List<Command> remoteCommands) {
        var remoteCommandMap = remoteCommands.stream()
                .collect(Collectors.toMap(Command::getName, Function.identity()));

        var localCommandNames = localCommands.stream()
                .map(CommandData::getName)
                .collect(Collectors.toSet());

        int upserts = 0;
        // 1. Upsert new and changed commands
        for (CommandData localCommand : localCommands) {
            var remoteCommand = remoteCommandMap.get(localCommand.getName());
            boolean changed = false;
            if (remoteCommand == null) {
                changed = true;
            } else {
                // Contexts and integration types don't play well in compares so we override them for this
                var localCommandForComparison = localCommand.setContexts(InteractionContextType.ALL).setIntegrationTypes(IntegrationType.ALL);
                CommandData remoteCommandData = CommandData.fromCommand(remoteCommand).setContexts(InteractionContextType.ALL).setIntegrationTypes(IntegrationType.ALL);
                if (!localCommandForComparison.toData().equals(remoteCommandData.toData())) {
                    changed = true;
                }
            }
            if (changed) {
                upserts++;
                guild.upsertCommand(localCommand).queue();
            }
        }

        int deletions = 0;
        // 2. Delete old commands
        for (Command remoteCommand : remoteCommands) {
            if (!localCommandNames.contains(remoteCommand.getName())) {
                deletions++;
                guild.deleteCommandById(remoteCommand.getId()).queue();
            }
        }
        Resources.getInstance().getLogger().info("{} upserts and {} deletions for guild {}.", upserts, deletions, guild.getName());
    }
}