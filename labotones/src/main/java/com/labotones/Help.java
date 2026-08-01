package com.labotones;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Help {

    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String FILE = "logs.txt";

    // Formato esperado de cada línea: [yyyy-MM-dd HH:mm:ss] mensaje
    private static final Pattern LOG_LINE_PATTERN =
            Pattern.compile("^\\[(\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2}:\\d{2})\\]\\s?(.*)$");

    // ========================
    // ESCRIBIR LOG
    // ========================
    public static void log(String mensaje) {
        String timestamp = LocalDateTime.now().format(formatter);
        // Usamos System.lineSeparator() para asegurar el salto de línea correcto
        String line = "[" + timestamp + "] " + mensaje + System.lineSeparator();

        // El 'true' activa el modo append (añadir al final)
        try (FileWriter fw = new FileWriter(FILE, true)) {
            fw.write(line);
            fw.flush(); // Fuerza la escritura inmediata al disco
        } catch (IOException e) {
            System.err.println("Error al escribir log: " + e.getMessage());
        }
    }

    // ========================
    // LEER LOGS
    // ========================
    public static String leerLogs() {
        try {
            File f = new File(FILE);
            if (!f.exists()) {
                return "No hay registros disponibles todavía.";
            }
            return Files.readString(Paths.get(FILE));
        } catch (IOException e) {
            return "Error al leer el archivo de logs: " + e.getMessage();
        }
    }

    // ========================
    // EXPORTAR LOGS A CSV
    // ========================
    /**
     * Exporta el contenido de logs.txt a un archivo CSV con columnas
     * Fecha, Hora, Mensaje — para poder ordenar/filtrar por fecha rápidamente
     * en Excel/Sheets al reportar una incidencia.
     *
     * @param destino  archivo .csv de salida
     * @param filtro   si no es null, solo se exportan las líneas de esa fecha
     * @return número de líneas exportadas
     */
    public static int exportarLogsCSV(File destino, LocalDate filtro) throws IOException {
        File origen = new File(FILE);
        if (!origen.exists()) {
            throw new IOException("No hay registros disponibles todavía.");
        }

        int exportadas = 0;
        try (java.io.OutputStreamWriter fw = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(destino), java.nio.charset.StandardCharsets.UTF_8)) {
            // BOM UTF-8 al principio para que Excel abra bien los acentos/emojis
            fw.write('\uFEFF');
            fw.write("Fecha;Hora;Mensaje" + System.lineSeparator());

            List<String> lineas = Files.readAllLines(origen.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            for (String linea : lineas) {
                if (linea == null || linea.isBlank()) continue;

                Matcher m = LOG_LINE_PATTERN.matcher(linea);
                String fecha, hora, mensaje;

                if (m.matches()) {
                    fecha = m.group(1);
                    hora = m.group(2);
                    mensaje = m.group(3);
                } else {
                    // Línea que no sigue el formato esperado (p.ej. multilínea) -> se exporta tal cual
                    fecha = "";
                    hora = "";
                    mensaje = linea;
                }

                if (filtro != null) {
                    if (fecha.isEmpty() || !fecha.equals(filtro.toString())) {
                        continue; // no coincide con la fecha filtrada
                    }
                }

                fw.write(fecha + ";" + hora + ";" + escaparCSV(mensaje) + System.lineSeparator());
                exportadas++;
            }
        }

        return exportadas;
    }

    /**
     * Escapa un campo para CSV con separador ';' — envuelve en comillas si
     * contiene el separador, comillas o saltos de línea, y duplica las comillas internas.
     */
    private static String escaparCSV(String campo) {
        if (campo == null) return "";
        if (campo.contains(";") || campo.contains("\"") || campo.contains("\n") || campo.contains("\r")) {
            return "\"" + campo.replace("\"", "\"\"") + "\"";
        }
        return campo;
    }
}