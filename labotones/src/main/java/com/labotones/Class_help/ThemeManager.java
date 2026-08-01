package com.labotones.Class_help;

public class ThemeManager {
    private static Theme currentTheme;

    static {
        String stored = ConfigManager.getProperty("theme", "");
        if (!stored.isEmpty()) {
            try {
                currentTheme = Theme.valueOf(stored);
            } catch (IllegalArgumentException e) {
                currentTheme = Theme.LIGHT;
            }
        } else {
            String darkMode = ConfigManager.getProperty("darkMode", "false");
            currentTheme = Boolean.parseBoolean(darkMode) ? Theme.DARK : Theme.LIGHT;
            ConfigManager.setProperty("theme", currentTheme.name());
            ConfigManager.setProperty("darkMode", null);
        }
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static void setTheme(Theme theme) {
        if (theme == null) return;
        currentTheme = theme;
        ConfigManager.setProperty("theme", theme.name());
    }

    /**
     * Avanza al siguiente tema en orden circular.
     * LIGHT → DARK → DRACULA → MATRIX → NORD → SUNSET → AZUL → LIGHT
     */
    public static void toggle() {
    switch (currentTheme) {
        case LIGHT:  setTheme(Theme.DARK); break;
        case DARK:   setTheme(Theme.DRACULA); break;
        case DRACULA:setTheme(Theme.MATRIX); break;
        case MATRIX: setTheme(Theme.NORD); break;
        case NORD:   setTheme(Theme.SUNSET); break;
        case SUNSET: setTheme(Theme.AZUL); break;
        case AZUL:   setTheme(Theme.ALPHA); break;
        case ALPHA:  setTheme(Theme.LIGHT); break;
        default:     setTheme(Theme.LIGHT); break;
    }
}
}