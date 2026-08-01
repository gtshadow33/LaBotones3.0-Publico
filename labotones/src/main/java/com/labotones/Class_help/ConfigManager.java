package com.labotones.Class_help;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties props = new Properties();
    
    // ========== NUEVO: Lock para thread-safety ==========
    private static final Object lock = new Object();

    static { cargar(); }

    private static void cargar() {
        synchronized (lock) {
            File file = new File(CONFIG_FILE);
            if (!file.exists()) return;
            try (FileReader reader = new FileReader(file)) {
                props.load(reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void guardar() {
        synchronized (lock) {
            try {
                Path filePath = Paths.get(CONFIG_FILE);
                Path tmpPath = Paths.get(CONFIG_FILE + ".tmp");

                // 1) Escribimos primero en un fichero temporal aparte...
                try (FileWriter writer = new FileWriter(tmpPath.toFile())) {
                    props.store(writer, "LaBotones");
                }

                // 2) ...y solo al final lo movemos de forma atómica sobre el real.
                //    Si el proceso muere a mitad de la escritura del .tmp (corte de luz,
                //    kill -9, RDP que se cae), config.properties nunca queda a medio
                //    escribir: o se ve la versión anterior completa, o la nueva completa.
                Files.move(tmpPath, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void reload() {
        synchronized (lock) {
            props.clear();
            cargar();
        }
    }

    public static String getProperty(String key, String defaultValue) {
        synchronized (lock) {
            return props.getProperty(key, defaultValue);
        }
    }

    public static void setProperty(String key, String value) {
        synchronized (lock) {
            if (value == null) props.remove(key);
            else props.setProperty(key, value);
            guardar();
        }
    }

    // SOLO ruta del repositorio - NADA MAS
    public static String getRepositorioPath() {
        synchronized (lock) {
            return getProperty("repositorio.path", "");
        }
    }

    public static void setRepositorioPath(String path) {
        synchronized (lock) {
            setProperty("repositorio.path", path);
        }
    }

    // SOLO para hashear (no se guarda en local)
    public static String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) { return null; }
    }

    public static boolean isBloqueado() {
        synchronized (lock) {
            return Boolean.parseBoolean(getProperty("botones.bloqueado", "false"));
        }
    }

    public static void setBloqueado(boolean bloqueado) {
        synchronized (lock) {
            setProperty("botones.bloqueado", String.valueOf(bloqueado));
        }
    }

    public static boolean isSincronizarOmitido() {
        synchronized (lock) {
            return Boolean.parseBoolean(getProperty("sincronizar.omitido", "false"));
        }
    }

    public static void setSincronizarOmitido(boolean omitido) {
        synchronized (lock) {
            setProperty("sincronizar.omitido", String.valueOf(omitido));
        }
    }
}