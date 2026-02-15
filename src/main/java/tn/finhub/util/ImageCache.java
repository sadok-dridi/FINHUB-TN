package tn.finhub.util;

import javafx.scene.image.Image;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {

    private static final Map<String, Image> cache = new ConcurrentHashMap<>();

    /**
     * Retrieves an image from the cache or loads it if not present.
     * Loading happens in the background (JavaFX Image feature).
     *
     * @param url The URL of the image.
     * @return The Image object (cached or newly created).
     */
    public static Image get(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        return cache.computeIfAbsent(url, k -> new Image(k, true)); // true = load in background
    }

    /**
     * Manually adds an image to the cache.
     *
     * @param url   The URL key.
     * @param image The Image object.
     */
    public static void put(String url, Image image) {
        if (url != null && image != null) {
            cache.put(url, image);
        }
    }

    /**
     * Clears the cache.
     */
    public static void clear() {
        cache.clear();
    }
}
