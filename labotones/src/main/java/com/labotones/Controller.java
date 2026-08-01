package com.labotones;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.labotones.Class_help.ButtonService;
import com.labotones.Class_help.ConfigManager;
import com.labotones.Class_help.HistorialEquipos;
import com.labotones.Class_help.SyncManager;
import com.labotones.Class_help.ThemeHelper;
import com.labotones.Class_help.ThemeManager;
import com.labotones.ControllerExternarFuntions.BloqueoManager;
import com.labotones.ControllerExternarFuntions.BotonesGridManager;
import com.labotones.ControllerExternarFuntions.ExternalHelp;
import com.labotones.ControllerExternarFuntions.MiniWindowManager;
import com.labotones.ControllerExternarFuntions.SyncController;
import com.labotones.SecondStatges.ButtonCRUDController;
import com.labotones.logic.Logic;
import com.labotones.logic.PingService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controlador principal de LaBotones.
 * Coordina BloqueoManager, SyncController, BotonesGridManager y MiniWindowManager.
 */
public class Controller {

    // ========================================================================
    // CONSTANTES
    // ========================================================================

    private static final int MAX_LOGS_VISUALES = 3;

    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    private static final Pattern BCN_PATTERN = Pattern.compile(
        "^BCN[A-Z0-9]+(\\.[A-Z0-9-]+)+$|^BCN[A-Z0-9]+$",
        Pattern.CASE_INSENSITIVE
    );

    // ========================================================================
    // SERVICIOS
    // ========================================================================

    // Antes había un único pool de 2 hilos compartido entre "cargar el grid" y
    // "ejecutar los .bat de los botones". Si el grid tardaba en cargar (repo
    // lento, muchos iconos) podía ocupar los dos hilos y dejar los clics en
    // botones esperando cola, o al revés. Ahora van separados:
    //  - gridExecutor: 1 hilo, de sobra para leer la lista de botones y
    //    disparar el repintado; no hace falta paralelismo aquí.
    //  - executor: para lanzar los .bat (conectar, botones). Se amplía a 2
    //    para que varios clics seguidos no se encolen esperando turno.
    private final ExecutorService gridExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final ButtonService buttonService = new ButtonService();
    private final BloqueoManager bloqueoManager = new BloqueoManager();

    private SyncController syncController;
    private BotonesGridManager gridManager;
    private MiniWindowManager miniManager;

    private Stage stage;

    // ========================================================================
    // UI (FXML)
    // ========================================================================

    @FXML private TextField castexto2;
    @FXML private TextField campoBuscarBotones;
    @FXML private ComboBox<String> comboCategoria;
    @FXML private GridPane contenedorBotones;
    @FXML private VBox logArea;
    @FXML private Label pingIndicator;
    @FXML private FlowPane historialPane;
    @FXML private Button btnTheme;
    @FXML private Button btnBloqueo;
    @FXML private Button btnMini;

    // ========================================================================
    // ESTADO
    // ========================================================================

    private String ultimoTextoValidado = "";

    // ========================================================================
    // PÚBLICO
    // ========================================================================

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // ========================================================================
    // INICIALIZACIÓN
    // ========================================================================

    @FXML
    public void initialize() {
        configurarBloqueo();
        configurarCampoTexto();
        cargarHistorial();
        inicializarManagers();

        syncController.iniciarSincronizacionYCarga();
        syncController.iniciarSyncAutomatico();

        aplicarTemaInicial();
        aplicarEstadoBloqueo();
        gridManager.configurarBusquedaYCategorias();

        Platform.runLater(() ->
            new Timeline(new KeyFrame(Duration.millis(500),
                e -> bloqueoManager.aplicarBloqueoAGrid())).play()
        );
    }

    private void inicializarManagers() {
        syncController = new SyncController(
            buttonService,
            castexto2,
            this::refrescarBotones,
            this::log
        );

        gridManager = new BotonesGridManager(
            buttonService,
            contenedorBotones,
            campoBuscarBotones,
            comboCategoria,
            bloqueoManager,
            gridExecutor,
            executor,
            this::log,
            this::isTextoValido,
            () -> castexto2.getText(),
            this::añadirAlHistorial
        );

        miniManager = new MiniWindowManager(
            bloqueoManager,
            this,
            castexto2,
            this::log
        );
    }

