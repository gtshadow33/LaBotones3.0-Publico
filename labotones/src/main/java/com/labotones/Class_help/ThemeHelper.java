package com.labotones.Class_help;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

public final class ThemeHelper {

    private static final String THEME_BUTTON_ICON = "🎨";

    private ThemeHelper() {}

    public static void applyTheme(Node node) {
        if (node == null) return;
        node.getStyleClass().removeAll(
            "light-mode", "dark-mode", "dracula-mode", "matrix-mode",
            "nord-mode", "sunset-mode", "azul-mode", "alpha-mode"
        );
        switch (ThemeManager.getCurrentTheme()) {
            case DARK:
                node.getStyleClass().add("dark-mode");
                break;
            case DRACULA:
                node.getStyleClass().add("dracula-mode");
                break;
            case MATRIX:
                node.getStyleClass().add("matrix-mode");
                break;
            case NORD:
                node.getStyleClass().add("nord-mode");
                break;
            case SUNSET:
                node.getStyleClass().add("sunset-mode");
                break;
            case AZUL:
                node.getStyleClass().add("azul-mode");
                break;
            case ALPHA:
                node.getStyleClass().add("alpha-mode");
                break;
            case LIGHT:
            default:
                // no se añade clase para light
                break;
        }
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        applyTheme(scene.getRoot());
    }

    public static void applyTheme(Stage stage) {
        if (stage == null) return;
        applyTheme(stage.getScene());
    }

    public static void updateThemeButton(Button btnTheme) {
        if (btnTheme == null) return;
        btnTheme.setText(THEME_BUTTON_ICON);
        String tema = ThemeManager.getCurrentTheme().name().toLowerCase();
        btnTheme.setTooltip(new Tooltip("Cambiar tema (actual: " + tema + ")"));
    }
}