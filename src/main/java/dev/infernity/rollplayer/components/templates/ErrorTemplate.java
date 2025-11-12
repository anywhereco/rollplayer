package dev.infernity.rollplayer.components.templates;

import dev.infernity.rollplayer.Resources;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;

public class ErrorTemplate {
    public static final String errorEmoji = Resources.getInstance().getConfig().getString("emoji.error", "❌");

    public static Container of(String error) {
        return of(error, null);
    }

    public static Container of(String error, String details){
        if (error == null) {error = "An error occured!";}
        if (details == null) {
            return Container.of(TextDisplay.ofFormat("### %s %s", errorEmoji, error)).withAccentColor(14495300);
        }
        return Container.of(
                TextDisplay.ofFormat("### %s %s", errorEmoji, error),
                Separator.createInvisible(Separator.Spacing.SMALL),
                TextDisplay.of(details)
        ).withAccentColor(14495300); // This is the same color as the X character (DD2E44)
    }
}
