package com.labotones.ControllerExternarFuntions;

import com.labotones.Class_help.ConfigManager;
import com.labotones.Class_help.SyncManager;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

import java.util.List;

/**
 * Gestor centralizado para la funcionalidad de bloqueo de botones
 * con contraseña del repositorio.
 */
public final class BloqueoManager {

    private static final String ESTILO_BLOQUEADO = "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;";
    private static final String ESTILO_DESBLOQUEADO = "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;";

    private boolean bloqueado = false;
    private Button btnBloqueo;
    private GridPane contenedorBotones;
    private Runnable onBloqueoCambiado;
    
    // ========== NUEVO: Referencias directas a botones excluidos ==========
    private Button btnConectar;
    private Button btnTheme;
    private Button btnMini;

    public BloqueoManager() {
        this.bloqueado = ConfigManager.isBloqueado();
    }

    // ========== CONFIGURACIÓN ==========

    public void setBtnBloqueo(Button btnBloqueo) {
        this.btnBloqueo = btnBloqueo;
        actualizarIconoBloqueo();
    }

    public void setContenedorBotones(GridPane contenedorBotones) {
        this.contenedorBotones = contenedorBotones;
    }

    public void setOnBloqueoCambiado(Runnable onBloqueoCambiado) {
        this.onBloqueoCambiado = onBloqueoCambiado;
    }

    // ========== NUEVO: Setters para botones excluidos ==========
    public void setBtnConectar(Button btnConectar) {
        this.btnConectar = btnConectar;
    }

    public void setBtnTheme(Button btnTheme) {
        this.btnTheme = btnTheme;
    }

    public void setBtnMini(Button btnMini) {
        this.btnMini = btnMini;
    }

    // ========== MÉTODOS PRINCIPALES ==========

    public boolean toggleBloqueoConPassword() {
        SyncManager sync = new SyncManager();
        String repoPath = sync.getRepositorioPath();

        if (repoPath == null || repoPath.isEmpty()) {
            ExternalHelp.mostrarAdvertencia(
                "Repositorio no configurado",
                "Para usar el bloqueo con contraseña, primero configure el repositorio en Configuración."
            );
            return false;
        }

        if (!sync.repositorioTienePassword()) {
            return manejarRepositorioSinPassword(sync);
        }

        return pedirPasswordYAlternarBloqueo(sync);
    }

    private boolean manejarRepositorioSinPassword(SyncManager sync) {
        var result = ExternalHelp.mostrarDialogoConfirmacion(
            "Sin contraseña configurada",
            "El repositorio no tiene contraseña",
            "¿Desea continuar sin contraseña o configurar una ahora?\n\n" +
            "• 'Continuar sin contraseña': El bloqueo será libre.\n" +
            "• 'Configurar contraseña': Podrá establecer una contraseña para proteger el bloqueo.",
            new javafx.scene.control.ButtonType("Continuar sin contraseña"),
            new javafx.scene.control.ButtonType("Configurar contraseña")
        );

        if (result == null) return false;

        if (result.getText().equals("Configurar contraseña")) {
            String nuevaPass = ExternalHelp.mostrarDialogoCrearPassword();
            if (nuevaPass != null && !nuevaPass.isEmpty()) {
                if (sync.cambiarPassword(nuevaPass)) {
                    return pedirPasswordYAlternarBloqueo(sync);
                } else {
                    ExternalHelp.mostrarError("Error", "No se pudo guardar la contraseña en el repositorio.");
                }
            }
            return false;
        } else {
            alternarBloqueoDirecto();
            return true;
        }
    }

    private boolean pedirPasswordYAlternarBloqueo(SyncManager sync) {
        String password = ExternalHelp.mostrarDialogoVerificarPassword();

        if (password == null) {
            return false;
        }

        if (sync.verificarPassword(password)) {
            alternarBloqueoDirecto();
            return true;
        } else {
            ExternalHelp.mostrarAdvertencia("Acceso denegado", "Contraseña incorrecta.");
            return false;
        }
    }

