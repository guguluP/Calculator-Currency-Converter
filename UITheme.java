package Project.model;

import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Centralized UI theme management with system theme detection.
 * Handles dark/light mode switching and color palette management.
 */
public final class UITheme {
    public enum ThemeMode {
        DARK, LIGHT, SYSTEM
    }

    private static ThemeMode themeMode = ThemeMode.DARK;
    private static boolean darkMode = true;
    private static boolean systemThemeInitialized = false;

    // ═══════════════════════════════════════════════════════════════════
    //  FONTS
    // ═══════════════════════════════════════════════════════════════════
    public static final Font FONT_DISPLAY = new Font("Segoe UI", Font.BOLD, 44);
    public static final Font FONT_BTN_LG = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_BTN_SM = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_STATUS = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    // ═══════════════════════════════════════════════════════════════════
    //  RADIUS & SIZES
    // ═══════════════════════════════════════════════════════════════════
    public static final int RADIUS_BTN = 12;
    public static final int RADIUS_PANEL = 16;
    public static final int RADIUS_DIALOG = 20;

    // ═══════════════════════════════════════════════════════════════════
    //  COLOR PALETTE - DARK MODE
    // ═══════════════════════════════════════════════════════════════════
    private static final Color DARK_BG_DEEP = new Color(20, 20, 30);
    private static final Color DARK_BG_SURFACE = new Color(30, 30, 42);
    private static final Color DARK_BG_ELEVATED = new Color(45, 45, 60);
    private static final Color DARK_DISPLAY_BG = new Color(25, 25, 35);
    private static final Color DARK_TEXT_PRIMARY = new Color(240, 240, 250);
    private static final Color DARK_TEXT_DIM = new Color(140, 140, 160);
    private static final Color DARK_GLASS_FILL = new Color(55, 55, 75, 200);
    private static final Color DARK_GLASS_HOVER = new Color(70, 70, 95, 220);
    private static final Color DARK_GLASS_PRESS = new Color(40, 40, 60, 240);
    private static final Color DARK_GLASS_BORDER = new Color(100, 100, 130, 160);

    // ═══════════════════════════════════════════════════════════════════
    //  COLOR PALETTE - LIGHT MODE
    // ═══════════════════════════════════════════════════════════════════
    private static final Color LIGHT_BG_DEEP = new Color(245, 245, 250);
    private static final Color LIGHT_BG_SURFACE = new Color(235, 235, 242);
    private static final Color LIGHT_BG_ELEVATED = new Color(225, 225, 235);
    private static final Color LIGHT_DISPLAY_BG = new Color(255, 255, 255);
    private static final Color LIGHT_TEXT_PRIMARY = new Color(30, 30, 40);
    private static final Color LIGHT_TEXT_DIM = new Color(120, 120, 135);
    private static final Color LIGHT_GLASS_FILL = new Color(240, 240, 248, 200);
    private static final Color LIGHT_GLASS_HOVER = new Color(220, 220, 235, 220);
    private static final Color LIGHT_GLASS_PRESS = new Color(200, 200, 220, 240);
    private static final Color LIGHT_GLASS_BORDER = new Color(180, 180, 200, 160);

    // ═══════════════════════════════════════════════════════════════════
    //  ACCENT COLORS (consistent across themes)
    // ═══════════════════════════════════════════════════════════════════
    public static final Color ACCENT_CYAN = new Color(0, 200, 255);
    public static final Color ACCENT_AMBER = new Color(255, 165, 0);
    public static final Color ACCENT_RED = new Color(255, 80, 100);
    public static final Color ACCENT_GREEN = new Color(100, 220, 120);
    public static final Color ACCENT_PURPLE = new Color(180, 100, 255);

    // ═══════════════════════════════════════════════════════════════════
    //  INITIALIZATION & DETECTION
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Initialize system theme detection based on OS settings.
     * Should be called once at application startup.
     */
    public static void initializeSystemTheme() {
        if (systemThemeInitialized) return;

        Preferences prefs = Preferences.userNodeForPackage(UITheme.class);
        
        // Check for saved preference first
        if (prefs.getBoolean("theme_saved", false)) {
            darkMode = prefs.getBoolean("theme_dark_mode", true);
            systemThemeInitialized = true;
            System.out.println("✅ Theme loaded from preferences: " + (darkMode ? "Dark" : "Light"));
            return;
        }

        // Detect system theme
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean detected = false;

        if (osName.contains("mac")) {
            darkMode = detectMacOSTheme();
            detected = true;
        } else if (osName.contains("windows")) {
            darkMode = detectWindowsTheme();
            detected = true;
        } else if (osName.contains("linux")) {
            darkMode = detectLinuxTheme();
            detected = true;
        }

        // Save detected theme
        prefs.putBoolean("theme_saved", true);
        prefs.putBoolean("theme_dark_mode", darkMode);
        try {
            prefs.flush();
        } catch (Exception ignored) {}

        systemThemeInitialized = true;
        System.out.println("✅ System theme detected: " + (darkMode ? "Dark" : "Light") + 
                         (detected ? " (auto)" : " (default)"));
    }

