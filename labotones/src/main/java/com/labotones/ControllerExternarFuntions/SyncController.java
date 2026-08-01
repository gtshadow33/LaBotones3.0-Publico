package com.labotones.ControllerExternarFuntions;

import java.io.File;
import java.util.Optional;

import com.labotones.Class_help.ButtonService;
import com.labotones.Class_help.ConfigManager;
import com.labotones.Class_help.SyncManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Gestiona toda la lógica de sincronización con el repositorio compartido.
 * Incluye sync al arrancar, sync automático periódico y diálogos de configuración.
 */
public class SyncController {

    private final ButtonService buttonService;
    private final TextField campoReferencia;
    private final Runnable onSyncCompletado;
    private final java.util.function.Consumer<String> logger;

    private Timeline syncTimer;
    private boolean sincronizando = false;

    public SyncController(
            ButtonService buttonService,
            TextField campoReferencia,
            Runnable onSyncCompletado,
            java.util.function.Consumer<String> logger) {
        this.buttonService = buttonService;
        this.campoReferencia = campoReferencia;
        this.onSyncCompletado = onSyncCompletado;
        this.logger = logger;
    }

    // ========================================================================
    // ARRANQUE - AHORA NO BLOQUEA LA UI
    // ========================================================================

    public void iniciarSincronizacionYCarga() {
        SyncManager sync = new SyncManager();
        String repoPath = sync.getRepositorioPath();

        // Si no hay repositorio configurado
        if (repoPath == null || repoPath.isEmpty()) {
            if (!ConfigManager.isSincronizarOmitido()) {
                preguntarConfigurarRepositorio();
            } else {
                logger.accept("ℹ Usando datos locales (sincronización omitida permanentemente).");
                // Recargar caché por si hay datos locales
                buttonService.reload();
                onSyncCompletado.run();
            }
            return;
        }

        // ========== 1. PRIMERO: mostrar los datos locales (instantáneo) ==========
        buttonService.reload(); // Asegurar que la caché está actualizada desde disco
        onSyncCompletado.run(); // Esto pinta el grid con lo que haya localmente
        logger.accept("📋 Mostrando datos locales...");

        // ========== 2. LUEGO: sincronizar en BACKGROUND (no bloquea) ==========
        new Thread(() -> {
            logger.accept("🔄 Sincronizando con repositorio en segundo plano...");

            sync.sincronizarDesdeRepositorio(
                // Éxito
                () -> Platform.runLater(() -> {
                    logger.accept("✅ Sincronización completada desde repositorio.");
                    buttonService.reload(); // Recargar caché con datos nuevos
                    onSyncCompletado.run();  // Refrescar grid con datos sincronizados
                }),
                // Error
                () -> Platform.runLater(() -> {
                    logger.accept("⚠️ No se pudo sincronizar con repositorio. Usando datos locales.");
                    // Si falla, nos quedamos con los datos que ya teníamos (ya cargados)
                }),
                // Ocupado (ya había otra sincronización en curso)
                () -> Platform.runLater(() -> {
                    logger.accept("ℹ Ya hay una sincronización en curso.");
                })
            );
        }).start();
    }

    // ========================================================================
    // SYNC AUTOMÁTICO CADA 5 MIN - AHORA EN BACKGROUND
    // ========================================================================

    public void iniciarSyncAutomatico() {
        syncTimer = new Timeline(
            new KeyFrame(Duration.minutes(15), e -> realizarSyncAutomatico())
        );
        syncTimer.setCycleCount(Timeline.INDEFINITE);
        syncTimer.play();
        logger.accept("🔄 Sync automático activado (cada 15 minutos)");
    }

    private void realizarSyncAutomatico() {
        if (sincronizando) return;

        SyncManager sync = new SyncManager();
        String repoPath = sync.getRepositorioPath();

        if (repoPath == null || repoPath.isEmpty()) {
            return;
        }

        sincronizando = true;

        // ========== Sync en BACKGROUND ==========
        new Thread(() -> {
            logger.accept("🔄 Sync automático en segundo plano...");

            sync.sincronizarDesdeRepositorio(
                () -> Platform.runLater(() -> {
                    sincronizando = false;
                    logger.accept("✅ Sync automático completado.");
                    buttonService.reload();
                    onSyncCompletado.run();
                }),
                () -> Platform.runLater(() -> {
                    sincronizando = false;
                    logger.accept("❌ Error en sync automático.");
                }),
                () -> Platform.runLater(() -> {
                    sincronizando = false;
                    logger.accept("ℹ Ya hay una sincronización en curso.");
                })
            );
        }).start();
    }

