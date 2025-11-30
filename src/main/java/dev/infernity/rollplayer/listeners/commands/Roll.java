package dev.infernity.rollplayer.listeners.commands;

import dev.infernity.rollplayer.Resources;
import dev.infernity.rollplayer.components.templates.ErrorTemplate;
import dev.infernity.rollplayer.listeners.templates.SimpleCommandListener;
import dev.infernity.rollplayer.rollplayerlib3.Parser;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Roll extends SimpleCommandListener {
    public Roll(){
        super("roll", "They see me rollin', they hatin'", "\uD83C\uDFB2");
    }

    @Override
    public List<CommandData> getCommandData(){
        return List.of(
                Commands.slash(commandName, commandDescription)
                        .setContexts(InteractionContextType.ALL)
                        .addOption(OptionType.STRING, "roll", "Roll expressions, rules are explained in rollhelp", false)
                        .setIntegrationTypes(IntegrationType.ALL)
                        .setContexts(InteractionContextType.ALL)
        );
    }

    private static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onCommandRan(@NotNull SlashCommandInteractionEvent event) {
        String input = event.getOption("roll",
                () -> Resources.getInstance().getDatabaseManager().getSettings(event.getUser().getIdLong()).getDefaultRoll(),
                OptionMapping::getAsString);
        try {

            if (isInteger(input)) { // If people are doing "roll 20", they probably expect "roll d20"
                input = "d" + input;
            }
            ArrayList<String> evaluations;
            ArrayList<String> expressions;
            List<ContainerChildComponent> output = new ArrayList<>();

            expressions = Parser.removeWhitespace(input);
            if (expressions.getFirst().equals("ERR") || expressions.size() > 10) {
                String errorString;

                if (expressions.getFirst().equals("ERR")) errorString = expressions.get(1);
                else errorString = "Rollplayer cannot roll more than 10 expressions at once";

                event.replyComponents(ErrorTemplate.of("Whitespace Removal error", errorString)).useComponentsV2().queue();
                return;
            }

            evaluations = Parser.evaluate(input);
            if (evaluations.getFirst().equals("ERR")) {
                event.replyComponents(ErrorTemplate.of("Evaluation error", evaluations.get(1))).useComponentsV2().queue();
                return;
            }


            // add each expression-evaluation pair
            for (int exp = 0; exp < evaluations.size(); exp++) {
                if (expressions.size() > 1) {
                    output.add(TextDisplay.ofFormat("**%s**", expressions.get(exp)));
                }

                String[] values;
                if (evaluations.get(exp).startsWith("r")) { // roll list clause
                    values = evaluations.get(exp).substring(2).split(" ");
                } else { // single output clause
                    values = new String[]{evaluations.get(exp)};
                }

                // remove trailing zeroes
                for (int s = 0; s < values.length; s++)
                    if (values[s].endsWith(".0"))
                        values[s] = values[s].substring(0, values[s].length() - 2);

                StringBuilder line = new StringBuilder("[");
                for (int s = 0; s < values.length; s++) {
                    line.append(values[s]);
                    if (s < values.length - 1)
                        line.append(", ");
                }
                if (values.length > 1) { //   append the total
                    double total = 0;
                    for (String s : values)
                        total += Double.parseDouble(s);

                    if (total == (int) total) //  remove trailing zero
                        line.append(" (total: ").append((int) total).append(")");
                    else
                        line.append(" (total: ").append(total).append(")");
                }
                line.append("]");
                output.add(TextDisplay.ofFormat("%s", line.toString()));
            }

            Container outputContainer = createContainer(TextDisplay.ofFormat("### %s /roll: %s", this.commandEmoji, input), output);


            // COLOR SECTION
            float minLerpHue = 0, upToMaxLerpHue = 120f / 360, atMaxLerpHue = 180f / 360, overMaxLerpHue = 300f / 360;
            float brightness = 70f / 100, saturation = 1;

            float hue;
            double valueSum = 0;
            double valueMax = Parser.evaluateMinMax(expressions, "max");
            double valueMin = Parser.evaluateMinMax(expressions, "min");

            if (Double.isNaN(valueMin) || Double.isNaN(valueMax)) {
                event.replyComponents(ErrorTemplate.of("Minmax calculation step failed", "If you're seeing this, please contact [the support server](https://discord.gg/TT3vyT3tAD)")).useComponentsV2().queue();
                return;
            }

            for (String s : evaluations) {
                if (s.startsWith("r")) {
                    String[] doubles = s.substring(2).split(" ");
                    for (String d : doubles)
                        valueSum += Double.parseDouble(d);
                } else valueSum += Double.parseDouble(s);
            }

            if (valueSum >= valueMax) {
                if (valueSum >= 2 * valueMax) {
                    hue = overMaxLerpHue;
                    brightness = .85f;
                } else {
                    // overmax lerp
                    // think of this as (valueSum - valueMax) / (2*valueMax - valueMax)
                    float lerp = (float) ((valueSum - valueMax) / valueMax);
                    hue = lerp * (overMaxLerpHue - atMaxLerpHue) + atMaxLerpHue;
                    brightness = (lerp * .1f) + .75f;
                }
            } else if (valueSum <= valueMin) {
                brightness = 0;
                hue = 0;
            } else {
                //what's lerpma?
                float lerp = (float) ((valueSum - valueMin) / (valueMax - valueMin));
                hue = lerp * (upToMaxLerpHue - minLerpHue) + minLerpHue;
            }

            outputContainer = outputContainer.withAccentColor(Color.getHSBColor(hue, saturation, brightness));

            event.replyComponents(outputContainer).useComponentsV2().queue();
        } catch (Exception e) {
            var errcode = Resources.getInstance().tryLogException(e, TextDisplay.ofFormat("Roll string: `%s`", input), TextDisplay.ofFormat("-# from `%s`", event.getUser().getName()));
             event.replyComponents(createContainer(
                    TextDisplay.of("**Rollplayer has run into an issue:**"),
                    TextDisplay.ofFormat("%s", e.toString()),
                    TextDisplay.ofFormat("\n-# This issue is unexpected. If you wish to track the issue, join our [support server](https://discord.gg/TT3vyT3tAD) and give the developers the following error code: %s", errcode)
            )).useComponentsV2().queue();
        }
    }
}
