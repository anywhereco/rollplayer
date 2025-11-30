package dev.infernity.rollplayer.listeners.commands;

import dev.infernity.rollplayer.Resources;
import dev.infernity.rollplayer.components.templates.ErrorTemplate;
import dev.infernity.rollplayer.listeners.templates.SimpleCommandListener;
import dev.infernity.rollplayer.settings.UserSettings;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class Settings extends SimpleCommandListener {
    public Settings() {
        super("settings", "User-specific settings for Rollplayer.", "\uD83D\uDD27");
    }

    @Override
    public List<CommandData> getCommandData() {
        return List.of(
                Commands.slash(commandName, commandDescription)
                        .addSubcommands(
                                new SubcommandData("default-roll", "Get or set your default roll expression.")
                                        .addOption(OptionType.STRING, "expression", "The roll expression to set as your default. Leave blank to get the value.", false)
                                )
                        .setIntegrationTypes(IntegrationType.ALL)
                        .setContexts(InteractionContextType.ALL)
        );
    }

    @Override
    public void onCommandRan(@NotNull SlashCommandInteractionEvent event) {
        if (Objects.equals(event.getSubcommandName(), "default-roll")) {
            UserSettings settings = Resources.getInstance().getSettingsManager().getSettings(event.getUser().getIdLong());
            String expression = event.getOption("expression", null, OptionMapping::getAsString);

            if (expression == null) {
                event.reply("Your current default roll is: `" + settings.getDefaultRoll() + "`").setEphemeral(true).queue();
            } else {
                event.replyComponents(ErrorTemplate.of("Setting your settings is temporarily disabled while migration occurs.")).useComponentsV2().setEphemeral(true).queue();
                //settings.setDefaultRoll(expression);
                //event.reply("Your default roll has been set to: `" + expression + "`").setEphemeral(true).queue();
            }
        }
    }
}
