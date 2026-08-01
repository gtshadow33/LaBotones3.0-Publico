package com.labotones.Class_help;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.image.Image;

/**
 * Caché de iconos en memoria, compartida por toda la app (grid principal,
 * mini ventanas, preview del CRUD).
 *
 * Antes se cargaba cada icono con {@code new Image(file.toURI().toString())},
 * que usa la caché interna de JavaFX indexada solo por URL: si el fichero
 * cambiaba en disco manteniendo el mismo nombre, se seguía viendo la versión
 * vieja hasta reiniciar la app. Quitar esa caché a secas (leer siempre del
 * disco) soluciona lo anterior pero introduce un problema nuevo: el grid se
 * repinta entero en cada tecla que escribes en el buscador, así que sin
 * ninguna caché se releerían todos los iconos del disco en cada pulsación,
 * notándose especialmente con muchos botones.
 *
 * Esta caché resuelve las dos cosas a la vez: se indexa por ruta absoluta +
 * fecha de última modificación del fichero. Si el fichero no ha cambiado,
 * se devuelve la imagen ya cargada en memoria (rápido). Si el fichero se ha
 * modificado (o es la primera vez que se pide), se relee del disco y se
 * actualiza la entrada.
 */
public final class IconCache {

    private IconCache() {}

    private record Entrada(long lastModified, Image image) {}

    private static final Map<String, Entrada> CACHE = new ConcurrentHashMap<>();

    /** Icono a tamaño "natural" del fichero (sin redimensionar). */
    public static Image obtener(File file) {
        return obtener(file, 0, 0);
    }

    /**
     * Icono redimensionado a anchoXalto (0 = tamaño original), manteniendo
     * relación de aspecto y suavizado, igual que hacían las llamadas antiguas
     * a new Image(url, w, h, true, true).
     */
    public static Image obtener(File file, double ancho, double alto) {
        if (file == null || !file.exists()) return null;

        String clave = file.getAbsolutePath() + "|" + (long) ancho + "x" + (long) alto;
        long modificado = file.lastModified();

        Entrada actual = CACHE.get(clave);
        if (actual != null && actual.lastModified() == modificado) {
            return actual.image();
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            Image img = (ancho > 0 || alto > 0)
                    ? new Image(fis, ancho, alto, true, true)
                    : new Image(fis);
            CACHE.put(clave, new Entrada(modificado, img));
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    /** Por si alguna vez hace falta forzar una relectura total (p.ej. tras un sync grande). */
    public static void limpiar() {
        CACHE.clear();
    }
}