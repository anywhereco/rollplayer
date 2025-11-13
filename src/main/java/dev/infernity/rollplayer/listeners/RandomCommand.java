 package dev.infernity.rollplayer.listeners;

import dev.infernity.rollplayer.components.PaginationComponent;
import dev.infernity.rollplayer.listeners.templates.SimpleCommandListener;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

 @SuppressWarnings("unused")
public class RandomCommand extends SimpleCommandListener {
    private final ArrayList<PaginationComponent.Page> pages = new ArrayList<>();

    public RandomCommand() {
        super("random", "Get a random object.", "\uD83C\uDFB2");
    }

     @Override
     public List<CommandData> getCommandData() {
         return List.of(
                 Commands.slash(commandName, commandDescription)
                         .addSubcommands(
                                 new SubcommandData("color", "Generate a random color.")
                                         .addOption(OptionType.BOOLEAN, "nice", "Try to get a nice color instead of a truly random color. Defaults to true", false)
                         )
                         .setIntegrationTypes(IntegrationType.ALL)
                         .setContexts(InteractionContextType.ALL)
         );
     }

     public void onCommandRan(@NotNull SlashCommandInteractionEvent event) {
        Random random = new Random();
        switch (event.getSubcommandName()) {
            case "color" -> {
                boolean nice = event.getOption("nice", true, OptionMapping::getAsBoolean);
                Color color;
                if (nice) {
                    color = Color.getHSBColor(random.nextFloat(1f), random.nextFloat(0.2f, 0.7f), random.nextFloat(0.4f, 1f));
                } else {
                    color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                }

                var img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
                var g2d = img.createGraphics();

                g2d.setColor(color);
                g2d.fillRect(0, 0, 128, 128);
                g2d.dispose();

                var baos = new ByteArrayOutputStream();
                try {
                    ImageIO.write(img, "PNG", baos);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                var fileUpload = FileUpload.fromData(baos.toByteArray(), "color.png");

                var container = createContainer(
                        TextDisplay.ofFormat("Hex code: #%s", Integer.toHexString(color.getRGB()).substring(2)),
                        MediaGallery.of(MediaGalleryItem.fromFile(fileUpload))
                ).withAccentColor(color);

                event.replyComponents(container).useComponentsV2().queue();
            }
            case null -> throw new IllegalStateException("How did you get a null value :cry:");
            default -> throw new IllegalStateException("Unexpected value: " + event.getSubcommandName());
        }
    }
}
