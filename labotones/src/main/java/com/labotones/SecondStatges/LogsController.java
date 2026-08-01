package com.labotones.SecondStatges;

import com.labotones.Help;
import com.labotones.Navigator;

import com.labotones.Class_help.ThemeHelper;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogsController {

    private static final int MAX_LOGS = 50; // Número máximo de líneas a mostrar

    @FXML private TextArea textArea;
    @FXML private TextField searchField;
    @FXML private Label searchStatus;
    @FXML private DatePicker fechaExportPicker;
    
    private String fullLogText = "";      // Texto normalizado con \n
    private List<MatchResult> matchResults = new ArrayList<>();
    private int currentMatchIndex = -1;
    
    private static class MatchResult {
        int startPos;
        int endPos;
        int lineNumber;
        
        MatchResult(int startPos, int endPos, int lineNumber) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.lineNumber = lineNumber;
        }
    }

    public void initialize() {
        actualizar();
        Platform.runLater(() -> {
            if (textArea.getScene() != null) {
                VBox rootVBox = (VBox) textArea.getScene().getRoot();
                ThemeHelper.applyTheme(rootVBox);
            }
        });
    }

    @FXML
    private void actualizar() {
        // Leer logs y NORMALIZAR saltos de línea a \n
        String rawLog = Help.leerLogs();
        String normalizedLog = rawLog.replace("\r\n", "\n").replace("\r", "\n");
        
        // ========== LIMITAR A LOS ÚLTIMOS 100 LOGS ==========
        String[] lineas = normalizedLog.split("\n", -1);
        
        // Si hay más de MAX_LOGS líneas, quedarse solo con las últimas MAX_LOGS
        if (lineas.length > MAX_LOGS) {
            StringBuilder sb = new StringBuilder();
            int startIndex = lineas.length - MAX_LOGS;
            for (int i = startIndex; i < lineas.length; i++) {
                sb.append(lineas[i]);
                if (i < lineas.length - 1) {
                    sb.append("\n");
                }
            }
            fullLogText = sb.toString();
        } else {
            fullLogText = normalizedLog;
        }
        
        textArea.setText(fullLogText);
        Platform.runLater(() -> {
            textArea.positionCaret(textArea.getLength());
            textArea.setScrollTop(Double.MAX_VALUE);
        });
        limpiarBusqueda();
    }

    @FXML
    private void volver() {
        Navigator.inicio();
    }

    @FXML
    private void exportarCSV() {
        LocalDate filtro = fechaExportPicker.getValue(); // null = todas las fechas

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar logs a CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo CSV (*.csv)", "*.csv"));

        String nombreSugerido = (filtro != null)
                ? "logs_" + filtro + ".csv"
                : "logs_completo.csv";
        fileChooser.setInitialFileName(nombreSugerido);

        Window ventana = textArea.getScene() != null ? textArea.getScene().getWindow() : null;
        File destino = fileChooser.showSaveDialog(ventana);
        if (destino == null) {
            return; // el usuario canceló
        }

        try {
            int exportadas = Help.exportarLogsCSV(destino, filtro);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Exportación completada");
            alert.setHeaderText(null);
            if (exportadas == 0) {
                alert.setContentText("No se encontraron registros para la fecha seleccionada.");
            } else {
                alert.setContentText("Se han exportado " + exportadas + " líneas a:\n" + destino.getAbsolutePath());
            }
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al exportar");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo exportar el CSV: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void buscarTexto() {
        String searchText = searchField.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            limpiarBusqueda();
            return;
        }
        
        realizarBusquedaPorCoincidencias(searchText);
    }
    
    @FXML
    private void buscarAnterior() {
        if (matchResults.isEmpty()) {
            buscarTexto();
            return;
        }
        
        currentMatchIndex--;
        if (currentMatchIndex < 0) {
            currentMatchIndex = matchResults.size() - 1;
        }
        
        seleccionarMatchActual();
    }
    
    @FXML
    private void buscarSiguiente() {
        if (matchResults.isEmpty()) {
            buscarTexto();
            return;
        }
        
        currentMatchIndex++;
        if (currentMatchIndex >= matchResults.size()) {
            currentMatchIndex = 0;
        }
        
        seleccionarMatchActual();
    }
    
    private void realizarBusquedaPorCoincidencias(String searchText) {
        matchResults.clear();
        
        String[] palabras = searchText.trim().split("\\s+");
        if (palabras.length == 0) return;
        
        List<Pattern> linePatterns = new ArrayList<>();
        for (String palabra : palabras) {
            String regex = "\\b" + Pattern.quote(palabra.toLowerCase());
            linePatterns.add(Pattern.compile(regex));
        }
        
        List<Pattern> wordPatterns = new ArrayList<>();
        for (String palabra : palabras) {
            String regex = "\\b" + Pattern.quote(palabra.toLowerCase()) + "[a-zA-Z0-9_]*";
            wordPatterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
        }
        
        String[] lineas = fullLogText.split("\n", -1);
        int globalPos = 0;
        
        for (int lineNum = 0; lineNum < lineas.length; lineNum++) {
            String linea = lineas[lineNum];
            String lowerLinea = linea.toLowerCase();
            
            boolean todasPresentes = true;
            for (Pattern p : linePatterns) {
                if (!p.matcher(lowerLinea).find()) {
                    todasPresentes = false;
                    break;
                }
            }
            
            if (todasPresentes) {
                List<MatchResult> matchesInLine = new ArrayList<>();
                for (Pattern p : wordPatterns) {
                    Matcher m = p.matcher(linea);
                    while (m.find()) {
                        int start = globalPos + m.start();
                        int end = globalPos + m.end();
                        matchesInLine.add(new MatchResult(start, end, lineNum));
                    }
                }
                matchesInLine.sort((a, b) -> Integer.compare(a.startPos, b.startPos));
                matchResults.addAll(matchesInLine);
            }
            
            globalPos += linea.length() + 1;
        }
        
        actualizarUIResultados();
    }
    
    private void actualizarUIResultados() {
        if (!matchResults.isEmpty()) {
            currentMatchIndex = 0;
            seleccionarMatchActual();
            searchStatus.setText(matchResults.size() + " coincidencias");
            searchStatus.setStyle("-fx-text-fill: #4CAF50;");
        } else {
            currentMatchIndex = -1;
            searchStatus.setText("0 coincidencias");
            searchStatus.setStyle("-fx-text-fill: #f44336;");
            textArea.selectRange(0, 0);
        }
    }
    
    private void seleccionarMatchActual() {
        if (matchResults.isEmpty() || currentMatchIndex < 0 || currentMatchIndex >= matchResults.size()) {
            return;
        }
        
        MatchResult match = matchResults.get(currentMatchIndex);
        textArea.selectRange(match.startPos, match.endPos);
        textArea.requestFocus();
        
        Platform.runLater(() -> {
            try {
                String[] lineas = fullLogText.split("\n", -1);
                double scrollAmount = (double) match.lineNumber / lineas.length;
                textArea.setScrollTop(scrollAmount * textArea.getHeight());
            } catch (Exception e) {
                textArea.positionCaret(match.startPos);
            }
        });
        
        searchStatus.setText((currentMatchIndex + 1) + "/" + matchResults.size() + " coincidencias");
    }
    
    private void limpiarBusqueda() {
        matchResults.clear();
        currentMatchIndex = -1;
        searchField.clear();
        searchStatus.setText("0 coincidencias");
        searchStatus.setStyle("-fx-text-fill: gray;");
        textArea.selectRange(0, 0);
    }
}