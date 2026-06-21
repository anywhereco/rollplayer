package dev.infernity.rollplayer.rollplayerlib4.lexer;

import dev.infernity.rollplayer.rollplayerlib4.SpanData;

public sealed interface Token {
    SpanData range();

    /// `12.34`
    record Number(String value, SpanData range) implements Token {}
    /// `:` in `1:100`
    record RangeDeclaration(SpanData range) implements Token {}
    /// `d` in `1d100`
    record DiceDeclaration(SpanData range) implements Token {}

    /// `{` in `drop{1,3,>10}`
    record OpenBracket(SpanData range) implements Token {}
    /// `}` in `drop{1,3,>10}`
    record CloseBracket(SpanData range) implements Token {}
    /// `drop` in `drop{1,3,>10}`
    record Keyword(Keyword keyword, SpanData range) implements Token {}


    /// Always at the end of input so you don't go out of bounds
    record EndOfInput(SpanData range) implements Token {}
}
