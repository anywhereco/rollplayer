package dev.infernity.rollplayer.eventmanager;

import dev.infernity.rollplayer.Resources;
import dev.infernity.rollplayer.components.templates.ErrorTemplate;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.InterfacedEventManager;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RollplayerEventManager extends InterfacedEventManager {

    private final CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();

    public void register(@NotNull Object listener) {
        if (!(listener instanceof EventListener)) {
            throw new IllegalArgumentException("Listener must implement EventListener");
        } else {
            this.listeners.add((EventListener)listener);
        }
    }

    public void unregister(@NotNull Object listener) {
        if (!(listener instanceof EventListener)) {
            JDALogger.getLog(this.getClass()).warn("Trying to remove a listener that does not implement EventListener: {}", listener.getClass().getName());
        }

        //noinspection SuspiciousMethodCalls This is done in the JDA and I don't want to mess with it
        this.listeners.remove(listener);
    }

    @NotNull
    public List<Object> getRegisteredListeners() {
        return List.copyOf(this.listeners);
    }

    public void handle(@NotNull GenericEvent event) {
        for(EventListener listener : this.listeners) {
            try {
                listener.onEvent(event);
            } catch (Throwable throwable) {
                var err = Resources.getInstance().tryLogException(throwable);
                if (event instanceof IReplyCallback reply && !reply.isAcknowledged()) {
                    reply.replyComponents(ErrorTemplate.of("An unexpected error occured in running this",
                            throwable.getMessage() + "\n\nIf this issue is unexpected, please contact the developers in [the support server](https://discord.gg/TT3vyT3tAD) and give them the following error code: " + err)).useComponentsV2().setEphemeral(true).queue();
                }
            }
        }
    }
}
