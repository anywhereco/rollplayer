package dev.infernity.rollplayer.listeners.commands;

import dev.infernity.rollplayer.Resources;
import dev.infernity.rollplayer.components.templates.ErrorTemplate;
import dev.infernity.rollplayer.i18n.ListJoiner;
import dev.infernity.rollplayer.listeners.templates.SimpleCommandListener;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Choose extends SimpleCommandListener {
    private List<String> pickRandom(List<String> list, int count) {
        Random random = new Random();
        List<String> selectedElements = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int randomIndex = random.nextInt(list.size());
            selectedElements.add(list.get(randomIndex));
        }
        return selectedElements;
    }

    private List<String> pickRandomUnique(List<String> list, int count) {
        Collections.shuffle(list);
        return list.subList(0, count);
    }

    public Choose() {
        super("choose", "Choose from a list of options.", Resources.getInstance().getConfig().getString("emoji.choose", "⨂"));
    }

    @Override
    public List<CommandData> getCommandData(){
        return List.of(
                Commands.slash(commandName, commandDescription)
                        .setContexts(InteractionContextType.ALL)
                        .addOption(OptionType.STRING, "options", "A list of options, split by a semicolon (;)", true)
                        .addOption(OptionType.INTEGER, "count", "The amount of options to pick", false)
                        .addOption(OptionType.BOOLEAN, "unique", "Whether the options should be unique or not.", false)
                        .setIntegrationTypes(IntegrationType.ALL)
                        .setContexts(InteractionContextType.ALL)
        );
    }


    public void onCommandRan(@NotNull SlashCommandInteractionEvent event) {
        List<String> options = Arrays.asList(Objects.requireNonNull(event.getOption("options")).getAsString().split(";"));

        int count = event.getOption("count", 1, OptionMapping::getAsInt);
        if (count > 100) {
            event.replyComponents(ErrorTemplate.of(
                    "Choice count is too high",
                    "The limit is 100."
            )).useComponentsV2().queue();
            return;
        }

        boolean unique = event.getOption("unique", true, OptionMapping::getAsBoolean);
        List<String> chosen;

        if (unique) {
            if (count > options.size()) {
                event.replyComponents(ErrorTemplate.of(
                        "Not enough options to pick from",
                        "If you want non-unique options, make the `unique` argument false (it is true by default)."
                        )).useComponentsV2().queue();
                return;
            }
            chosen = pickRandomUnique(options, count);
        } else {
            chosen = pickRandom(options, count);
        }

        event.replyComponents(createContainer(
                TextDisplay.of(ListJoiner.joinList(event.getUserLocale(), chosen))
        )).useComponentsV2().queue();
    }
}
