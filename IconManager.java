// ═════════════════════════════════════════════════════════════════════
// FIXED IconManager.java
// ═════════════════════════════════════════════════════════════════════

package Project.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import Project.model.UITheme;

public final class IconManager {

    public static void setAppIcon(JFrame frame) {
        java.util.List<Image> icons = new java.util.ArrayList<>();

        // 1. Try as resource in the same package (AppIcon.png in Project/)
        try {
            URL url = IconManager.class.getResource("../AppIcon.png");
            if (url != null) {
                Image img = Toolkit.getDefaultToolkit().getImage(url);
                icons.add(img);
                System.out.println("✅ Loaded AppIcon.png from resources");
            }
        } catch (Exception ignored) {}

        // 2. Try the PNG file in the current directory
        try {
            File pngFile = new File("AppIcon.png");
            if (pngFile.exists()) {
                Image img = Toolkit.getDefaultToolkit().getImage(pngFile.getAbsolutePath());
                icons.add(img);
                System.out.println("✅ Loaded AppIcon.png from current directory");
            }
        } catch (Exception e) {
            System.err.println("Failed to load from current directory: " + e.getMessage());
        }

        // 3. Try .icns as backup
        try {
            File icnsFile = new File("AppIcon.icns");
            if (icnsFile.exists()) {
                Image img = Toolkit.getDefaultToolkit().getImage(icnsFile.getAbsolutePath());
                icons.add(img);
                System.out.println("✅ Loaded AppIcon.icns");
            }
        } catch (Exception ignored) {}

        // Set multiple sizes for best compatibility
        if (!icons.isEmpty()) {
            frame.setIconImages(icons);

            // Set taskbar/dock icon for better visibility
            if (Taskbar.isTaskbarSupported()) {
                try {
                    Taskbar.getTaskbar().setIconImage(icons.get(0));
                } catch (Exception ignored) {}
            }
        } else {
            // Fallback: Generate a basic icon if none found
            System.out.println("⚠️  No AppIcon found, generating fallback icon");
            frame.setIconImage(createHighQualityIcon());
        }
    }

    private static Image createHighQualityIcon() {
        int size = 256;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Background rounded rect
        g.setColor(new Color(30, 30, 42));
        g.fillRoundRect(20, 20, size - 40, size - 40, 60, 60);

        // Border
        g.setColor(UITheme.ACCENT_AMBER);
        g.setStroke(new BasicStroke(20));
        g.drawRoundRect(45, 45, size - 90, size - 90, 40, 40);

        // Equals symbol
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 140));
        FontMetrics fm = g.getFontMetrics();
        String eq = "=";
        int x = (size - fm.stringWidth(eq)) / 2;
        int y = (size + fm.getAscent()) / 2 - 10;
        g.drawString(eq, x, y);

        g.dispose();
        return img;
    }
}