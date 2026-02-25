package tn.finhub.util;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

/**
 * Small helper to display native system notifications when supported.
 * Falls back to console logging if SystemTray is not available.
 */
public class DesktopNotificationUtil {

    public static void showInfo(String title, String message) {
        show(title, message, TrayIcon.MessageType.INFO);
    }

    public static void showWarning(String title, String message) {
        show(title, message, TrayIcon.MessageType.WARNING);
    }

    public static void showError(String title, String message) {
        show(title, message, TrayIcon.MessageType.ERROR);
    }

    private static void show(String title, String message, TrayIcon.MessageType type) {
        if (!SystemTray.isSupported()) {
            System.out.println("[Notification] " + title + " - " + message);
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Minimal transparent icon to avoid dealing with external resources
            Image image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

            TrayIcon trayIcon = new TrayIcon(image, "FinHub");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("FinHub");

            tray.add(trayIcon);
            trayIcon.displayMessage(title, message, type);

            // Remove icon after showing notification to avoid clutter
            tray.remove(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
            System.out.println("[Notification][fallback] " + title + " - " + message);
        }
    }
}

