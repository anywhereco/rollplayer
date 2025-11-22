package dev.infernity.rollplayer.listeners.commands;

import dev.infernity.rollplayer.Resources;
import dev.infernity.rollplayer.components.templates.ErrorTemplate;
import dev.infernity.rollplayer.listeners.templates.SimpleCommandListener;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

public class TicTacToe extends SimpleCommandListener {
    private class TTTGameState{
        public final int width;
        public final int victoryRequirement;
        public final TileState[][] grid;
        public final User playerX;
        @Nullable
        public User playerO;
        public GameResult result;
        public GameState state;
        public final long expirationTimestamp;
        private boolean singleplayer;

        TTTGameState(
                int width,
                int victoryRequirement,
                TileState[][] grid,
                User playerX,
                long expirationTimestamp
        ) {
            this.width = width;
            this.victoryRequirement = victoryRequirement;
            this.grid = grid;
            this.playerX = playerX;
            this.playerO = null;
            this.result = GameResult.INCOMPLETE;
            this.state = GameState.WAITING_FOR_PLAYER;
            this.expirationTimestamp = expirationTimestamp;
            this.singleplayer = false;
        }

        TTTGameState(int width, int victoryRequirement, User user) {
            this(width, victoryRequirement, initializeEmpty(width), user, Instant.now().getEpochSecond() + 900);
        }

        public static TileState[][] initializeEmpty(int width){
            var tiles = new TileState[width][width];
            for (int i = 0; i < width; i++) {
                Arrays.fill(tiles[i], TileState.EMPTY);
            }
            return tiles;
        }

        public Container getBoard(UUID id) {
            assert playerO != null;
            String topbar;
            if (playerX.getIdLong() == playerO.getIdLong()) {
                topbar = String.format("### %s solo game: %s vs. themselves", commandEmoji, playerX.getEffectiveName());
            } else {
                topbar = String.format("### %s game: %s vs. %s", commandEmoji, playerX.getEffectiveName(), playerO.getEffectiveName());
            }
            return createContainer(
                    TextDisplay.of(topbar),
                    getBoardInner(id)
            );
        }

        private List<ContainerChildComponent> getBoardInner(UUID id){
            List<ContainerChildComponent> list = new ArrayList<>();
            list.add(getTypeString(width, victoryRequirement));
            list.add(getStateString());
            for (int i = 0; i < width; i++) {
                List<Button> row = new ArrayList<>();
                for (int j = 0; j < width; j++) {
                    row.add(
                            Button.secondary(
                                    namespacedId("%s:tile:%d:%d", id.toString(), i, j),
                                    grid[i][j].getEmoji()
                            ).withDisabled(grid[i][j] != TileState.EMPTY || state == GameState.OVER)
                    );
                }
                list.add(ActionRow.of(row));
            }
            return list;
        }


        private static TextDisplay getTypeString(int width, int victoryRequirement) {
            return TextDisplay.ofFormat("%dx%d, %d in a row to win", width, width, victoryRequirement);
        }

        private TextDisplay getStateString() {
            if (state == GameState.OVER) {
                return switch (result) {
                    case INCOMPLETE -> throw new IllegalStateException("Game state is over, but result is incomplete?");
                    case X_WINS -> TextDisplay.ofFormat("X (%s) won!", playerX.getEffectiveName());
                    case O_WINS -> TextDisplay.ofFormat("O (%s) won!", Objects.requireNonNull(playerO).getEffectiveName());
                    case DRAW -> TextDisplay.of("It's a draw!");
                };
            } else {
                return switch (state) {
                    case WAITING_FOR_PLAYER -> TextDisplay.of("Waiting for another player...");
                    case X_TURN -> TextDisplay.ofFormat("It's X's turn! (%s)", playerX.getEffectiveName());
                    case O_TURN -> TextDisplay.ofFormat("It's O's turn! (%s)", Objects.requireNonNull(playerO).getEffectiveName());
                    case OVER -> throw new IllegalStateException("we just checked if the game is over :(");
                };
            }
        }

        protected enum TileState {
            EMPTY,
            X,
            O;

            public Emoji getEmoji(){
                return switch (this) {
                    case EMPTY -> Emoji.fromFormatted("<:transparent:1413011570040242376>");
                    case X -> Emoji.fromFormatted("<:error:1413000022449979392>");
                    case O -> Emoji.fromFormatted("<:nought:1413005511871299604>");
                };
            }
        }
        protected enum GameResult {
            INCOMPLETE,
            X_WINS,
            O_WINS,
            DRAW
        }
        protected enum GameState {
            WAITING_FOR_PLAYER,
            X_TURN,
            O_TURN,
            OVER
        }
        public enum TileSetResult {
            SUCCESS,
            GAME_NOT_STARTED,
            TILE_ALREADY_SET,
            NOT_TURN,
            NOT_PLAYING,
            GAME_ENDED
        }