    // ========================================================================
    // BLOQUEO
    // ========================================================================

    private void configurarBloqueo() {
        bloqueoManager.setBtnBloqueo(btnBloqueo);
        bloqueoManager.setContenedorBotones(contenedorBotones);
        bloqueoManager.setOnBloqueoCambiado(this::onBloqueoCambiado);
    }

    private void onBloqueoCambiado() {
        if (bloqueoManager.isBloqueado() && miniManager != null && miniManager.hayMiniAbiertas()) {
            miniManager.cerrarTodas();
            log("🔒 Mini ventanas cerradas por bloqueo");
        }
        aplicarEstadoBloqueo();
        log((bloqueoManager.isBloqueado() ? "🔒" : "🔓") + " Botones " +
            (bloqueoManager.isBloqueado() ? "BLOQUEADOS" : "DESBLOQUEADOS"));
    }

    private void aplicarEstadoBloqueo() {
        if (btnMini == null) return;
        btnMini.setDisable(bloqueoManager.isBloqueado());
        btnMini.setStyle(bloqueoManager.isBloqueado() ? "-fx-opacity: 0.5;" : "-fx-opacity: 1.0;");
        if (bloqueoManager.isBloqueado() && miniManager != null && miniManager.hayMiniAbiertas()) {
            miniManager.cerrarTodas();
        }
    }

    @FXML
    private void toggleBloqueoConPassword() {
        bloqueoManager.toggleBloqueoConPassword();
    }

    public boolean isBloqueado() {
        return bloqueoManager.isBloqueado();
    }

    // ========================================================================
    // CAMPO DE TEXTO Y VALIDACIÓN
    // ========================================================================

