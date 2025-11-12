package dev.infernity.rollplayer.listeners.templates;

import dev.infernity.rollplayer.listeners.interfaces.CommandDataCapable;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

///
/// A simple listener for slash commands.
/// Override onCommandRan for when the command is ran.
/// Override getCommandData to change the command details. It should only return one command which follows the command name.
/// Create buttons whose IDs start with the command name then a colon to
public abstract class SimpleCommandListener implements EventListener, CommandDataCapable {
    protected final String commandName;
    protected final String commandDescription;
    protected final String commandEmoji;

    public SimpleCommandListener(String commandName, String commandDescription, String commandEmoji) {
        this.commandName = commandName;
        this.commandDescription = commandDescription;
        this.commandEmoji = commandEmoji;
    }

    public List<CommandData> getCommandData(){
        return List.of(Commands.slash(commandName, commandDescription).setContexts(InteractionContextType.ALL).setIntegrationTypes(IntegrationType.ALL));
    }

    public String getTitle(){
        return String.format("### %s /%s", commandEmoji, commandName);
    }

    public Container createContainer(TextDisplay title, List<ContainerChildComponent> list){
        list.add(0, title);
        list.add(1, Separator.createInvisible(Separator.Spacing.SMALL));
        return Container.of(list);
    }

    public Container createContainer(List<ContainerChildComponent> list){
        return createContainer(TextDisplay.ofFormat("### %s /%s", commandEmoji, commandName), list);
    }

    public Container createContainer(ContainerChildComponent... components){
        List<ContainerChildComponent> list = new java.util.ArrayList<>(Arrays.stream(components).toList());
        return createContainer(TextDisplay.ofFormat("### %s /%s", commandEmoji, commandName), list);
    }

    public abstract void onCommandRan(@NotNull SlashCommandInteractionEvent event);

    public void onButtonPress(@NotNull ButtonInteractionEvent event) {
        throw new RuntimeException("A button was pressed with this listener, but no override was given to onButtonPress! Event ID: " + event.getComponentId());
    }

    @SuppressWarnings("unused")
    public String namespacedId(String identifier) {
        return commandName + ":" + identifier;
    }

    public String namespacedId(String identifier, Object... args) {
        return String.format(commandName + ":" + identifier, args);
    }

    @Override
    public void onEvent(@NotNull GenericEvent genericEvent) {
        if (genericEvent instanceof SlashCommandInteractionEvent event) {
            if (event.getName().equals(commandName)) {
                onCommandRan(event);
            }
        } else if (genericEvent instanceof ButtonInteractionEvent event) {
            String[] id = event.getComponentId().split(":");
            if (Objects.equals(id[0], commandName)) {
                onButtonPress(event);
            }
        }
    }
}