        public void setPlayerO(@NotNull User player){
            playerO = player;
            state = GameState.X_TURN;
            singleplayer = player.getIdLong() == playerX.getIdLong();
        }

        public TileSetResult trySetTile(int x, int y, User user){
            if (playerO == null) {
                return TileSetResult.GAME_NOT_STARTED;
            }
            if (state == GameState.OVER) {
                return TileSetResult.GAME_ENDED;
            }
            TileState tile;
            long id = user.getIdLong();
            if (singleplayer) {
                tile = switch (state) {
                    case WAITING_FOR_PLAYER -> throw new IllegalStateException("Not possible to be a singleplayer game if uninitialized");
                    case X_TURN -> TileState.X;
                    case O_TURN -> TileState.O;
                    case OVER -> throw new IllegalStateException("Already checked if game over above");
                };
            } else {
                if (id == playerX.getIdLong()) {
                    tile = TileState.X;
                } else if (id == playerO.getIdLong()) {
                    tile = TileState.O;
                } else {
                    return TileSetResult.NOT_PLAYING;
                }
            }
            if (grid[x][y] != TileState.EMPTY){
                return TileSetResult.TILE_ALREADY_SET; // This is done after the tile check so that if the user isn't playing we return that instead
            }
            if (((state == GameState.X_TURN && tile == TileState.O) || (state == GameState.O_TURN && tile == TileState.X))
                && (playerO.getIdLong() != playerX.getIdLong())) {
                return TileSetResult.NOT_TURN;
            }
            grid[x][y] = tile;
            switch (state) {
                case O_TURN -> state = GameState.X_TURN;
                case X_TURN -> state = GameState.O_TURN;
            }
            checkWinner();
            return TileSetResult.SUCCESS;
        }

        private void setWinner(TileState winner) {
            this.state = GameState.OVER;
            this.result = (winner == TileState.X) ? GameResult.X_WINS : GameResult.O_WINS;
        }


        private void checkWinner() {
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < width; j++) {
                    TileState currentTile = grid[i][j];
                    if (currentTile == TileState.EMPTY) {
                        continue;
                    }

                    // Horizontal (-)
                    if (j <= width - victoryRequirement) {
                        boolean win = true;
                        for (int k = 1; k < victoryRequirement; k++) {
                            if (grid[i][j + k] != currentTile) {
                                win = false;
                                break;
                            }
                        }
                        if (win) {
                            setWinner(currentTile);
                            return;
                        }
                    }

                    // Vertical (|)
                    if (i <= width - victoryRequirement) {
                        boolean win = true;
                        for (int k = 1; k < victoryRequirement; k++) {
                            if (grid[i + k][j] != currentTile) {
                                win = false;
                                break;
                            }
                        }
                        if (win) {
                            setWinner(currentTile);
                            return;
                        }
                    }

                    // Diagonal (\)
                    if (i <= width - victoryRequirement && j <= width - victoryRequirement) {
                        boolean win = true;
                        for (int k = 1; k < victoryRequirement; k++) {
                            if (grid[i + k][j + k] != currentTile) {
                                win = false;
                                break;
                            }
                        }
                        if (win) {
                            setWinner(currentTile);
                            return;
                        }
                    }

                    // Diagonal but the other way (/)
                    if (i <= width - victoryRequirement && j >= victoryRequirement - 1) {
                        boolean win = true;
                        for (int k = 1; k < victoryRequirement; k++) {
                            if (grid[i + k][j - k] != currentTile) {
                                win = false;
                                break;
                            }
                        }
                        if (win) {
                            setWinner(currentTile);
                            return;
                        }
                    }
                }
            }

