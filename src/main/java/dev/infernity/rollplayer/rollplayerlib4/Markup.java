package dev.infernity.rollplayer.rollplayerlib4;

public enum Markup {
    ITALIC,
    BOLD;

    public record Group<V>(Markup highlight, V value) {}
}