    /**
     * Detect macOS system theme (Mojave+ only)
     */
    private static boolean detectMacOSTheme() {
        try {
            String appleInterfaceStyle = System.getProperty("apple.awt.application.appearance");
            if (appleInterfaceStyle != null) {
                return appleInterfaceStyle.contains("NSAppearanceNameDarkAqua");
            }
        } catch (Exception ignored) {}
        return true; // Default to dark
    }

    /**
     * Detect Windows system theme (Windows 10+)
     */
    private static boolean detectWindowsTheme() {
        try {
            // Check Windows registry for AppsUseLightTheme
            // 0 = dark, 1 = light
            Process process = Runtime.getRuntime().exec(
                "reg query HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize /v AppsUseLightTheme"
            );
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("0x0")) return true;  // Dark mode
                if (line.contains("0x1")) return false; // Light mode
            }
        } catch (Exception ignored) {}
        return true; // Default to dark
    }

    /**
     * Detect Linux system theme (GTK-based)
     */
    private static boolean detectLinuxTheme() {
        try {
            // Check GTK theme settings
            String gtkTheme = System.getenv("GTK_THEME");
            if (gtkTheme != null) {
                return gtkTheme.toLowerCase().contains("dark");
            }

            // Check for dark appearance in environment
            String colorScheme = System.getenv("GTK_THEME_PREFER_DARK");
            return colorScheme != null && colorScheme.equals("1");
        } catch (Exception ignored) {}
        return true; // Default to dark
    }

    // ═══════════════════════════════════════════════════════════════════
    //  THEME SWITCHING
    // ═══════════════════════════════════════════════════════════════════

    public static void setDarkMode(boolean dark) {
        darkMode = dark;
        Preferences prefs = Preferences.userNodeForPackage(UITheme.class);
        prefs.putBoolean("theme_dark_mode", dark);
        try {
            prefs.flush();
        } catch (Exception ignored) {}
        System.out.println("✅ Theme changed to: " + (dark ? "Dark" : "Light"));
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static ThemeMode getThemeMode() {
        return themeMode;
    }

    public static void setThemeMode(ThemeMode mode) {
        themeMode = mode;
        switch (mode) {
            case DARK -> darkMode = true;
            case LIGHT -> darkMode = false;
            case SYSTEM -> {
                // Re-initialize to detect system theme
                systemThemeInitialized = false;
                initializeSystemTheme();
            }
        }
        Preferences prefs = Preferences.userNodeForPackage(UITheme.class);
        prefs.putBoolean("theme_saved", true);
        prefs.putBoolean("theme_dark_mode", darkMode);
        try {
            prefs.flush();
        } catch (Exception ignored) {}
        System.out.println("✅ Theme changed to: " + mode + " (" + (darkMode ? "Dark" : "Light") + ")");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  COLOR ACCESSORS
    // ═══════════════════════════════════════════════════════════════════

    public static Color BG_DEEP() {
        return darkMode ? DARK_BG_DEEP : LIGHT_BG_DEEP;
    }

    public static Color BG_SURFACE() {
        return darkMode ? DARK_BG_SURFACE : LIGHT_BG_SURFACE;
    }

    public static Color BG_ELEVATED() {
        return darkMode ? DARK_BG_ELEVATED : LIGHT_BG_ELEVATED;
    }

    public static Color DISPLAY_BG() {
        return darkMode ? DARK_DISPLAY_BG : LIGHT_DISPLAY_BG;
    }

    public static Color TEXT_PRIMARY() {
        return darkMode ? DARK_TEXT_PRIMARY : LIGHT_TEXT_PRIMARY;
    }

    public static Color TEXT_DIM() {
        return darkMode ? DARK_TEXT_DIM : LIGHT_TEXT_DIM;
    }

    public static Color GLASS_FILL() {
        return darkMode ? DARK_GLASS_FILL : LIGHT_GLASS_FILL;
    }

    public static Color GLASS_HOVER() {
        return darkMode ? DARK_GLASS_HOVER : LIGHT_GLASS_HOVER;
    }

    public static Color GLASS_PRESS() {
        return darkMode ? DARK_GLASS_PRESS : LIGHT_GLASS_PRESS;
    }

    public static Color GLASS_BORDER() {
        return darkMode ? DARK_GLASS_BORDER : LIGHT_GLASS_BORDER;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Creates a color with adjusted brightness
     */
    public static Color brighten(Color c, float factor) {
        int r = Math.min(255, (int) (c.getRed() * factor));
        int g = Math.min(255, (int) (c.getGreen() * factor));
        int b = Math.min(255, (int) (c.getBlue() * factor));
        return new Color(r, g, b, c.getAlpha());
    }

    /**
     * Creates a color with adjusted opacity
     */
    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /**
     * Get theme name for display
     */
    public static String getThemeName() {
        return darkMode ? "Dark" : "Light";
    }
}