package dev.infernity.rollplayer.framework.types.interactions;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface Interaction {
    InteractionType getType();

    record Ping () implements Interaction {
        @Override
        public InteractionType getType() {
            return InteractionType.PING;
        }
    }
}