            // If no winner was found, check for a draw (board is full)
            boolean isFull = true;
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < width; j++) {
                    if (grid[i][j] == TileState.EMPTY) {
                        isFull = false;
                        break;
                    }
                }
                if (!isFull) {
                    break;
                }
            }

            if (isFull) {
                this.state = GameState.OVER;
                this.result = GameResult.DRAW;
            }
        }
    }
    private final Map<UUID, TTTGameState> gameStateMap = new HashMap<>();

    public void mapCleanup(){
        gameStateMap.entrySet().removeIf(entry -> entry.getValue().expirationTimestamp < Instant.now().getEpochSecond());
    }

    public TicTacToe() {
        super("tictactoe", "test desc", Resources.getInstance().getConfig().getString("emoji.tictactoe", "\uD80C\uDCD1"));
    }

    @Override
    public List<CommandData> getCommandData() {
        return List.of(
                Commands.slash("tictactoe", "Play a game of Tic Tac Toe!")
                        .setContexts(InteractionContextType.ALL)
                        .addOption(OptionType.INTEGER, "width", "The width of the board (1-5, default 3)", false)
                        .addOption(OptionType.INTEGER, "victory_requirement", "The amount in a row required to win (defaults to board size)", false)
                        .setIntegrationTypes(IntegrationType.ALL)
                        .setContexts(InteractionContextType.ALL)
        );
    }

    public void onCommandRan(@NotNull SlashCommandInteractionEvent event) {
        mapCleanup();
        long width = event.getOption("width", 3L, OptionMapping::getAsLong);
        long victoryRequirement = event.getOption("victory_requirement", width, OptionMapping::getAsLong);
        if (width < 1) {
            event.replyComponents(ErrorTemplate.of("You can't have a 0x0 board!")).useComponentsV2().setEphemeral(true).queue();
            return;
        }
        if (width > 5) {
            event.replyComponents(ErrorTemplate.of("You can't have a 6x6 board or larger!",
                    "Unfortunately, this is due to discord limitations :(")).useComponentsV2().setEphemeral(true).queue();
            return;
        }
        if (victoryRequirement > width) {
            event.replyComponents(ErrorTemplate.of("Victory requirement is larger than width!",
                            String.format("How are you supposed to get a %d in a row with a width of %d?", victoryRequirement, width)))
                    .useComponentsV2().setEphemeral(true).queue();
            return;
        }
        if (victoryRequirement < -2147483000) {
            event.replyComponents(ErrorTemplate.of("No integer underflow for you >:["))
                    .useComponentsV2().setEphemeral(true).queue();
            return;
        }
        UUID id = UUID.randomUUID();
        event.replyComponents(
                createContainer(createInitialBoard(id, (int) width, (int) victoryRequirement))
        ).useComponentsV2().queue(_ ->
            gameStateMap.put(id, new TTTGameState(
                    (int) width,
                    (int) victoryRequirement,
                    event.getUser()
            ))
        );
    }

    @Override
    public void onButtonPress(@NotNull ButtonInteractionEvent event) {
        String[] split = event.getComponentId().split(":");
        UUID id = UUID.fromString(split[1]);
        var state = gameStateMap.get(id);
        if (state == null){
            event.replyComponents(ErrorTemplate.of("This game has expired.")).useComponentsV2().setEphemeral(true).queue();
            return;
        }
        if (Objects.equals(split[2], "joinGame")) {
            state.setPlayerO(event.getUser());
            event.editComponents(state.getBoard(id)).useComponentsV2().queue();
        } else if (Objects.equals(split[2], "tile")) {
            TTTGameState.TileSetResult result = state.trySetTile(
                    Integer.parseInt(split[3]),
                    Integer.parseInt(split[4]),
                    event.getUser()
            );
            switch (result) {
                case SUCCESS ->
                        event.editComponents(state.getBoard(id)).useComponentsV2().queue();
                case GAME_NOT_STARTED ->
                        event.replyComponents(ErrorTemplate.of("This game hasn't started yet!", "Press the Join Game button to join.")).useComponentsV2().setEphemeral(true).queue();
                case TILE_ALREADY_SET ->
                        event.replyComponents(ErrorTemplate.of("There's already something on this tile!")).useComponentsV2().setEphemeral(true).queue();
                case NOT_TURN ->
                        event.replyComponents(ErrorTemplate.of("It's not your turn!")).useComponentsV2().setEphemeral(true).queue();
                case NOT_PLAYING ->
                        event.replyComponents(ErrorTemplate.of("You aren't in this game!")).useComponentsV2().setEphemeral(true).queue();
                case GAME_ENDED ->
                        event.replyComponents(ErrorTemplate.of("This game has ended!")).useComponentsV2().setEphemeral(true).queue();
            }
        }
    }

    private List<ContainerChildComponent> createInitialBoard(UUID id, int width, int victoryRequirement){
        List<ContainerChildComponent> list = new ArrayList<>();
        list.add(TTTGameState.getTypeString(width, victoryRequirement));
        list.add(TextDisplay.of("Waiting for another player..."));

        for (int i = 0; i < width; i++) {
            List<Button> row = new ArrayList<>();
            for (int j = 0; j < width; j++) {
                row.add(
                        Button.secondary(
                                namespacedId("%s:tile:%d:%d", id.toString(), i, j),
                                TTTGameState.TileState.EMPTY.getEmoji()
                        )
                );
            }
            list.add(ActionRow.of(row));
        }
        list.add(ActionRow.of(Button.success(namespacedId("%s:joinGame", id.toString()), "Join the game!")));
        return list;
    }
}
