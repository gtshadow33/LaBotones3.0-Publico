package com.labotones.Class_help;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Servicio de acceso a los botones (CRUD sobre botones.json).
 *
 * Thread-safety: el estado (caché en memoria) es {@code static}, compartido
 * por todas las instancias de {@code ButtonService}. Esto es intencionado:
 * hay varias partes de la app (Controller, ButtonCRUDController) que crean
 * su propia instancia, y todas deben ver el mismo estado sin tener que
 * llamar a {@link #reload()} manualmente para sincronizarse entre ellas.
 *
 * La caché se carga UNA VEZ, de forma inmediata (eager), la primera vez que
 * se toca la clase — la garantiza segura entre hilos la propia inicialización
 * de campos estáticos de Java (JLS 12.4.2), sin necesidad de ningún flag
 * "loaded" ni de lógica de carga perezosa.
 *
 * A partir de ahí, todo acceso a la caché (lecturas y escrituras por igual)
 * se protege con un único {@link ReentrantLock}. No se usa un
 * {@link java.util.concurrent.locks.ReentrantReadWriteLock}: los botones son
 * una lista pequeña que se lee en microsegundos desde memoria, así que la
 * ventaja teórica de permitir lecturas en paralelo no compensa la complejidad
 * extra (upgrade de read-lock a write-lock, HoldCounters por hilo, etc.). Un
 * lock exclusivo simple es más fácil de razonar y de mantener, con el mismo
 * resultado práctico aquí.
 */
public class ButtonService {

    public enum Result { OK, INVALID, NOT_FOUND, IO_ERROR }

    private static final File       FILE   = new File("botones.json");
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .build();

    private static final ReentrantLock LOCK = new ReentrantLock();

    /** Caché en memoria, compartida por todas las instancias. Acceso SIEMPRE bajo el lock. */
    private static final List<Button_prop> cache = new ArrayList<>(cargarDesdeDisco());

    private static List<Button_prop> cargarDesdeDisco() {
        try {
            if (FILE.exists() && FILE.length() > 0) {
                try (FileReader reader = new FileReader(FILE)) {
                    return MAPPER.readValue(reader, new TypeReference<List<Button_prop>>() {});
                }
            }
        } catch (Exception e) {
            System.err.println("[ButtonService] Error al cargar: " + e.getMessage());
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<Button_prop> getAll() {
        LOCK.lock();
        try {
            // Copia defensiva: el caller no puede modificar la caché interna,
            // y no se le expone la lista real mientras otro hilo podría mutarla.
            return List.copyOf(cache);
        } finally {
            LOCK.unlock();
        }
    }

    public Result add(Button_prop nuevo) {
        if (nuevo == null || !nuevo.isValid()) return Result.INVALID;
        LOCK.lock();
        try {
            int nextId = cache.stream().mapToInt(Button_prop::getID).max().orElse(0) + 1;
            nuevo.setID(nextId);
            cache.add(nuevo);
            return persistSinLock();
        } finally {
            LOCK.unlock();
        }
    }

    public Result update(Button_prop actualizado) {
        if (actualizado == null || !actualizado.isValid()) return Result.INVALID;
        LOCK.lock();
        try {
            for (int i = 0; i < cache.size(); i++) {
                if (cache.get(i).getID() == actualizado.getID()) {
                    cache.set(i, actualizado);
                    return persistSinLock();
                }
            }
            return Result.NOT_FOUND;
        } finally {
            LOCK.unlock();
        }
    }

    public Result delete(int id) {
        LOCK.lock();
        try {
            boolean removed = cache.removeIf(b -> b.getID() == id);
            if (!removed) return Result.NOT_FOUND;
            return persistSinLock();
        } finally {
            LOCK.unlock();
        }
    }

    /** Fuerza una relectura desde disco (p.ej. tras sincronizar con el repositorio). */
    public void reload() {
        LOCK.lock();
        try {
            cache.clear();
            cache.addAll(cargarDesdeDisco());
        } finally {
            LOCK.unlock();
        }
    }

    // ========================================================================
    // Métodos internos. IMPORTANTE: solo se llaman con LOCK ya adquirido por
    // el método público correspondiente — de ahí el sufijo "SinLock".
    // ========================================================================

    // ========== ESCRITURA ATÓMICA ==========
    private Result persistSinLock() {
        try {
            Path filePath = Paths.get(FILE.getAbsolutePath());
            Path tmpPath = Paths.get(FILE.getAbsolutePath() + ".tmp");

            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(cache);
            try (FileWriter writer = new FileWriter(tmpPath.toFile())) {
                writer.write(json);
            }

            Files.move(tmpPath, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            return Result.OK;
        } catch (Exception e) {
            System.err.println("[ButtonService] Error al guardar: " + e.getMessage());
            e.printStackTrace();
            return Result.IO_ERROR;
        }
    }
}