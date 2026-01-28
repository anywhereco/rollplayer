package dev.infernity.rollplayer.framework.files;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class JarPather<T> {
    public String getFolderWithJarFile(Class<T> clazz) {
        String[] split = getJarFilePath(clazz).split("/");
        return Arrays.stream(split).limit(split.length - 1).collect(Collectors.joining("/"));
    }

    public String getJarFilePath(Class<T> clazz) {
        try {
            return byGetProtectionDomain(clazz);
        } catch (Exception e) {
            // cannot get jar file path using byGetProtectionDomain so we try getting the resource instead
        }
        return byGetResource(clazz);
    }

    private String byGetProtectionDomain(Class<T> clazz) throws URISyntaxException {
        URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        return Paths.get(url.toURI()).toString();
    }

    private String byGetResource(Class<T> clazz) {
        URL classResource = clazz.getResource(clazz.getSimpleName() + ".class");
        if (classResource == null) {
            throw new RuntimeException("class resource is null");
        }
        String url = classResource.toString();
        if (url.startsWith("jar:file:")) {
            // extract 'file:......jarName.jar' part from the url string
            String path = url.replaceAll("^jar:(file:.*[.]jar)!/.*", "$1");
            try {
                return Paths.get(new URI(path)).toString();
            } catch (Exception e) {
                throw new RuntimeException("Invalid Jar File URL String");
            }
        }
        throw new RuntimeException("Invalid Jar File URL String");
    }

}