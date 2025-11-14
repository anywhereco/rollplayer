 package dev.infernity.rollplayer.listeners;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.infernity.rollplayer.listeners.templates.SimpleCommandListener;
import dev.infernity.rollplayer.util.RandomExt;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class RandomCommand extends SimpleCommandListener {
    private static class String2IntHashMap extends Object2IntOpenHashMap<String>{

    }

    public RandomCommand() {
        super("random", "Get a random object.", "\uD83C\uDFB2");
        this.loadFiles();
    }

    final File namesFile = new File("data/random/names.json");
    final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private HashMap<String, HashMap<String, HashMap<String, String2IntHashMap>>> names = new HashMap<>();


    public void loadFiles() {
        try (FileReader reader = new FileReader(namesFile)) {
            var type = new TypeToken<HashMap<String, HashMap<String, HashMap<String, String2IntHashMap>>>>(){};
            names = gson.fromJson(reader, type);
        } catch (IOException e) {
            // You forgot the file idot
            names = new HashMap<>();
        }
    }

    @Override
    public List<CommandData> getCommandData() {
        return List.of(
                Commands.slash(commandName, commandDescription)
                        .addSubcommands(
                                new SubcommandData("color", "Generate a random color.")
                                        .addOption(OptionType.BOOLEAN, "nice", "Try to get a nice color instead of a truly random color. Defaults to true", false),
                                new SubcommandData("name", "Generate a random name.")
                                        .addOptions(
                                                new OptionData(OptionType.STRING, "form", "Whether you'd like a first name, last name, or both", true)
                                                        .addChoice("First name","first")
                                                        .addChoice("Last name","last")
                                                        .addChoice("Full name","full")
                                        )
                                        .addOptions(
                                                new OptionData(OptionType.STRING, "gender", "Gender associated with the name", false)
                                                        .addChoice("Male","male")
                                                        .addChoice("Female","female")
                                        )
                                        .addOption(OptionType.STRING, "origin", "Country of origin", false, true)
                        )
                        .setIntegrationTypes(IntegrationType.ALL)
                        .setContexts(InteractionContextType.ALL)
        );
    }

    public void onAutocomplete(@NotNull CommandAutoCompleteInteractionEvent event) {
        String[] origins = new String[names.size()];
        names.keySet().toArray(origins);
        if (Objects.equals(event.getSubcommandName(), "name")) {
            if (event.getFocusedOption().getName().equals("origin")) {
                List<Command.Choice> options = Stream.of(origins)
                        .filter(word -> word.toLowerCase().startsWith(event.getFocusedOption().getValue().toLowerCase()))
                        .map(word -> new Command.Choice(word, word))
                        .collect(Collectors.toList());
                event.replyChoices(options.subList(0,Math.min(options.size(),25))).queue();
            }
        }
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

               var container = createContainerSubcommand("color",
                       TextDisplay.ofFormat("Hex code: #%s", Integer.toHexString(color.getRGB()).substring(2)),
                       MediaGallery.of(MediaGalleryItem.fromFile(fileUpload))
               ).withAccentColor(color);

               event.replyComponents(container).useComponentsV2().queue();
           }
           case "name" -> {
               String country = event.getOption("origin", "", OptionMapping::getAsString);
               String gender = event.getOption("gender", "", OptionMapping::getAsString);
               String form = event.getOption("form", "", OptionMapping::getAsString);
               if (country.isEmpty()) {
                   List<String> countries = new ArrayList<>(names.keySet());
                   country = countries.get(random.nextInt(countries.size()));
               }
               var countryNames = names.get(country);
               if (gender.isEmpty()) {
                   List<String> genders = new ArrayList<>(countryNames.keySet());
                   gender = genders.get(random.nextInt(genders.size()));
               }
               var genderNames = countryNames.get(gender);
               var firstNames = genderNames.get("first").keySet().stream().toList();
               var firstNameWeights = genderNames.get("first").values().intStream().boxed().toList();
               var lastNames = genderNames.get("last").keySet().stream().toList();
               var lastNameWeights = genderNames.get("last").values().intStream().boxed().toList();
               String name = "";
               switch (form) {
                   case "first" ->
                       name = RandomExt.weighted_choice(firstNames,firstNameWeights);
                   case "last" ->
                       name = RandomExt.weighted_choice(lastNames,lastNameWeights);
                   case "full" -> {
                       String firstName = RandomExt.weighted_choice(firstNames,firstNameWeights);
                       String lastName = RandomExt.weighted_choice(lastNames,lastNameWeights);
                       name = firstName + " " + lastName;
                   }
               }
               var container = createContainerSubcommand("name",
                       TextDisplay.of(name),
                       TextDisplay.of(String.format("-# %s name from %s", gender, StringUtils.capitalize(country)))
               );

               event.replyComponents(container).useComponentsV2().queue();
           }
           case null -> throw new IllegalStateException("How did you get a null value :cry:");
           default -> throw new IllegalStateException("Unexpected value: " + event.getSubcommandName());
       }
   }
}
