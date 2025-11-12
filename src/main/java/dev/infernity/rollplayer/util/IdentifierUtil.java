package dev.infernity.rollplayer.util;

public class IdentifierUtil {
    public static String identifier(String namespace, String id, Object... formatArgs){
        return String.format(namespace + ":" + id, formatArgs);
    }
}
