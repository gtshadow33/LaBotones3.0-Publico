package com.labotones.ControllerExternarFuntions;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import com.labotones.Class_help.ThemeHelper;
import com.labotones.Class_help.ThemeManager;

public final class ExternalHelp {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ========== ALERTAS ==========
    public static void mostrarAlerta(Alert.AlertType tipo, String titulo, String header, String contenido) {
        Platform.runLater(() -> {
            Alert alert = new Alert(tipo);
            alert.setTitle(titulo);
            alert.setHeaderText(header);
            alert.setContentText(contenido);
            alert.showAndWait();
        });
    }

    public static void mostrarAdvertencia(String titulo, String contenido) {
        mostrarAlerta(Alert.AlertType.WARNING, titulo, null, contenido);
    }

    public static void mostrarError(String titulo, String contenido) {
        mostrarAlerta(Alert.AlertType.ERROR, titulo, null, contenido);
    }

    public static void mostrarInformacion(String titulo, String contenido) {
        mostrarAlerta(Alert.AlertType.INFORMATION, titulo, null, contenido);
    }

    // ========== DIÁLOGOS DE CONTRASEÑA ==========
    public static String mostrarDialogoPassword(String titulo, String header, String content, String promptText) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(titulo);
        dialog.setHeaderText(header);
        
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10, 15, 10, 15));
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(promptText != null ? promptText : "Contraseña");
        passwordField.setPrefWidth(300);
        passwordField.setMaxWidth(300);
        
        Label label = new Label(content);
        
        grid.add(label, 0, 0);
        grid.add(passwordField, 0, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setMinWidth(300);
        dialog.getDialogPane().setMaxWidth(400);
        
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, cancelButton);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == okButton) {
                return passwordField.getText();
            }
            return null;
        });
        
        dialog.setOnShown(e -> passwordField.requestFocus());
        
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public static String mostrarDialogoPassword(String titulo, String header, String content) {
        return mostrarDialogoPassword(titulo, header, content, "Contraseña");
    }

    public static String mostrarDialogoCrearPassword() {
        return mostrarDialogoPassword(
            "Configurar contraseña",
            "No hay contraseña configurada en el repositorio.",
            "Nueva contraseña:",
            "Introduce tu nueva contraseña"
        );
    }

    public static String mostrarDialogoVerificarPassword() {
        return mostrarDialogoPassword(
            "Acceso restringido",
            "Introduce la contraseña del repositorio",
            "Contraseña:",
            "Introduce tu contraseña"
        );
    }

    // ========== DIÁLOGO DE CONFIRMACIÓN ==========
    public static ButtonType mostrarDialogoConfirmacion(String titulo, String header, String content, ButtonType... opciones) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(opciones);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.orElse(null);
    }

    // ========== LOGS VISUALES ==========
    public static void agregarLogVisual(VBox logArea, String mensaje, int maxLogs) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DEFAULT_FORMATTER);
            Label lbl = new Label("[" + timestamp + "] " + mensaje);
            lbl.setWrapText(true);
            lbl.getStyleClass().add("logs");
            lbl.setMaxWidth(Double.MAX_VALUE);

            if (logArea.getChildren().size() >= maxLogs) {
                logArea.getChildren().remove(logArea.getChildren().size() - 1);
            }
            logArea.getChildren().add(0, lbl);
        });
    }

    // ========== TEMA (DELEGA EN ThemeHelper) ==========
    public static void aplicarTema(VBox root, Button btnTheme) {
        ThemeHelper.applyTheme(root);
        if (btnTheme != null) {
            ThemeHelper.updateThemeButton(btnTheme);
        }
    }

    public static void aplicarTema(Scene scene) {
        ThemeHelper.applyTheme(scene);
    }

    public static void aplicarTema(Stage stage) {
        ThemeHelper.applyTheme(stage);
    }

    // ========== INDICADOR DE PING ==========
    public static void actualizarIndicadorPing(Label pingIndicator, String estado) {
        Platform.runLater(() -> {
            pingIndicator.setVisible(true);
            String color;
            String tooltip;
            switch (estado.toLowerCase()) {
                case "comprobando":
                    color = "#F0A500";
                    tooltip = "Comprobando...";
                    break;
                case "alcanzable":
                    color = "#27AE60";
                    tooltip = "Equipo/IP alcanzable";
                    break;
                case "no_alcanzable":
                    color = "#E74C3C";
                    tooltip = "Equipo/IP no responde";
                    break;
                default:
                    color = "#000000";
                    tooltip = "";
            }
            pingIndicator.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px;");
            pingIndicator.setTooltip(new Tooltip(tooltip));
        });
    }

    // ========== HISTORIAL (CHIPS) ==========
    public static void refrescarHistorialChips(FlowPane historialPane, List<String> historial,
                                               java.util.function.Consumer<String> onSeleccion) {
        Platform.runLater(() -> {
            historialPane.getChildren().clear();
            for (String equipo : historial) {
                String texto = equipo;
                Button chip = new Button(texto);
                chip.setPrefWidth(120);
                chip.setMinWidth(120);
                chip.setMaxWidth(120);
                chip.setStyle("-fx-alignment: center; -fx-font-size: 11px;");
                chip.getStyleClass().add("chip-historial");
                chip.setOnAction(e -> {
                    if (onSeleccion != null) onSeleccion.accept(equipo);
                });
                historialPane.getChildren().add(chip);
            }
        });
    }

    // ========== VALIDACIÓN DE FORMULARIO ==========
    public static void configurarValidacionBorde(TextField textField, java.util.function.Predicate<String> validador) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                textField.setStyle("");
            } else if (!validador.test(newVal)) {
                textField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
            } else {
                textField.setStyle("");
            }
        });
    }

    // ========== AUTOCOMPLETADO ==========
    public static void configurarAutocompletado(TextField textField, List<String> sugerencias,
                                                java.util.function.Consumer<String> onSeleccion) {
        ContextMenu popup = new ContextMenu();
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                popup.hide();
                return;
            }
            String input = newVal.toUpperCase();
            List<String> coincidencias = sugerencias.stream()
                    .filter(s -> s.toUpperCase().contains(input))
                    .limit(6)
                    .toList();
            if (coincidencias.isEmpty()) {
                popup.hide();
                return;
            }
            popup.getItems().clear();
            for (String match : coincidencias) {
                MenuItem item = new MenuItem(match);
                item.setOnAction(e -> {
                    onSeleccion.accept(match);
                    popup.hide();
                });
                popup.getItems().add(item);
            }
            if (!popup.isShowing()) {
                popup.show(textField, textField.localToScreen(0, textField.getHeight()).getX(),
                           textField.localToScreen(0, textField.getHeight()).getY());
            }
        });
        textField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) popup.hide();
        });
    }

    // ========== UTILIDADES ADICIONALES ==========
    public static void registrarEnterGlobal(Scene scene, TextField textField,
                                            java.util.function.Predicate<String> validador,
                                            Runnable accion) {
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                String texto = textField.getText();
                if (texto != null && !texto.isBlank() && validador.test(texto)) {
                    accion.run();
                }
                event.consume();
            }
        });
    }

    public static void mostrarAccesoDenegadoPorBloqueo() {
        mostrarAdvertencia(
            " Acceso denegado",
            "Haz clic en el botón 🔒 y proporciona la contraseña."
        );
    }
}