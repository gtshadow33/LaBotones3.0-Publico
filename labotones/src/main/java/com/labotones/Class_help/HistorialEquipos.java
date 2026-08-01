package com.labotones.Class_help;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.File;
import java.util.*;

public class HistorialEquipos {
    
    private static final File FILE = new File("historial.json");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final int MAX_HISTORIAL = 3;
    
    private static List<String> historial = new ArrayList<>();
    private static boolean cargado = false;
    
    // Cargar historial desde archivo
    private static void cargar() {
        if (cargado) return;
        
        if (FILE.exists() && FILE.length() > 0) {
            try {
                historial = MAPPER.readValue(FILE, new TypeReference<>() {});
            } catch (Exception e) {
                System.err.println("Error cargando historial: " + e.getMessage());
                historial = new ArrayList<>();
            }
        }
        cargado = true;
    }
    
    // Guardar historial a archivo
    private static void guardar() {
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(FILE, historial);
        } catch (Exception e) {
            System.err.println("Error guardando historial: " + e.getMessage());
        }
    }
    
    // Añadir equipo al historial (sin duplicados, el último al principio)
    public static void añadir(String equipo) {
        if (equipo == null || equipo.isBlank()) return;
        
        cargar();
        
        // Eliminar si ya existe
        historial.removeIf(e -> e.equalsIgnoreCase(equipo));
        
        // Añadir al principio
        historial.add(0, equipo);
        
        // Limitar tamaño
        while (historial.size() > MAX_HISTORIAL) {
            historial.remove(historial.size() - 1);
        }
        
        guardar();
    }
    
    // Obtener todos los equipos del historial
    public static List<String> getHistorial() {
        cargar();
        return Collections.unmodifiableList(historial);
    }

    // Eliminar un equipo concreto del historial
    public static void eliminar(String equipo) {
        if (equipo == null || equipo.isBlank()) return;
        cargar();
        historial.removeIf(e -> e.equalsIgnoreCase(equipo));
        guardar();
    }

    // Vaciar el historial por completo (y el archivo en disco)
    public static void limpiar() {
        cargar();
        historial.clear();
        guardar();
    }
}