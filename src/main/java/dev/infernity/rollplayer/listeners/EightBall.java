package dev.infernity.rollplayer.listeners;

import dev.infernity.rollplayer.listeners.templates.SimpleCommandListener;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public class EightBall extends SimpleCommandListener {
    private final String[] RESPONSES = {
            "It is certain",
            "It is decidedly so",
            "Without a doubt",
            "Yes, definitely",
            "You may rely on it",
            "As I see it, yes",
            "Most likely",
            "Outlook good",
            "Yes",
            "Signs point to yes",
            "Reply hazy, try again",
            "Ask again later",
            "Better not tell you now",
            "Cannot predict now",
            "Concentrate and ask again",
            "Don't count on it",
            "My reply is no",
            "My sources say no",
            "Outlook not so good",
            "Very doubtful"};

    public EightBall() {
        super("8ball", "Query the magic 8 ball for answers to your questions.","\uD83C\uDFB1");
    }

    @Override
    public List<CommandData> getCommandData(){
        return List.of(
                Commands.slash(commandName, commandDescription)
                        .setContexts(InteractionContextType.ALL)
                        .addOption(OptionType.STRING, "question", "question desc", false)
                        .setIntegrationTypes(IntegrationType.ALL)
                        .setContexts(InteractionContextType.ALL)
        );
    }


    public void onCommandRan(@NotNull SlashCommandInteractionEvent event) {
        Random random = new Random();
        int randomIndex = random.nextInt(RESPONSES.length);
        String response = RESPONSES[randomIndex];
        var question = event.getOption("question");
        if (question == null) {
            event.replyComponents(createContainer(
                TextDisplay.of("\uD83C\uDFB1 " + response)
            )).useComponentsV2().queue();
            return;
        }
        event.replyComponents(createContainer(
                TextDisplay.of("**" + question.getAsString() + "**"),
                TextDisplay.of(response)
        )).useComponentsV2().queue();
    }
}
