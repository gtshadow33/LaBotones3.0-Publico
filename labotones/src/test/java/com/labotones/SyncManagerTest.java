package com.labotones;

import com.labotones.Class_help.ConfigManager;
import com.labotones.Class_help.SyncManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SyncManagerTest {

    private SyncManager syncManager;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        syncManager = new SyncManager();
    }

    @Test
    void testVerificarPasswordSinRepositorio() {
        // Si no hay repositorio configurado, debe retornar false
        syncManager.setRepositorioPath("");
        boolean resultado = syncManager.verificarPassword("cualquierpassword");
        assertFalse(resultado);
    }

    @Test
    void testVerificarPasswordRepositorioNoExiste() {
        // Si la ruta no existe, debe retornar false
        syncManager.setRepositorioPath("/ruta/inexistente/12345");
        boolean resultado = syncManager.verificarPassword("password");
        assertFalse(resultado);
    }

    @Test
    void testRepositorioTienePasswordCuandoNoHay() {
        // Si no hay archivo .repo_password, debe retornar false
        syncManager.setRepositorioPath(tempDir.toString());
        boolean tiene = syncManager.repositorioTienePassword();
        assertFalse(tiene);
    }

    @Test
    void testCambiarPasswordYVerificar() throws IOException {
        // Cambiar contraseña y luego verificarla
        syncManager.setRepositorioPath(tempDir.toString());
        
        String passwordNueva = "miContraseña123";
        boolean cambio = syncManager.cambiarPassword(passwordNueva);
        assertTrue(cambio, "La contraseña debería haberse guardado");

        // Ahora verificar que se puede logear con esa contraseña
        boolean verificacion = syncManager.verificarPassword(passwordNueva);
        assertTrue(verificacion, "La contraseña debería ser correcta");

        // Y debe fallar con contraseña incorrecta
        boolean verificacionMala = syncManager.verificarPassword("contraseñaIncorrecta");
        assertFalse(verificacionMala, "Contraseña incorrecta debería fallar");
    }

    @Test
    void testRepositorioTienePasswordDespuesDeCambiar() throws IOException {
        syncManager.setRepositorioPath(tempDir.toString());
        syncManager.cambiarPassword("password123");
        
        boolean tiene = syncManager.repositorioTienePassword();
        assertTrue(tiene, "Debería tener contraseña después de cambiarla");
    }

    @Test
    void testHashingEsConsistente() {
        // El mismo password debe producir el mismo hash
        String password = "testPassword";
        String hash1 = ConfigManager.hashPassword(password);
        String hash2 = ConfigManager.hashPassword(password);
        assertEquals(hash1, hash2, "El hash debe ser determinista");
    }

    @Test
    void testPasswordVaciaNoSeGuarda() throws IOException {
        syncManager.setRepositorioPath(tempDir.toString());
        syncManager.cambiarPassword("password123");
        assertTrue(syncManager.repositorioTienePassword());
        
        // Borrar password (pasar null o vacío)
        boolean borrado = syncManager.cambiarPassword("");
        assertTrue(borrado);
        
        assertFalse(syncManager.repositorioTienePassword(), 
                    "No debería haber contraseña después de limpiarla");
    }

    @Test
    void testRepositorioPathSeGuardaEnConfig() {
        String ruta = "/path/compartido";
        syncManager.setRepositorioPath(ruta);
        
        // Crear nuevo manager y verificar que recupera la ruta
        SyncManager manager2 = new SyncManager();
        assertEquals(ruta, manager2.getRepositorioPath(), 
                    "La ruta debería persistir en config");
    }
}