    public void alternarBloqueoDirecto() {
        this.bloqueado = !this.bloqueado;
        ConfigManager.setBloqueado(this.bloqueado);

        actualizarIconoBloqueo();
        actualizarEstadoBotones();

        if (onBloqueoCambiado != null) {
            onBloqueoCambiado.run();
        }
    }

    public void setBloqueado(boolean bloqueado) {
        this.bloqueado = bloqueado;
        ConfigManager.setBloqueado(bloqueado);
        actualizarIconoBloqueo();
        actualizarEstadoBotones();

        if (onBloqueoCambiado != null) {
            onBloqueoCambiado.run();
        }
    }

    public void aplicarBloqueoAGrid() {
        if (contenedorBotones == null) return;
        actualizarEstadoBotones();
    }

    // ========== ACTUALIZACIÓN DE UI ==========

    private void actualizarIconoBloqueo() {
        if (btnBloqueo == null) return;
        Platform.runLater(() -> {
            if (bloqueado) {
                btnBloqueo.setText("🔒");
                btnBloqueo.setStyle(ESTILO_BLOQUEADO);
                btnBloqueo.setTooltip(new Tooltip("Desbloquear botones (requiere contraseña del repositorio)"));
                btnBloqueo.getStyleClass().removeAll("btn-bloqueo-barra-desbloqueado");
                btnBloqueo.getStyleClass().add("btn-bloqueo-barra-bloqueado");
            } else {
                btnBloqueo.setText("🔓");
                btnBloqueo.setStyle(ESTILO_DESBLOQUEADO);
                btnBloqueo.setTooltip(new Tooltip("Bloquear botones (requiere contraseña del repositorio)"));
                btnBloqueo.getStyleClass().removeAll("btn-bloqueo-barra-bloqueado");
                btnBloqueo.getStyleClass().add("btn-bloqueo-barra-desbloqueado");
            }
        });
    }

    // ========== MÉTODO CORREGIDO: Exclusión por referencia, no por ID ==========
    private void actualizarEstadoBotones() {
        if (contenedorBotones == null) return;
        
        Platform.runLater(() -> {
            for (Node node : contenedorBotones.getChildren()) {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    
                    // ========== CORRECCIÓN: Excluir por REFERENCIA, no por ID ==========
                    boolean esExcluido = false;
                    
                    // Excluir por referencia directa
                    if (btn == btnConectar) esExcluido = true;
                    if (btn == btnBloqueo) esExcluido = true;
                    if (btn == btnTheme) esExcluido = true;
                    if (btn == btnMini) esExcluido = true;
                    
                    // Fallback: si la referencia no funciona, usar ID como respaldo
                    if (!esExcluido) {
                        String btnId = btn.getId();
                        if (btnId != null) {
                            esExcluido = btnId.equals("btnConectar") ||
                                         btnId.equals("btnBloqueo") ||
                                         btnId.equals("btnTheme") ||
                                         btnId.equals("btnMini");
                        }
                    }

                    // Excluir botones dinámicos marcados como "no bloquear" (excluidoBloqueo=true en botones.json)
                    if (!esExcluido && Boolean.TRUE.equals(btn.getProperties().get("excluidoBloqueo"))) {
                        esExcluido = true;
                    }
                    
                    if (esExcluido) {
                        btn.setDisable(false);
                        btn.getStyleClass().remove("boton-bloqueado");
                        continue;
                    }
                    
                    btn.setDisable(bloqueado);
                    if (bloqueado) {
                        btn.getStyleClass().add("boton-bloqueado");
                    } else {
                        btn.getStyleClass().remove("boton-bloqueado");
                    }
                }
            }
        });
    }

    // ========== GETTERS ==========

    public boolean isBloqueado() {
        return bloqueado;
    }

    public void mostrarAccesoDenegadoPorBloqueo() {
        ExternalHelp.mostrarAccesoDenegadoPorBloqueo();
    }
}