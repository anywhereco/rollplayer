package dev.infernity.rollplayer.rollplayerlib4;

import java.util.List;

public enum Keywords {
    DROP(new Operation.Dropper());


    public sealed interface Operation {
        final class Dropper implements Operation {
            List<Double> action(List<Double> numbers){
                // TODO dropping stuff whatever
                return List.of(1.0d);
            };
        }
    }


    private final Operation operation;

    public Operation getOperation() {
        return operation;
    }

    private Keywords(Operation operation) {
        this.operation = operation;
    }
}
