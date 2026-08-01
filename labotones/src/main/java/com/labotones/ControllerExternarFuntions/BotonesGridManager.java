package com.labotones.ControllerExternarFuntions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.labotones.Class_help.Button_prop;
import com.labotones.Class_help.ButtonService;
import com.labotones.Class_help.CountdownDialog;
import com.labotones.ControllerExternarFuntions.BloqueoManager;
import com.labotones.ControllerExternarFuntions.ExternalHelp;
import com.labotones.logic.Logic;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

/**
 * Gestiona la carga, filtrado y renderizado de botones en el GridPane principal.
 */
public class BotonesGridManager {

    private static final String TODAS_LAS_CATEGORIAS = "Todas las categorías";

    private final ButtonService buttonService;
    private final GridPane contenedorBotones;
    private final TextField campoBuscar;
    private final ComboBox<String> comboCategoria;
    private final BloqueoManager bloqueoManager;
    /** Hilo dedicado solo a cargar/leer la lista de botones (no compite con las ejecuciones). */
    private final ExecutorService gridExecutor;
    /** Pool para lanzar los .bat de los botones al pulsarlos. */
    private final ExecutorService executor;
    private final java.util.function.Consumer<String> logger;
    private final java.util.function.Supplier<Boolean> isTextoValido;
    private final java.util.function.Supplier<String> getTexto;
    private final java.util.function.Consumer<String> añadirAlHistorial;

    private final List<Button_prop> todosLosBotones = new ArrayList<>();
    private int col = 0;
    private int row = 0;

    public BotonesGridManager(
            ButtonService buttonService,
            GridPane contenedorBotones,
            TextField campoBuscar,
            ComboBox<String> comboCategoria,
            BloqueoManager bloqueoManager,
            ExecutorService gridExecutor,
            ExecutorService executor,
            java.util.function.Consumer<String> logger,
            java.util.function.Supplier<Boolean> isTextoValido,
            java.util.function.Supplier<String> getTexto,
            java.util.function.Consumer<String> añadirAlHistorial) {
        this.buttonService = buttonService;
        this.contenedorBotones = contenedorBotones;
        this.campoBuscar = campoBuscar;
        this.comboCategoria = comboCategoria;
        this.bloqueoManager = bloqueoManager;
        this.gridExecutor = gridExecutor;
        this.executor = executor;
        this.logger = logger;
        this.isTextoValido = isTextoValido;
        this.getTexto = getTexto;
        this.añadirAlHistorial = añadirAlHistorial;
    }

    // ========================================================================
    // CONFIGURACIÓN INICIAL
    // ========================================================================

