package dev.infernity.rollplayer.listeners;

import dev.infernity.rollplayer.components.PaginationComponent;
import dev.infernity.rollplayer.components.templates.ErrorTemplate;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PaginationManager implements EventListener {
    private final Map<UUID, PaginationComponent> componentMap = new HashMap<>();

    public <T extends IReplyCallback> void post(PaginationComponent pc, T interaction) {
        componentMap.put(pc.uuid, pc);
        interaction.replyComponents(pc.asContainer()).useComponentsV2().queue();
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof ButtonInteractionEvent buttonInteractionEvent){
            String[] id = buttonInteractionEvent.getComponentId().split(":");
            if (Objects.equals(id[0], "pagination")) {
                buttonPageChange(buttonInteractionEvent);
            }
        }
        if (event instanceof StringSelectInteractionEvent stringSelectInteractionEvent){
            String[] id = stringSelectInteractionEvent.getValues().getFirst().split(":");
            if (Objects.equals(id[0], "pagination")) {
                stringSelectPageChange(stringSelectInteractionEvent);
            }
        }
    }

    private PaginationComponent.Result toPage(PaginationComponent pc, int page){
        return pc.toPage(page);
    }

    private PaginationComponent.Result goToMenu(PaginationComponent pc){
        return pc.goToMenu();
    }

    private PaginationComponent.Result exitMenu(PaginationComponent pc){
        return pc.exitMenu();
    }

    private <T extends IReplyCallback & IMessageEditCallback> void updateMenu(PaginationComponent.Result res, PaginationComponent pc, T interaction) {
        switch (res) {
            case OK -> interaction.editComponents(pc.asContainer()).useComponentsV2().queue();
            case PAGE_DOES_NOT_EXIST -> interaction.replyComponents(ErrorTemplate.of("That page doesn't exist!")).useComponentsV2().setEphemeral(true).queue();
            case EXPIRED -> interaction.replyComponents(ErrorTemplate.of("This interaction has expired.")).useComponentsV2().setEphemeral(true).queue();
            case NOT_YOUR_MENU -> interaction.replyComponents(ErrorTemplate.of("This isn't your menu!")).useComponentsV2().setEphemeral(true).queue();
        }
    }

    private <T extends IReplyCallback & IMessageEditCallback> void pageChange(T event, User user, String[] id, boolean inMenu){
        var pc = componentMap.get(UUID.fromString(id[1]));
        PaginationComponent.Result res;
        if (Objects.isNull(pc)) {
            res = PaginationComponent.Result.EXPIRED;
        } else if (!(user.getIdLong() == pc.menuOpener)) {
            res = PaginationComponent.Result.NOT_YOUR_MENU;
        } else {
            res = switch (id[2]) {
                case "toPage" -> {
                    if (inMenu) exitMenu(pc);
                    yield toPage(pc, Integer.parseInt(id[3]));
                }
                case "goToMenu" -> goToMenu(pc);
                case "exitMenu" -> exitMenu(pc);
                default -> throw new IllegalStateException("Unexpected value: " + id[2]);
            };
        }
        updateMenu(res, pc, event);
    }

    private void buttonPageChange(ButtonInteractionEvent event) {
        String[] id = event.getComponentId().split(":");
        pageChange(event, event.getUser(), id, false);
    }

    private void stringSelectPageChange(StringSelectInteractionEvent event) {
        String[] id = event.getValues().getFirst().split(":");
        pageChange(event, event.getUser(), id, true);
    }
}
