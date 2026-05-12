package tn.finhub.util;

import java.awt.Desktop;
import java.net.URI;

public class BrowserUtil {

    public static void openUrl(String url) {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            URI uri = new URI(url);
            
            // Bypass AWT Desktop completely on Linux to prevent Wayland crashes
            if (os.contains("linux")) {
                fallbackOpen(url);
                return;
            }
            
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(uri);
                    return;
                } catch (Exception e) {
                    // Fall through to platform-specific fallback
                }
            }
            fallbackOpen(url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to open browser: " + e.getMessage(), e);
        }
    }

    private static void fallbackOpen(String url) {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("linux")) {
                String[] browsers = {"xdg-open", "google-chrome", "firefox", "chromium-browser", "chromium"};
                for (String browser : browsers) {
                    try {
                        new ProcessBuilder(browser, url).start();
                        return;
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not open browser on this system", e);
        }
    }
}