    public void configurarBusquedaYCategorias() {
        if (campoBuscar != null) {
            // Debounce: repintar el grid entero (incluidos los iconos) en CADA
            // tecla es lo que hacía sentir la búsqueda poco fluida con muchos
            // botones. Con este pequeño retardo, solo se repinta cuando el
            // usuario deja de escribir un momento (200ms), no en cada letra.
            javafx.animation.PauseTransition debounce =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
            debounce.setOnFinished(e -> aplicarFiltroBotones());
            campoBuscar.textProperty().addListener((obs, oldV, newV) -> debounce.playFromStart());
        }
        if (comboCategoria != null) {
            comboCategoria.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldV, newV) -> aplicarFiltroBotones());
        }
    }

    // ========================================================================
    // CARGA Y REFRESCO
    // ========================================================================

    public void cargarBotonesEnBackground() {
        gridExecutor.submit(() -> {
            try {
                List<Button_prop> botones = buttonService.getAll();
                Platform.runLater(() -> {
                    todosLosBotones.clear();
                    todosLosBotones.addAll(botones);
                    actualizarCategoriasDisponibles();
                    aplicarFiltroBotones();
                });
                Platform.runLater(() -> bloqueoManager.aplicarBloqueoAGrid());
            } catch (Exception e) {
                logger.accept("❌ Error cargando botones: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void refrescar() {
        cargarBotonesEnBackground();
    }

    // ========================================================================
    // FILTRADO
    // ========================================================================

    private void actualizarCategoriasDisponibles() {
        if (comboCategoria == null) return;

        String seleccionActual = comboCategoria.getValue();

        java.util.LinkedHashSet<String> categorias = new java.util.LinkedHashSet<>();
        categorias.add(TODAS_LAS_CATEGORIAS);
        todosLosBotones.stream()
                .map(Button_prop::getCategoria)
                .filter(c -> c != null && !c.isBlank())
                .sorted(String::compareToIgnoreCase)
                .forEach(categorias::add);

        comboCategoria.setItems(FXCollections.observableArrayList(categorias));

        if (seleccionActual != null && categorias.contains(seleccionActual)) {
            comboCategoria.setValue(seleccionActual);
        } else {
            comboCategoria.setValue(TODAS_LAS_CATEGORIAS);
        }
    }

    public void aplicarFiltroBotones() {
        String textoBusqueda = campoBuscar != null && campoBuscar.getText() != null
                ? campoBuscar.getText().trim().toLowerCase()
                : "";
        String categoriaSeleccionada = comboCategoria != null ? comboCategoria.getValue() : null;

        List<Button_prop> filtrados = new ArrayList<>();
        for (Button_prop b : todosLosBotones) {
            boolean coincideTexto = textoBusqueda.isEmpty()
                    || b.getName().toLowerCase().contains(textoBusqueda)
                    || b.getDescription().toLowerCase().contains(textoBusqueda)
                    || b.getCategoria().toLowerCase().contains(textoBusqueda);

            boolean coincideCategoria = categoriaSeleccionada == null
                    || categoriaSeleccionada.equals(TODAS_LAS_CATEGORIAS)
                    || categoriaSeleccionada.equalsIgnoreCase(b.getCategoria());

            if (coincideTexto && coincideCategoria) filtrados.add(b);
        }

        renderizarBotones(filtrados);
    }

    // ========================================================================
    // RENDERIZADO
    // ========================================================================

    private void renderizarBotones(List<Button_prop> botones) {
        contenedorBotones.getChildren().clear();
        col = 0;
        row = 0;

        for (Button_prop b : botones) {
            Button btn = crearBoton(b);
            agregarBotonAGrid(btn, b);
        }

        bloqueoManager.aplicarBloqueoAGrid();
    }

    private Button crearBoton(Button_prop b) {
        Button btn = new Button(b.getName());
        btn.setPrefSize(120, 95);
        btn.setStyle("-fx-font-size: 12px; -fx-alignment: center;");
        btn.getProperties().put("excluidoBloqueo", b.isExcluidoBloqueo());

        try {
            File file = new File("icons/" + b.getIcono());
            Image img = com.labotones.Class_help.IconCache.obtener(file);
            if (img != null) {
                ImageView icon = new ImageView(img);
                icon.setFitWidth(48);
                icon.setFitHeight(48);
                btn.setGraphic(icon);
                btn.setContentDisplay(ContentDisplay.TOP);
            }
        } catch (Exception e) {
            // Icono no encontrado, se ignora
        }

        btn.setTooltip(new Tooltip(
                (b.isRequiereConfirmacion() ? "⏱ Requiere confirmación (5 seg)\n" : "") + b.getDescription()));
        return btn;
    }

    private void agregarBotonAGrid(Button btn, Button_prop b) {
        btn.setOnAction(e -> {
            if (bloqueoManager.isBloqueado() && !b.isExcluidoBloqueo()) {
                bloqueoManager.mostrarAccesoDenegadoPorBloqueo();
                return;
            }

            if (b.isArgumento()) {
                if (!isTextoValido.get()) {
                    ExternalHelp.mostrarAdvertencia("Campo obligatorio",
                        "Es necesario ingresar un ordenador válido (BCNTXXXX, BCNXXXX o IP, excepto 127.0.0.1)");
                    return;
                }
                String pc = getTexto.get().toUpperCase();
                añadirAlHistorial.accept(pc);

                if (b.isRequiereConfirmacion()) {
                    CountdownDialog.run(b.getName(), b.getDescription() + ": " + pc, () -> {
                        logger.accept("▶ " + b.getDescription() + ": " + pc);
                        executor.submit(() -> Logic.ejecutar(b.getLocateBat(), pc));
                    });
                } else {
                    logger.accept("▶ " + b.getDescription() + ": " + pc);
                    executor.submit(() -> Logic.ejecutar(b.getLocateBat(), pc));
                }
            } else {
                if (b.isRequiereConfirmacion()) {
                    CountdownDialog.run(b.getName(), b.getDescription(), () -> {
                        logger.accept("▶ " + b.getDescription());
                        executor.submit(() -> Logic.ejecutar(b.getLocateBat()));
                    });
                } else {
                    logger.accept("▶ " + b.getDescription());
                    executor.submit(() -> Logic.ejecutar(b.getLocateBat()));
                }
            }
        });

        contenedorBotones.add(btn, col, row);
        col++;
        if (col == 5) {
            col = 0;
            row++;
        }
    }
}