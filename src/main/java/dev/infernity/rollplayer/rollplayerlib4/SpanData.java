package dev.infernity.rollplayer.rollplayerlib4;


import org.jetbrains.annotations.NotNull;

public record SpanData(int cursorStart, int cursorEnd, String originalString) {
    @Override
    public @NotNull String toString() {
        return cursorStart + ".." + cursorEnd;
    }

    public static SpanData merge(SpanData from, SpanData to) {
        return new SpanData(from.cursorStart(), to.cursorEnd(), to.originalString());
    }

    public DebugInfo debugInfo() {
        int column = 1;
        int row = 1;
        int lineStart = 0;
        for (int i = 0; i < cursorStart; i++) {
            if (originalString.charAt(i) == '\n') {
                column = 1;
                row += 1;
                lineStart = i + 1;
            } else {
                column += 1;
            }
        }

        int lineEnd = originalString.indexOf('\n', cursorStart);
        if (lineEnd == -1) lineEnd = originalString.length();
        String linePreview = originalString.substring(lineStart, lineEnd);

        return new DebugInfo(linePreview, column - 1, column + (cursorEnd - cursorStart), row);
    }

    public record DebugInfo(
            String linePreview,
            int from,
            int to,
            int line
    ) {
        @Override
        public @NotNull String toString() {
            var startingWs = 0;
            for (var ch : linePreview.toCharArray()) {
                if (ch == ' ') {
                    startingWs += 1;
                } else if (ch == '\t') {
                    startingWs += 4;
                } else {
                    break;
                }
            }

            int length = Math.max(to - from - 1, 1);
            return linePreview.trim() +
                    "\n" +
                    " ".repeat(Math.max(0, from - startingWs)) +
                    "^".repeat(length)
                    + ((line > 1) ? "\nline " + line : "");
        }
    }
}