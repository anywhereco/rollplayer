package dev.infernity.rollplayer.listeners;

import dev.infernity.rollplayer.Resources;
import dev.infernity.rollplayer.components.PaginationComponent;
import dev.infernity.rollplayer.listeners.templates.SimpleCommandListener;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class Test extends SimpleCommandListener {
    private final ArrayList<PaginationComponent.Page> pages = new ArrayList<>();

    public Test() {
        super("test", "test desc", "\uD83D\uDD27");
        pages.add(new PaginationComponent.Page("pg 1","This is page 1"));
        pages.add(new PaginationComponent.Page("pg 2","This is page 2"));
        pages.add(new PaginationComponent.Page("pg 3","This is page 3"));
    }

    public void onCommandRan(@NotNull SlashCommandInteractionEvent event) {
        Resources.getInstance().getPaginationManager().post(new PaginationComponent(pages, event.getUser(), null), event);
    }
}