    private void configurarCampoTexto() {
        ExternalHelp.configurarValidacionBorde(castexto2, this::isFormatoValido);

        List<String> sugerencias = cargarNomenclaturas();
        ExternalHelp.configurarAutocompletado(castexto2, sugerencias, this::seleccionarSugerencia);

        Platform.runLater(() -> {
            if (castexto2.getScene() != null) {
                ExternalHelp.registrarEnterGlobal(castexto2.getScene(), castexto2,
                        this::isFormatoValido, this::conectar);
            }
        });

        castexto2.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused && isTextoValido()) lanzarPingSiValido();
        });

        castexto2.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isTextoValido()) {
                String normalizado = newVal.toUpperCase();
                if (!normalizado.equals(ultimoTextoValidado)) {
                    ultimoTextoValidado = normalizado;
                    lanzarPingSiValido();
                }
            } else {
                pingIndicator.setVisible(false);
            }
        });

        pingIndicator.setOnMouseClicked(event -> {
            if (isTextoValido()) lanzarPingSiValido();
            else pingIndicator.setVisible(false);
        });

        pingIndicator.setOnMouseEntered(event -> {
            if (isTextoValido())
                pingIndicator.setStyle(pingIndicator.getStyle() + " -fx-cursor: hand;");
        });

        pingIndicator.setOnMouseExited(event ->
            pingIndicator.setStyle(pingIndicator.getStyle().replace(" -fx-cursor: hand;", ""))
        );
    }

    private boolean isFormatoValido(String texto) {
        if (texto == null || texto.isBlank()) return false;
        String upper = texto.toUpperCase();
        return IP_PATTERN.matcher(upper).matches() && !"127.0.0.1".equals(upper)
            || BCN_PATTERN.matcher(upper).matches();
    }

    public boolean isTextoValido() {
        String texto = castexto2.getText();
        return texto != null && !texto.isBlank() && isFormatoValido(texto);
    }

    private List<String> cargarNomenclaturas() {
        try (var is = getClass().getResourceAsStream("/com/labotones/nomenclaturas.txt")) {
            if (is == null) return List.of();
            try (var br = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {
                return br.lines().map(String::toUpperCase).toList();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private void seleccionarSugerencia(String texto) {
        if (bloqueoManager.isBloqueado()) {
            bloqueoManager.mostrarAccesoDenegadoPorBloqueo();
            return;
        }
        castexto2.setText(texto);
        ultimoTextoValidado = texto;
        castexto2.positionCaret(texto.length());
        lanzarPingSiValido();
    }

    // ========================================================================
    // HISTORIAL
    // ========================================================================

    private void cargarHistorial() {
        List<String> historial = HistorialEquipos.getHistorial();
        ExternalHelp.refrescarHistorialChips(historialPane, historial, equipo -> {
            castexto2.setText(equipo);
            ultimoTextoValidado = equipo;
            lanzarPingSiValido();
        });
    }

    private void añadirAlHistorial(String equipo) {
        if (equipo != null && !equipo.isBlank() && isFormatoValido(equipo)) {
            HistorialEquipos.añadir(equipo.toUpperCase());
            cargarHistorial();
        }
    }

    // ========================================================================
    // PING
    // ========================================================================

    private void lanzarPingSiValido() {
        if (!isTextoValido()) {
            pingIndicator.setVisible(false);
            return;
        }
        pingIndicator.setVisible(true);
        PingService.ping(castexto2.getText(), estado -> {
            switch (estado) {
                case COMPROBANDO  -> ExternalHelp.actualizarIndicadorPing(pingIndicator, "comprobando");
                case ALCANZABLE   -> ExternalHelp.actualizarIndicadorPing(pingIndicator, "alcanzable");
                case NO_ALCANZABLE-> ExternalHelp.actualizarIndicadorPing(pingIndicator, "no_alcanzable");
            }
        });
    }

    // ========================================================================
    // TEMA
    // ========================================================================

    private void aplicarTemaInicial() {
        Platform.runLater(() -> {
            if (btnTheme.getScene() != null) {
                VBox root = (VBox) btnTheme.getScene().getRoot();
                ExternalHelp.aplicarTema(root, btnTheme);
            }
        });
    }

    @FXML
    private void cambiarTema() {
        ThemeManager.toggle();
        if (btnTheme.getScene() != null) {
            VBox root = (VBox) btnTheme.getScene().getRoot();
            ThemeHelper.applyTheme(root);
            ThemeHelper.updateThemeButton(btnTheme);
        }
        if (miniManager != null) miniManager.aplicarTemaATodas();
    }

    // ========================================================================
    // BOTONES (delegado a BotonesGridManager)
    // ========================================================================

    public void refrescarBotones() {
        if (gridManager != null) gridManager.cargarBotonesEnBackground();
    }

    // ========================================================================
    // ACCIONES FXML
    // ========================================================================

    @FXML
    private void conectar() {
        if (!isTextoValido()) {
            ExternalHelp.mostrarAdvertencia("Campo obligatorio",
                "Ingrese un ordenador válido (BCNTXXXX, BCNXXXX o IP, excepto 127.0.0.1)");
            return;
        }
        String texto = castexto2.getText().toUpperCase();
        añadirAlHistorial(texto);
        executor.submit(() -> Logic.ejecutar("conectar.bat", texto));
        log("Conectando a: " + texto);
    }

    @FXML
    private void abrirLogs() {
        Navigator.navegar("logs-view.fxml");
        if (miniManager != null) miniManager.cerrarTodas();
    }

    @FXML
    private void abrirCRUD() {
        if (miniManager != null) miniManager.cerrarTodas();
        SyncManager syncManager = new SyncManager();
        String repoPath = syncManager.getRepositorioPath();

        if (repoPath == null || repoPath.isEmpty()) {
            configurarRepositorioYAbrirCRUD();
            return;
        }

        if (!syncManager.repositorioTienePassword()) {
            String newPass = ExternalHelp.mostrarDialogoCrearPassword();
            if (newPass != null && !newPass.isEmpty()) {
                if (syncManager.cambiarPassword(newPass)) {
                    navegarACRUD();
                } else {
                    ExternalHelp.mostrarAdvertencia("Error", "No se pudo guardar la contraseña en el repositorio.");
                }
            }
            return;
        }

        String password = ExternalHelp.mostrarDialogoVerificarPassword();
        if (password != null) {
            if (syncManager.verificarPassword(password)) {
                navegarACRUD();
            } else {
                ExternalHelp.mostrarAdvertencia("Acceso denegado", "Contraseña incorrecta.");
            }
        }
    }

    private void navegarACRUD() {
        Object controller = Navigator.navegar("buttons-view.fxml");
        if (controller instanceof ButtonCRUDController crud) {
            crud.setOnBotonesModificados(this::refrescarBotones);
        }
    }

    private void configurarRepositorioYAbrirCRUD() {
    Platform.runLater(() -> {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Seleccionar carpeta del repositorio");
        File initialDir = new File(System.getProperty("user.home"));
        if (initialDir.exists()) dc.setInitialDirectory(initialDir);

        Stage s = (Stage) castexto2.getScene().getWindow();
        File selectedDir = dc.showDialog(s);

        if (selectedDir != null) {
            String path = selectedDir.getAbsolutePath();
            SyncManager sync = new SyncManager();
            sync.setRepositorioPath(path);
            log("📁 Repositorio configurado en: " + path);

            sync.sincronizarDesdeRepositorio(
                () -> Platform.runLater(() -> {
                    // RECARGAR CACHÉ ANTES DE REFRESCAR LA VISTA (ya viene fusionada
                    // con lo que hubiera en local desde SyncManager)
                    new ButtonService().reload();
                    log("✅ Sincronización completada desde repositorio (botones locales fusionados).");
                    // Subimos el resultado fusionado al repo para que los botones que
                    // solo tenías en local también lleguen a los demás técnicos. En
                    // un hilo aparte para no bloquear la UI mientras se sube por red.
                    new Thread(() -> sync.sincronizarAlRepositorio(
                        () -> log("✅ Botones fusionados subidos al repositorio."),
                        () -> log("⚠️ No se pudo subir la fusión al repositorio, se queda solo en local por ahora.")
                    )).start();
                    ExternalHelp.mostrarInformacion("Repositorio configurado",
                        "Repositorio configurado correctamente en:\n" + path);
                    refrescarBotones();
                    abrirCRUD();
                }),
                () -> Platform.runLater(() -> {
                    log("❌ Error al sincronizar. Usando datos locales.");
                    ExternalHelp.mostrarAdvertencia("Error de sincronización",
                        "No se pudo sincronizar con el repositorio.");
                    // Aunque falle, si el usuario tenía botones locales, los mostramos
                    new ButtonService().reload();
                    refrescarBotones();
                    abrirCRUD();
                })
            );
        } else {
            log("ℹ Configuración de repositorio cancelada.");
            ExternalHelp.mostrarAdvertencia("Repositorio necesario",
                "Para administrar los botones es necesario configurar un repositorio.");
        }
    });
}

    // ========================================================================
    // MINI VENTANAS (delegado a MiniWindowManager)
    // ========================================================================

    @FXML
    private void abrirMini() {
        if (miniManager != null) miniManager.abrirMini();
    }

    @FXML
    private void cerrarTodasLasMinis() {
        if (miniManager != null) miniManager.cerrarTodas();
    }

    // ========================================================================
    // LOGS Y SHUTDOWN
    // ========================================================================

    public void log(String mensaje) {
        ExternalHelp.agregarLogVisual(logArea, mensaje, MAX_LOGS_VISUALES);
        Help.log(mensaje);
    }

    public void shutdown() {
        ConfigManager.setBloqueado(bloqueoManager.isBloqueado());
        if (miniManager != null) miniManager.cerrarTodas();
        if (syncController != null) syncController.stop();
        // executor.shutdown() NO bloquea: solo deja de aceptar tareas nuevas.
        // Antes se esperaba aquí mismo, en el hilo de JavaFX, hasta 3 segundos
        // a que las tareas en curso terminaran (awaitTermination). Como esto se
        // llama cada vez que se vuelve a la pantalla principal (App.setActiveController
        // cierra el Controller anterior), si había algo tardando en el executor viejo
        // la ventana se quedaba congelada esos segundos. Ahora esa espera se hace
        // en un hilo aparte para cada pool: si la tarea no termina sola, se fuerza
        // su cierre, pero sin bloquear la interfaz.
        cerrarExecutorSinBloquear(executor, "labotones-executor-watchdog");
        cerrarExecutorSinBloquear(gridExecutor, "labotones-grid-executor-watchdog");
    }

    private void cerrarExecutorSinBloquear(ExecutorService es, String nombreHiloWatchdog) {
        if (es == null || es.isShutdown()) return;
        es.shutdown();
        Thread watchdog = new Thread(() -> {
            try {
                if (!es.awaitTermination(3, TimeUnit.SECONDS)) {
                    es.shutdownNow();
                }
            } catch (InterruptedException e) {
                es.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }, nombreHiloWatchdog);
        watchdog.setDaemon(true);
        watchdog.start();
    }
}