    public void stop() {
        if (syncTimer != null) syncTimer.stop();
    }

    // ========================================================================
    // DIÁLOGOS DE CONFIGURACIÓN (se mantienen igual)
    // ========================================================================

    private void preguntarConfigurarRepositorio() {
        Platform.runLater(() -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Repositorio no configurado");
            dialog.setHeaderText("No hay una ruta de repositorio configurada");

            VBox content = new VBox(10);
            content.setPadding(new Insets(15));

            Label label = new Label(
                "¿Desea configurar la ruta del repositorio ahora?\n\n" +
                "• Si selecciona 'Configurar', podrá elegir la carpeta del repositorio.\n" +
                "• Si selecciona 'Usar local', continuará con los datos locales."
            );
            label.setWrapText(true);

            CheckBox checkNoMostrar = new CheckBox("No volver a mostrar este mensaje");
            checkNoMostrar.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

            content.getChildren().addAll(label, checkNoMostrar);
            dialog.getDialogPane().setContent(content);

            ButtonType btnConfigurar = new ButtonType("Configurar repositorio", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnLocal = new ButtonType("Usar solo local", ButtonBar.ButtonData.CANCEL_CLOSE);

            dialog.getDialogPane().getButtonTypes().addAll(btnConfigurar, btnLocal);
            dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-font-size: 13px;");

            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent()) {
                if (checkNoMostrar.isSelected()) {
                    ConfigManager.setSincronizarOmitido(true);
                    logger.accept("ℹ Sincronización omitida permanentemente.");
                }
                if (result.get() == btnConfigurar) {
                    abrirSelectorCarpetaRepositorio();
                } else {
                    logger.accept("ℹ Usando datos locales (repositorio no configurado).");
                    buttonService.reload();
                    onSyncCompletado.run();
                }
            } else {
                if (checkNoMostrar.isSelected()) ConfigManager.setSincronizarOmitido(true);
                logger.accept("ℹ Usando datos locales (repositorio no configurado).");
                buttonService.reload();
                onSyncCompletado.run();
            }
        });
    }

    public void abrirSelectorCarpetaRepositorio() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar carpeta del repositorio");

        File initialDir = new File(System.getProperty("user.home"));
        if (initialDir.exists()) directoryChooser.setInitialDirectory(initialDir);

        Stage stage = (Stage) campoReferencia.getScene().getWindow();
        File selectedDir = directoryChooser.showDialog(stage);

        if (selectedDir != null) {
            String path = selectedDir.getAbsolutePath();
            new SyncManager().setRepositorioPath(path);
            logger.accept("📁 Repositorio configurado en: " + path);
            preguntarSincronizarAhora();
        } else {
            logger.accept("ℹ Configuración cancelada. Usando datos locales.");
            buttonService.reload();
            onSyncCompletado.run();
        }
    }

    private void preguntarSincronizarAhora() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Sincronizar repositorio");
        dialog.setHeaderText("Repositorio configurado correctamente");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        Label label = new Label("¿Desea sincronizar con el repositorio ahora?");
        label.setStyle("-fx-font-size: 13px;");

        CheckBox checkNoMostrar = new CheckBox("No volver a mostrar este mensaje");
        checkNoMostrar.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        content.getChildren().addAll(label, checkNoMostrar);
        dialog.getDialogPane().setContent(content);

        ButtonType btnSync = new ButtonType("Sincronizar ahora", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnLocal = new ButtonType("Usar local por ahora", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(btnSync, btnLocal);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent()) {
            if (checkNoMostrar.isSelected()) ConfigManager.setSincronizarOmitido(true);

            if (result.get() == btnSync) {
                // ========== Sincronizar en BACKGROUND ==========
                new Thread(() -> {
                    SyncManager sync = new SyncManager();
                    sync.sincronizarDesdeRepositorio(
                        () -> Platform.runLater(() -> {
                            logger.accept("✅ Sincronización completada desde repositorio.");
                            buttonService.reload();
                            onSyncCompletado.run();
                        }),
                        () -> Platform.runLater(() -> {
                            logger.accept("❌ Error al sincronizar. Usando datos locales.");
                            buttonService.reload();
                            onSyncCompletado.run();
                        })
                    );
                }).start();
            } else {
                logger.accept("ℹ Usando datos locales (sin sincronizar).");
                buttonService.reload();
                onSyncCompletado.run();
            }
        }
    }
}