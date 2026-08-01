package com.labotones.ControllerExternarFuntions;

import java.util.ArrayList;
import java.util.List;

import com.labotones.Controller;
import com.labotones.MiniController;
import com.labotones.ControllerExternarFuntions.BloqueoManager;
import com.labotones.ControllerExternarFuntions.ExternalHelp;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Gestiona la apertura, cierre y estado de las mini ventanas flotantes.
 */
public class MiniWindowManager {

    private final List<Stage> miniStages = new ArrayList<>();
    private final BloqueoManager bloqueoManager;
    private final Controller mainController;
    private final TextField campoReferencia;
    private final java.util.function.Consumer<String> logger;

    public MiniWindowManager(
            BloqueoManager bloqueoManager,
            Controller mainController,
            TextField campoReferencia,
            java.util.function.Consumer<String> logger) {
        this.bloqueoManager = bloqueoManager;
        this.mainController = mainController;
        this.campoReferencia = campoReferencia;
        this.logger = logger;
    }

    // ========================================================================
    // ABRIR
    // ========================================================================

    public void abrirMini() {
        if (bloqueoManager.isBloqueado()) {
            bloqueoManager.mostrarAccesoDenegadoPorBloqueo();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(mainController.getClass().getResource("mini-view.fxml"));
            Scene scene = new Scene(loader.load(), 360, 260);
            scene.getStylesheets().add(mainController.getClass().getResource("styles.css").toExternalForm());

            Stage miniStage = new Stage();
            miniStage.initStyle(StageStyle.DECORATED);
            miniStage.setTitle("LaMiniBotones");
            miniStage.setScene(scene);

            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            double margin = 20;
            miniStage.setX(screenBounds.getMaxX() - scene.getWidth() - margin);
            miniStage.setY(screenBounds.getMinY() + margin);

            miniStage.setResizable(false);
            miniStage.setAlwaysOnTop(true);

            MiniController controller = loader.getController();
            controller.setMiniStage(miniStage);
            controller.setTextFieldPrincipal(campoReferencia);
            controller.setMainController(mainController);
            controller.setBloqueado(bloqueoManager.isBloqueado());

            controller.setOnCerrar(() -> {
                miniStages.remove(miniStage);
                logger.accept("▶ Mini ventana cerrada - Quedan " + miniStages.size() + " abiertas");
            });

            miniStage.setOnCloseRequest(event -> controller.cerrarMini());

            miniStages.add(miniStage);
            miniStage.show();

            VBox root = (VBox) scene.getRoot();
            ExternalHelp.aplicarTema(root, null);
            logger.accept("▶ Nueva mini ventana abierta - Total: " + miniStages.size());

        } catch (Exception e) {
            e.printStackTrace();
            logger.accept("Error abriendo mini ventana: " + e.getMessage());
            ExternalHelp.mostrarAdvertencia("Error", "No se pudo abrir la vista mini");
        }
    }

    // ========================================================================
    // CERRAR
    // ========================================================================

    public void cerrarTodas() {
        for (Stage stage : new ArrayList<>(miniStages)) {
            if (stage != null && stage.isShowing()) {
                stage.close();
            }
        }
        miniStages.clear();
        logger.accept("Todas las mini ventanas cerradas");
    }

    public void aplicarTemaATodas() {
        for (Stage stage : miniStages) {
            if (stage != null && stage.isShowing()) {
                com.labotones.Class_help.ThemeHelper.applyTheme(stage);
            }
        }
    }

    public boolean hayMiniAbiertas() {
        return !miniStages.isEmpty();
    }

    public int getCantidad() {
        return miniStages.size();
    }
}
