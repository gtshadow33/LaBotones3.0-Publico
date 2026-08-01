package com.labotones.SecondStatges;

import com.labotones.Navigator;
import com.labotones.Class_help.Button_prop;
import com.labotones.Class_help.ConfigManager;
import com.labotones.Class_help.ButtonService;
import com.labotones.Class_help.ButtonService.Result;
import com.labotones.Class_help.SyncManager;
import com.labotones.Class_help.ThemeHelper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ButtonCRUDController {

    @FXML private TableView<Button_prop> table;
    @FXML private TableColumn<Button_prop, String> colName, colDesc, colCategoria;
    @FXML private TextField fieldName, fieldDesc;
    @FXML private ComboBox<String> comboBat, comboIcono, comboCategoriaForm;
    @FXML private CheckBox checkArgumento, checkExcluidoBloqueo, checkRequiereConfirmacion;
    @FXML private Button btnGuardar, btnEliminar, btnCancelar, btnCambiarPassword;
    @FXML private Label lblStatus, lblRepositorio;
    @FXML private Button btnSeleccionarRepositorio;

    private final ButtonService service = new ButtonService();
    private Runnable onBotonesModificados;
    private SyncManager syncManager;
    private boolean sincronizando = false;

    // Caché estática para listados de archivos
    private static List<String> cachedBats = null;
    private static List<String> cachedIcons = null;

    public void setOnBotonesModificados(Runnable callback) {
        this.onBotonesModificados = callback;
    }

    @FXML
    public void initialize() {
        // ========== CONFIGURACIÓN UI (RÁPIDO, SIN I/O) ==========
        configurarColumnas();
        configurarSeleccion();
        configurarValidacionEnVivo();
        configurarDeseleccionConClick();
        configurarPreviewIcono();

        syncManager = new SyncManager();
        cargarRepositorio();

        // ========== CARGA ASÍNCRONA (TODO en background) ==========
        cargarDatosEnBackground();

        Platform.runLater(() -> {
            if (btnGuardar.getScene() != null) {
                VBox rootVBox = (VBox) btnGuardar.getScene().getRoot();
                ThemeHelper.applyTheme(rootVBox);
            }
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    // ========================================================================
    // CARGA ASÍNCRONA EN BACKGROUND
    // ========================================================================

    private void cargarDatosEnBackground() {
        // Deshabilitar botones mientras se carga
        btnGuardar.setDisable(true);
        btnEliminar.setDisable(true);
        btnSeleccionarRepositorio.setDisable(true);
        table.setPlaceholder(new Label("Cargando datos..."));

        new Thread(() -> {
            try {
                // 1. Cargar archivos (I/O) desde caché
                List<String> bats = getCachedBats();
                List<String> icons = getCachedIcons();

                // 2. Cargar botones (desde caché de ButtonService)
                List<Button_prop> botones = service.getAll();

                // 3. Extraer categorías de los botones
                java.util.LinkedHashSet<String> categorias = new java.util.LinkedHashSet<>();
                botones.stream()
                        .map(Button_prop::getCategoria)
                        .filter(c -> c != null && !c.isBlank())
                        .sorted(String::compareToIgnoreCase)
                        .forEach(categorias::add);
                if (categorias.isEmpty()) {
                    categorias.add(Button_prop.CATEGORIA_DEFECTO);
                }

                // 4. Actualizar UI en hilo de FX
                Platform.runLater(() -> {
                    // Combos
                    comboBat.setItems(FXCollections.observableArrayList(bats));
                    comboIcono.setItems(FXCollections.observableArrayList(icons));
                    if (comboIcono.getItems().isEmpty() || !comboIcono.getItems().get(0).isEmpty()) {
                        comboIcono.getItems().add(0, "");
                    }
                    comboCategoriaForm.setItems(FXCollections.observableArrayList(categorias));
                    comboCategoriaForm.setValue(Button_prop.CATEGORIA_DEFECTO);

                    // Tabla
                    table.setItems(FXCollections.observableArrayList(botones));

                    // Habilitar botones
                    btnGuardar.setDisable(false);
                    btnEliminar.setDisable(false);
                    btnSeleccionarRepositorio.setDisable(false);
                    setModoCrear();
                    table.setPlaceholder(new Label("Sin botones configurados"));

                    mostrarStatus("✅ " + botones.size() + " botones cargados", false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnGuardar.setDisable(false);
                    btnEliminar.setDisable(false);
                    btnSeleccionarRepositorio.setDisable(false);
                    table.setPlaceholder(new Label("❌ Error al cargar datos"));
                    mostrarStatus("❌ Error: " + e.getMessage(), true);
                });
                e.printStackTrace();
            }
        }).start();
    }

    // ========================================================================
    // CACHÉ DE ARCHIVOS
    // ========================================================================

    private List<String> getCachedBats() {
        if (cachedBats == null) {
            cachedBats = listarArchivos("bats/", ".bat");
        }
        return cachedBats;
    }

    private List<String> getCachedIcons() {
        if (cachedIcons == null) {
            cachedIcons = listarArchivos("icons/", ".png", ".jpg", ".jpeg", ".gif");
        }
        return cachedIcons;
    }

    private void invalidarCacheArchivos() {
        cachedBats = null;
        cachedIcons = null;
    }

    // ========================================================================
    // REPOSITORIO COMPARTIDO
    // ========================================================================

    private void cargarRepositorio() {
        String ruta = syncManager.getRepositorioPath();
        if (ruta == null || ruta.isEmpty()) {
            lblRepositorio.setText("No configurado");
            lblRepositorio.setStyle("-fx-text-fill: #e74c3c;");
        } else {
            lblRepositorio.setText(ruta);
            lblRepositorio.setStyle("-fx-text-fill: #2ecc71;");
        }
    }

    @FXML
    private void seleccionarRepositorio() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Seleccionar carpeta del repositorio compartido");
        String current = syncManager.getRepositorioPath();
        if (current != null && !current.isEmpty()) {
            File currentDir = new File(current);
            if (currentDir.exists()) dirChooser.setInitialDirectory(currentDir);
        }
        File selected = dirChooser.showDialog(lblRepositorio.getScene().getWindow());
        if (selected != null) {
            syncManager.setRepositorioPath(selected.getAbsolutePath());

            mostrarStatus("🔄 Sincronizando en segundo plano...", false);
            new Thread(() -> {
                syncManager.sincronizarDesdeRepositorio(
                    () -> Platform.runLater(() -> {
                        service.reload();
                        invalidarCacheArchivos();
                        cargarDatosEnBackground();
                        cargarRepositorio();
                        mostrarStatus("✅ Repositorio configurado y sincronizado.", false);
                    }),
                    () -> Platform.runLater(() -> {
                        service.reload();
                        invalidarCacheArchivos();
                        cargarDatosEnBackground();
                        cargarRepositorio();
                        mostrarStatus("⚠️ Repositorio configurado, error al sincronizar.", true);
                    })
                );
            }).start();
        }
    }

    // ========================================================================
    // MÉTODOS CRUD
    // ========================================================================

    @FXML
    private void guardar() {
        if (!validarFormulario()) return;
        
        Button_prop seleccionado = table.getSelectionModel().getSelectedItem();
        boolean esCrear = (seleccionado == null);
        Button_prop botonModificado = esCrear ? new Button_prop() : seleccionado.copy();
        construirDesdeFormulario(botonModificado);
        
        Result resultado = esCrear 
            ? service.add(botonModificado) 
            : service.update(botonModificado);
        
        if (resultado == Result.OK) {
            invalidarCacheArchivos();
            refrescarTabla();
            if (onBotonesModificados != null) onBotonesModificados.run();
            limpiarFormulario();
            table.getSelectionModel().clearSelection();
            cargarArchivosEnCombos();
            cargarCategoriasEnCombo();
            mostrarStatus("Botón " + (esCrear ? "creado" : "actualizado") + " correctamente ✓", false);
            
            sincronizarAlRepositorio("Botón " + (esCrear ? "creado" : "actualizado"));
        } else {
            manejarResultado(resultado, esCrear ? "creado" : "actualizado");
        }
    }

    @FXML
    private void eliminar() {
        Button_prop sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarStatus("Selecciona un botón primero", true);
            return;
        }
        if (confirmar("¿Eliminar botón?", "Se eliminará \"" + sel.getName() + "\" de forma permanente.")) {
            String nombreEliminado = sel.getName();
            Result resultado = service.delete(sel.getID());
            
            if (resultado == Result.OK) {
                invalidarCacheArchivos();
                refrescarTabla();
                if (onBotonesModificados != null) onBotonesModificados.run();
                limpiarFormulario();
                table.getSelectionModel().clearSelection();
                mostrarStatus("Botón eliminado correctamente ✓", false);
                
                sincronizarAlRepositorio("Botón eliminado: " + nombreEliminado);
            } else {
                manejarResultado(resultado, "eliminado");
            }
        }
    }

    private void sincronizarAlRepositorio(String accion) {
        if (sincronizando) return;
        
        String repoPath = syncManager.getRepositorioPath();
        if (repoPath == null || repoPath.isEmpty()) {
            mostrarStatus("⚠️ Botón guardado localmente. Repositorio no configurado.", false);
            return;
        }
        
        sincronizando = true;
        mostrarStatus("🔄 Sincronizando al repositorio... (" + accion + ")", false);
        
        btnGuardar.setDisable(true);
        btnEliminar.setDisable(true);
        btnSeleccionarRepositorio.setDisable(true);
        
        new Thread(() -> {
            syncManager.sincronizarAlRepositorio(
                () -> Platform.runLater(() -> {
                    sincronizando = false;
                    btnGuardar.setDisable(false);
                    btnEliminar.setDisable(false);
                    btnSeleccionarRepositorio.setDisable(false);
                    mostrarStatus("✅ " + accion + " y sincronizado al repositorio.", false);
                    ConfigManager.reload();
                }),
                () -> Platform.runLater(() -> {
                    sincronizando = false;
                    btnGuardar.setDisable(false);
                    btnEliminar.setDisable(false);
                    btnSeleccionarRepositorio.setDisable(false);
                    mostrarStatus("⚠️ " + accion + " localmente, error al sincronizar.", true);
                }),
                () -> Platform.runLater(() -> {
                    sincronizando = false;
                    btnGuardar.setDisable(false);
                    btnEliminar.setDisable(false);
                    btnSeleccionarRepositorio.setDisable(false);
                    mostrarStatus("✅ " + accion + " (sincronización en curso).", false);
                })
            );
        }).start();
    }

    // ========================================================================
    // CONFIGURACIÓN DE UI
    // ========================================================================

    private void configurarPreviewIcono() {
        javafx.util.Callback<javafx.scene.control.ListView<String>, javafx.scene.control.ListCell<String>> cellFactory =
            lv -> new javafx.scene.control.ListCell<>() {
                private final javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
                {
                    imageView.setFitWidth(20);
                    imageView.setFitHeight(20);
                    imageView.setPreserveRatio(true);
                }

                @Override
                protected void updateItem(String nombreIcono, boolean empty) {
                    super.updateItem(nombreIcono, empty);
                    if (empty || nombreIcono == null || nombreIcono.isBlank()) {
                        setText(empty ? null : "(Sin icono)");
                        setGraphic(null);
                        return;
                    }
                    setText(nombreIcono);
                    Image img = cargarImagenIcono(nombreIcono);
                    if (img != null) {
                        imageView.setImage(img);
                        setGraphic(imageView);
                    } else {
                        setGraphic(null);
                    }
                }
            };

        comboIcono.setCellFactory(cellFactory);
        comboIcono.setButtonCell(cellFactory.call(null));
    }

    private Image cargarImagenIcono(String nombreIcono) {
        File file = new File("icons/" + nombreIcono);
        return com.labotones.Class_help.IconCache.obtener(file, 20, 20);
    }

    private void configurarColumnas() {
        colName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));
        colDesc.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDescription()));
        colCategoria.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCategoria()));
        colName.prefWidthProperty().bind(table.widthProperty().multiply(0.28));
        colCategoria.prefWidthProperty().bind(table.widthProperty().multiply(0.24));
        colDesc.prefWidthProperty().bind(table.widthProperty().multiply(0.48));
    }

    private void configurarSeleccion() {
        table.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) -> {
            if (seleccionado != null) {
                cargarEnFormulario(seleccionado);
                setModoEditar();
            } else {
                limpiarFormulario();
                setModoCrear();
            }
        });
    }

    private void configurarDeseleccionConClick() {
        table.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (event.getTarget() instanceof TableView ||
                    (event.getTarget() instanceof Label && ((Label)event.getTarget()).getText().equals("Sin botones configurados"))) {
                    table.getSelectionModel().clearSelection();
                }
            }
        });
    }

    private void configurarValidacionEnVivo() {
        fieldName.textProperty().addListener((obs, o, n) -> {
            if (n.isBlank()) fieldName.setStyle("-fx-border-color: #e74c3c;");
            else fieldName.setStyle("");
        });
    }

    private void cargarArchivosEnCombos() {
        comboBat.setItems(FXCollections.observableArrayList(getCachedBats()));
        comboIcono.setItems(FXCollections.observableArrayList(getCachedIcons()));
        if (comboIcono.getItems().isEmpty() || !comboIcono.getItems().get(0).isEmpty()) {
            comboIcono.getItems().add(0, "");
        }
    }

    private List<String> listarArchivos(String directorio, String... extensiones) {
        List<String> archivos = new ArrayList<>();
        File dir = new File(directorio);
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    String name = f.getName();
                    for (String ext : extensiones) {
                        if (name.toLowerCase().endsWith(ext)) {
                            archivos.add(name);
                            break;
                        }
                    }
                }
            }
        }
        archivos.sort(String::compareToIgnoreCase);
        return archivos;
    }

    private void refrescarTabla() {
        table.setItems(FXCollections.observableArrayList(service.getAll()));
    }

    private void cargarCategoriasEnCombo() {
        String seleccionActual = comboCategoriaForm.getValue();
        java.util.LinkedHashSet<String> categorias = new java.util.LinkedHashSet<>();
        service.getAll().stream()
                .map(Button_prop::getCategoria)
                .filter(c -> c != null && !c.isBlank())
                .sorted(String::compareToIgnoreCase)
                .forEach(categorias::add);
        if (categorias.isEmpty()) categorias.add(Button_prop.CATEGORIA_DEFECTO);
        comboCategoriaForm.setItems(FXCollections.observableArrayList(categorias));
        if (seleccionActual != null && categorias.contains(seleccionActual)) {
            comboCategoriaForm.setValue(seleccionActual);
        } else {
            comboCategoriaForm.setValue(Button_prop.CATEGORIA_DEFECTO);
        }
    }

    private void limpiarFormulario() {
        fieldName.clear();
        fieldDesc.clear();
        comboBat.setValue(null);
        comboIcono.setValue(null);
        comboCategoriaForm.setValue(null);
        checkArgumento.setSelected(false);
        checkExcluidoBloqueo.setSelected(false);
        checkRequiereConfirmacion.setSelected(false);
        fieldName.setStyle("");
        lblStatus.setText("");
    }

    private void setModoCrear() {
        btnGuardar.setText("Crear botón");
        btnEliminar.setDisable(true);
        btnCancelar.setDisable(true);
    }

    private void setModoEditar() {
        btnGuardar.setText("Guardar cambios");
        btnEliminar.setDisable(false);
        btnCancelar.setDisable(false);
    }

    private void cargarEnFormulario(Button_prop b) {
        fieldName.setText(b.getName());
        fieldDesc.setText(b.getDescription());
        comboBat.setValue(b.getLocateBat());
        comboIcono.setValue(b.getIcono());
        comboCategoriaForm.setValue(b.getCategoria());
        checkArgumento.setSelected(b.isArgumento());
        checkExcluidoBloqueo.setSelected(b.isExcluidoBloqueo());
        checkRequiereConfirmacion.setSelected(b.isRequiereConfirmacion());
    }

    private void aplicarFormulario(Button_prop b) {
        b.setName(fieldName.getText().trim());
        b.setDescription(fieldDesc.getText().trim());
        b.setLocateBat(comboBat.getValue());
        b.setIcono(comboIcono.getValue() != null ? comboIcono.getValue() : "");
        b.setCategoria(comboCategoriaForm.getValue());
        b.setArgumento(checkArgumento.isSelected());
        b.setExcluidoBloqueo(checkExcluidoBloqueo.isSelected());
        b.setRequiereConfirmacion(checkRequiereConfirmacion.isSelected());
    }

    private Button_prop construirDesdeFormulario(Button_prop b) {
        aplicarFormulario(b);
        return b;
    }

    private boolean validarFormulario() {
        boolean ok = true;
        if (fieldName.getText().isBlank()) {
            fieldName.setStyle("-fx-border-color: #e74c3c;");
            ok = false;
        }
        if (comboBat.getValue() == null || comboBat.getValue().isBlank()) {
            comboBat.setStyle("-fx-border-color: #e74c3c;");
            ok = false;
        } else {
            comboBat.setStyle("");
        }
        if (!ok) mostrarStatus("Nombre y archivo .bat son obligatorios", true);
        return ok;
    }

    private void manejarResultado(Result resultado, String accion) {
        switch (resultado) {
            case OK -> {
                invalidarCacheArchivos();
                refrescarTabla();
                if (onBotonesModificados != null) onBotonesModificados.run();
                limpiarFormulario();
                table.getSelectionModel().clearSelection();
                cargarArchivosEnCombos();
                cargarCategoriasEnCombo();
                mostrarStatus("Botón " + accion + " correctamente ✓", false);
            }
            case INVALID -> mostrarStatus("Datos inválidos, revisa el formulario", true);
            case NOT_FOUND -> mostrarStatus("No se encontró el botón a modificar", true);
            case IO_ERROR -> mostrarStatus("Error al guardar en disco", true);
        }
    }

    private void mostrarStatus(String msg, boolean esError) {
        lblStatus.setText(msg);
        lblStatus.setStyle(esError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #2ecc71;");
    }

    private boolean confirmar(String titulo, String contenido) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(contenido);
        return a.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }

    @FXML
    private void cancelar() {
        limpiarFormulario();
        table.getSelectionModel().clearSelection();
        mostrarStatus("Edición cancelada", false);
    }

    @FXML
    private void volver() {
        Navigator.inicio();
    }

    // ========================================================================
    // CAMBIAR CONTRASEÑA
    // ========================================================================

    @FXML
    private void cambiarPassword() {
        String repoPath = syncManager.getRepositorioPath();
        if (repoPath == null || repoPath.isEmpty()) {
            mostrarStatus("❌ No hay repositorio configurado.", true);
            return;
        }

        boolean tienePassword = syncManager.repositorioTienePassword();

        if (!tienePassword) {
            TextInputDialog crearDialog = new TextInputDialog();
            crearDialog.setTitle("Crear contraseña del repositorio");
            crearDialog.setHeaderText("No hay contraseña configurada en el repositorio.\nIntroduce una nueva contraseña:");
            crearDialog.setContentText("Nueva contraseña:");
            crearDialog.getEditor().setPromptText("Contraseña");

            Optional<String> newPass = crearDialog.showAndWait();
            if (newPass.isPresent() && !newPass.get().isEmpty()) {
                if (syncManager.cambiarPassword(newPass.get())) {
                    mostrarStatus("✅ Contraseña creada en el repositorio.", false);
                } else {
                    mostrarStatus("❌ Error al crear contraseña en el repositorio.", true);
                }
            }
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cambiar contraseña del repositorio");
        dialog.setHeaderText("Introduce la contraseña actual y la nueva.");

        PasswordField oldPassField = new PasswordField();
        oldPassField.setPromptText("Contraseña actual");
        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("Nueva contraseña");
        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Confirmar nueva contraseña");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Contraseña actual:"), 0, 0);
        grid.add(oldPassField, 1, 0);
        grid.add(new Label("Nueva contraseña:"), 0, 1);
        grid.add(newPassField, 1, 1);
        grid.add(new Label("Confirmar nueva:"), 0, 2);
        grid.add(confirmPassField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return newPassField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String oldPass = oldPassField.getText();
            String newPass = newPassField.getText();
            String confirmPass = confirmPassField.getText();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                mostrarStatus("❌ Todos los campos son obligatorios.", true);
                return;
            }

            if (!newPass.equals(confirmPass)) {
                mostrarStatus("❌ Las contraseñas nuevas no coinciden.", true);
                return;
            }

            if (!syncManager.verificarPassword(oldPass)) {
                mostrarStatus("❌ Contraseña actual incorrecta.", true);
                return;
            }

            if (syncManager.cambiarPassword(newPass)) {
                mostrarStatus("✅ Contraseña del repositorio actualizada correctamente.", false);
            } else {
                mostrarStatus("❌ Error al actualizar la contraseña en el repositorio.", true);
            }
        }
    }
}