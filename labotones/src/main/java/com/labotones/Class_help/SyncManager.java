package com.labotones.Class_help;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncManager {

    private static final String REPO_FILE = "repo.password";
    private static final String[] CARPETAS_SYNC = { "bats", "icons" };

    /**
     * Evita que una sincronización automática y una manual (o dos
     * automáticas seguidas) corran a la vez sobre los mismos ficheros.
     * Es estático porque cada llamada crea su propia instancia de
     * SyncManager, pero el recurso en disco (botones.json, bats/, icons/)
     * es compartido por toda la app.
     */
    private static final AtomicBoolean SYNC_EN_CURSO = new AtomicBoolean(false);

    /**
     * Copia recursivamente todo el contenido de origen dentro de destino.
     * Si origen no existe, NO hace nada y devuelve false.
     */
    private boolean copiarCarpeta(Path origen, Path destino) {
        if (!Files.exists(origen)) {
  
            return false;
        }
        
        try {
            Files.createDirectories(destino);
            Files.walkFileTree(origen, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path target = destino.resolve(origen.relativize(dir));
                    Files.createDirectories(target);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path target = destino.resolve(origen.relativize(file));
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (IOException e) {
            System.err.println("❌ Error copiando carpeta " + origen + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Limpia recursivamente una carpeta
     */
    private void limpiarCarpeta(Path dir) {
        if (!Files.exists(dir)) return;
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("⚠️ No se pudo limpiar completamente: " + dir);
        }
    }

    public String getRepositorioPath() {
        return ConfigManager.getRepositorioPath();
    }

    public void setRepositorioPath(String path) {
        ConfigManager.setRepositorioPath(path);
    }

    public boolean repositorioTienePassword() {
        String repoPath = getRepositorioPath();
        if (repoPath == null || repoPath.isEmpty()) {
            return false;
        }
        File passwordFile = new File(repoPath, REPO_FILE);
        return passwordFile.exists() && passwordFile.length() > 0;
    }

    public boolean verificarPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        String repoPath = getRepositorioPath();
        if (repoPath == null || repoPath.isEmpty()) {
            return false;
        }

        File passwordFile = new File(repoPath, REPO_FILE);
        if (!passwordFile.exists()) {
            return false;
        }

        try {
            String stored = new String(Files.readAllBytes(passwordFile.toPath())).trim();
            String hashIntroducido = ConfigManager.hashPassword(password);

            if (!esHashValido(stored)) {
                boolean coincide = password.equals(stored);
                if (coincide) {
                    cambiarPassword(password);
          
                }
                return coincide;
            }

            return hashIntroducido != null && hashIntroducido.equals(stored);
        } catch (IOException e) {
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }

    private boolean esHashValido(String texto) {
        return texto != null && texto.matches("^[a-f0-9]{64}$");
    }

    public boolean cambiarPassword(String nuevaPassword) {
        String repoPath = getRepositorioPath();
        if (repoPath == null || repoPath.isEmpty()) {
            return false;
        }

        File repoDir = new File(repoPath);
        if (!repoDir.exists() || !repoDir.isDirectory()) {
            return false;
        }

        // Password vacía o null = borrar la contraseña del repositorio (no es un error).
        if (nuevaPassword == null || nuevaPassword.isEmpty()) {
            File passwordFile = new File(repoDir, REPO_FILE);
            if (!passwordFile.exists()) {
                return true; // ya no había contraseña, objetivo cumplido
            }
            try {
                Files.deleteIfExists(passwordFile.toPath());
                return true;
            } catch (IOException e) {
                System.err.println("Error borrando contraseña: " + e.getMessage());
                return false;
            }
        }

        String hash = ConfigManager.hashPassword(nuevaPassword);
        if (hash == null) {
            return false;
        }

        try {
            File passwordFile = new File(repoDir, REPO_FILE);
            Files.write(passwordFile.toPath(), hash.getBytes());
            return true;
        } catch (IOException e) {
            System.err.println("Error changing password: " + e.getMessage());
            return false;
        }
    }

    // ========================================================================
    // SINCRONIZACIÓN DESDE REPOSITORIO A LOCAL (CORREGIDA)
    // ========================================================================

    public void sincronizarDesdeRepositorio(Runnable onSuccess, Runnable onError) {
        sincronizarDesdeRepositorio(onSuccess, onError, null);
    }

    /**
     * @param onOcupado se invoca cuando ya hay otra sincronización en curso.
     *                   NO es un error real (los datos ya se están sincronizando
     *                   en la otra operación), así que no debe tratarse ni mostrarse
     *                   como tal. Si es null, se ignora silenciosamente.
     */
    public void sincronizarDesdeRepositorio(Runnable onSuccess, Runnable onError, Runnable onOcupado) {
        if (!SYNC_EN_CURSO.compareAndSet(false, true)) {
            if (onOcupado != null) onOcupado.run();
            return;
        }
        try {
            String repoPath = getRepositorioPath();
            if (repoPath == null || repoPath.isEmpty()) {
                if (onError != null) onError.run();
                return;
            }

            File repoDir = new File(repoPath);
            if (!repoDir.exists() || !repoDir.isDirectory()) {
                System.err.println("Repositorio no existe: " + repoPath);
                if (onError != null) onError.run();
                return;
            }

            Path localPath = Paths.get(System.getProperty("user.dir"));
            List<String> copiados = new ArrayList<>();
            boolean error = false;

            // ========== 1. COPIAR botones.json (el repo manda, sin merge) ==========
            // Antes aquí se hacía un "merge" para no perder botones creados
            // solo en local. Se quitó: con más de un técnico usando el mismo
            // repositorio, ese merge no sabía distinguir "esto lo creé yo y
            // aún no lo he subido" de "esto lo borró otra persona (o yo mismo
            // desde otro momento) y mi caché local todavía lo tiene" — y en
            // el segundo caso, el botón borrado volvía a aparecer solo. Ahora
            // la descarga es una copia directa: el repo siempre manda.
            File repoConfigFile = new File(repoDir, "botones.json");
            if (repoConfigFile.exists()) {
                try {
                    File localConfigFile = new File("botones.json");
                    Files.copy(repoConfigFile.toPath(), localConfigFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                    new ButtonService().reload();
                    copiados.add("botones.json");
                } catch (IOException e) {
                    System.err.println("❌ Error copiando botones.json: " + e.getMessage());
                    error = true;
                }
            } else {
  
            }

            // ========== 2. COPIAR CARPETAS (aditivo: no se borra nada en local) ==========
            for (String carpeta : CARPETAS_SYNC) {
                Path origen = Paths.get(repoPath).resolve(carpeta);
                Path destino = localPath.resolve(carpeta);
                
                if (Files.exists(origen)) {
                    try {
                        // Copia aditiva: los .bat/iconos del repo se copian (y sobreescriben
                        // si hay uno con el mismo nombre), pero NO se borra nada que solo
                        // exista en local, para no perder scripts/iconos creados offline.
                        if (copiarCarpeta(origen, destino)) {
                            copiados.add(carpeta);
                   
                        } else {
                            System.err.println("❌ Error copiando " + carpeta);
                            error = true;
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error copiando " + carpeta + ": " + e.getMessage());
                        error = true;
                    }
                } else {
                 }
            }

            if (error) {
                System.err.println("❌ Sincronización completada con errores. Archivos copiados: " + copiados);
                if (onError != null) onError.run();
            } else {
             
                if (onSuccess != null) onSuccess.run();
            }

        } catch (Exception e) {
            System.err.println("❌ Error en sincronización desde repositorio: " + e.getMessage());
            e.printStackTrace();
            if (onError != null) onError.run();
        } finally {
            SYNC_EN_CURSO.set(false);
        }
    }

    // ========================================================================
    // SINCRONIZACIÓN DESDE LOCAL A REPOSITORIO (CORREGIDA)
    // ========================================================================

    public void sincronizarAlRepositorio(Runnable onSuccess, Runnable onError) {
        sincronizarAlRepositorio(onSuccess, onError, null);
    }

    /**
     * @param onOcupado se invoca cuando ya hay otra sincronización en curso.
     *                   NO es un error real, así que no debe tratarse ni mostrarse
     *                   como tal. Si es null, se ignora silenciosamente.
     */
    public void sincronizarAlRepositorio(Runnable onSuccess, Runnable onError, Runnable onOcupado) {
        if (!SYNC_EN_CURSO.compareAndSet(false, true)) {
            if (onOcupado != null) onOcupado.run();
            return;
        }
        try {
            String repoPath = getRepositorioPath();
            if (repoPath == null || repoPath.isEmpty()) {
                if (onError != null) onError.run();
                return;
            }

            File repoDir = new File(repoPath);
            if (!repoDir.exists()) {
                repoDir.mkdirs();
            }

            Path localPath = Paths.get(System.getProperty("user.dir"));
            List<String> copiados = new ArrayList<>();
            boolean error = false;

            // ========== 1. COPIAR botones.json ==========
            File localConfigFile = new File("botones.json");
            if (localConfigFile.exists()) {
                try {
                    Path repoConfigPath = Paths.get(repoPath, "botones.json");
                    Files.copy(localConfigFile.toPath(), repoConfigPath, 
                        StandardCopyOption.REPLACE_EXISTING);
                    copiados.add("botones.json");
                 
                } catch (IOException e) {
                    System.err.println("❌ Error copiando botones.json: " + e.getMessage());
                    error = true;
                }
            } else {
      
            }

            // ========== 2. COPIAR CARPETAS ==========
            for (String carpeta : CARPETAS_SYNC) {
                Path origen = localPath.resolve(carpeta);
                Path destino = Paths.get(repoPath).resolve(carpeta);
                
                if (Files.exists(origen)) {
                    try {
                        // Limpiar destino si existe
                        if (Files.exists(destino)) {
                            limpiarCarpeta(destino);
                        }
                        // Copiar
                        if (copiarCarpeta(origen, destino)) {
                            copiados.add(carpeta);
                 
                        } else {
                            System.err.println("❌ Error copiando " + carpeta + " al repositorio");
                            error = true;
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error copiando " + carpeta + ": " + e.getMessage());
                        error = true;
                    }
                } else {
    
                }
            }

            if (error) {
                System.err.println("❌ Sincronización al repositorio completada con errores. Copiados: " + copiados);
                if (onError != null) onError.run();
            } else {
    
                if (onSuccess != null) onSuccess.run();
            }

        } catch (Exception e) {
            System.err.println("❌ Error en sincronización al repositorio: " + e.getMessage());
            e.printStackTrace();
            if (onError != null) onError.run();
        } finally {
            SYNC_EN_CURSO.set(false);
        }
    }

    /**
     * Método de compatibilidad para código existente
     */
    public boolean repositorioListo() {
        String repoPath = getRepositorioPath();
        if (repoPath == null || repoPath.isEmpty()) {
            return false;
        }
        File repoDir = new File(repoPath);
        return repoDir.exists() && repoDir.isDirectory();
    }

    /**
     * Método de compatibilidad para código existente
     */
    public String obtenerEstadoRepositorio() {
        String repoPath = getRepositorioPath();
        if (repoPath == null || repoPath.isEmpty()) {
            return "❌ No configurado";
        }
        File repoDir = new File(repoPath);
        if (!repoDir.exists() || !repoDir.isDirectory()) {
            return "❌ Ruta inválida: " + repoPath;
        }
        if (repositorioTienePassword()) {
            return "✅ Configurado (con contraseña)";
        } else {
            return "⚠️ Configurado (sin contraseña)";
        }
    }
}