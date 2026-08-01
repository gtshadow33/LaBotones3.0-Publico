package com.labotones;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.labotones.Class_help.ButtonService;
import com.labotones.Class_help.Button_prop;
import com.labotones.Class_help.ThemeHelper;
import com.labotones.ControllerExternarFuntions.ExternalHelp;
import com.labotones.logic.Logic;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MiniController {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final ButtonService buttonService = new ButtonService();
    
    @FXML private GridPane contenedorBotonesMini;
    @FXML private Label lblInfo;
    
    private Stage miniStage;
    private TextField textFieldPrincipal;
    private int col = 0;
    private int row = 0;
    private Runnable onCerrar;
    
    private Controller mainController;
    private boolean botonesBloqueados = false;
    
    private static final int COLUMNAS = 3;

    @FXML
    public void initialize() {
        cargarBotonesMini();
        Platform.runLater(() -> {
            if (contenedorBotonesMini != null && contenedorBotonesMini.getScene() != null) {
                VBox root = (VBox) contenedorBotonesMini.getScene().getRoot();
                ThemeHelper.applyTheme(root);
            }
        });
    }

    public void setBloqueado(boolean bloqueado) {
        Platform.runLater(() -> {
            this.botonesBloqueados = bloqueado;
            logMain("📢 Estado de bloqueo actualizado: " + (bloqueado ? "BLOQUEADO" : "DESBLOQUEADO"));
            actualizarEstadoVisual();
        });
    }

    private void actualizarEstadoVisual() {
        Platform.runLater(() -> {
            for (Node node : contenedorBotonesMini.getChildren()) {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    if (!"btnRecargar".equals(btn.getId())) {
                        btn.setDisable(botonesBloqueados);
                        if (botonesBloqueados) {
                            btn.getStyleClass().add("boton-bloqueado");
                        } else {
                            btn.getStyleClass().remove("boton-bloqueado");
                        }
                    }
                }
            }
            
            if (lblInfo != null) {
                if (botonesBloqueados) {
                    lblInfo.setText("🔒 BLOQUEADO");
                    lblInfo.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
                } else {
                    int totalBotones = contenedorBotonesMini.getChildren().size();
                    lblInfo.setText("✅ " + totalBotones + " botones");
                    lblInfo.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
            }
        });
    }

    private void cargarBotonesMini() {
        executor.submit(() -> {
            try {
                List<Button_prop> botones = buttonService.getAll();
                Platform.runLater(() -> {
                    contenedorBotonesMini.getChildren().clear();
                    col = 0;
                    row = 0;
                });
                
                for (Button_prop b : botones) {
                    Button btn = crearBotonMini(b);
                    Platform.runLater(() -> agregarBotonMiniAGrid(btn, b));
                    Thread.sleep(5);
                }
                
                Platform.runLater(() -> {
                    if (mainController != null) {
                        botonesBloqueados = mainController.isBloqueado();
                    }
                    actualizarEstadoVisual();
                    logMain("▶ Mini ventana cargada con " + botones.size() + " botones");
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblInfo != null) {
                        lblInfo.setText("❌ Error al cargar");
                        lblInfo.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
                    }
                    logMain("❌ Error al cargar mini ventana: " + e.getMessage());
                });
                e.printStackTrace();
            }
        });
    }

    private Button crearBotonMini(Button_prop b) {
        Button btn = new Button(b.getName());
        btn.setPrefSize(100, 100);
        btn.setMinSize(100, 100);
        btn.setMaxSize(100, 100);
        btn.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center; -fx-padding: 6 4 6 4; -fx-wrap-text: true;");
        btn.getStyleClass().add("btn-mini");
        
        try {
            File file = new File("icons/" + b.getIcono());
            Image img = com.labotones.Class_help.IconCache.obtener(file);
            if (img != null) {
                ImageView icon = new ImageView(img);
                icon.setFitWidth(32);
                icon.setFitHeight(32);
                btn.setGraphic(icon);
                btn.setContentDisplay(ContentDisplay.TOP);
            }
        } catch (Exception e) {
            // Sin icono
        }
        
        btn.setTooltip(new Tooltip(b.getDescription()));
        return btn;
    }

    private void agregarBotonMiniAGrid(Button btn, Button_prop b) {
        btn.setOnAction(e -> {
            if (botonesBloqueados) {
                if (lblInfo != null) {
                    lblInfo.setText("🔒 Botones bloqueados");
                    lblInfo.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
                    new Timeline(new KeyFrame(Duration.seconds(2), ev -> {
                        actualizarEstadoVisual();
                    })).play();
                }
                logMain("🔒 Intento de ejecución con botones bloqueados");
                return;
            }
            
            if (b.isArgumento()) {
                if (textFieldPrincipal == null) {
                    if (lblInfo != null) {
                        lblInfo.setText("❌ No hay TextField");
                        lblInfo.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
                    }
                    logMain("❌ Mini: No hay TextField disponible");
                    return;
                }
                
                String pc = textFieldPrincipal.getText().toUpperCase();
                
                if (pc == null || pc.isBlank()) {
                    if (lblInfo != null) {
                        lblInfo.setText("❌ Introduce un PC");
                        lblInfo.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
                    }
                    logMain("❌ Mini: Texto vacío");
                    return;
                }
                
                if (mainController != null && !mainController.isTextoValido()) {
                    if (lblInfo != null) {
                        lblInfo.setText("❌ Formato inválido");
                        lblInfo.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
                    }
                    return;
                }
                
                if (lblInfo != null) {
                    lblInfo.setText("▶ " + b.getName() + ": " + pc);
                    lblInfo.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
                logMain("▶ Mini - " + b.getDescription() + ": " + pc);
                
                executor.submit(() -> {
                    Logic.ejecutar(b.getLocateBat(), pc);
                    Platform.runLater(() -> {
                        if (lblInfo != null && !botonesBloqueados) {
                            lblInfo.setText("✅ Listo - " + pc);
                            lblInfo.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 14px;");
                        }
                    });
                });
                
            } else {
                if (lblInfo != null) {
                    lblInfo.setText("▶ " + b.getName());
                    lblInfo.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
                logMain("▶ Mini - " + b.getDescription());
                
                executor.submit(() -> {
                    Logic.ejecutar(b.getLocateBat());
                    Platform.runLater(() -> {
                        if (lblInfo != null && !botonesBloqueados) {
                            lblInfo.setText("✅ Listo");
                            lblInfo.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 14px;");
                        }
                    });
                });
            }
        });
        
        contenedorBotonesMini.add(btn, col, row);
        col++;
        if (col >= COLUMNAS) {
            col = 0;
            row++;
        }
    }

    @FXML
    private void recargar() {
        if (lblInfo != null) {
            lblInfo.setText("🔄 Recargando...");
            lblInfo.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
        logMain("▶ Mini - Recargando botones...");
        cargarBotonesMini();
    }

    @FXML
    public void cerrarMini() {
        if (miniStage != null) {
            miniStage.close();
        }
        if (onCerrar != null) {
            onCerrar.run();
        }
        logMain("▶ Mini ventana cerrada");
        shutdown();
    }

    /**
     * ✅ CIERRA CORRECTAMENTE EL EXECUTOR
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void logMain(String mensaje) {
        if (mainController != null) {
            Platform.runLater(() -> {
                mainController.log("[MINI] " + mensaje);
            });
        }
    }

    // ========== GETTERS Y SETTERS ==========
    public boolean isBloqueado() {
        return botonesBloqueados;
    }

    public void setMiniStage(Stage stage) {
        this.miniStage = stage;
        if (stage != null) {
            stage.setAlwaysOnTop(true);
        }
    }

    public void setTextFieldPrincipal(TextField textField) {
        this.textFieldPrincipal = textField;
    }

    public void setOnCerrar(Runnable onCerrar) {
        this.onCerrar = onCerrar;
    }

    public void setMainController(Controller controller) {
        this.mainController = controller;
        if (controller != null) {
            this.botonesBloqueados = controller.isBloqueado();
            Platform.runLater(() -> {
                actualizarEstadoVisual();
            });
        }
    }
}