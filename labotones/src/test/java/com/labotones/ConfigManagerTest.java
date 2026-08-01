package com.labotones;

import com.labotones.Class_help.ConfigManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    @Test
    void testHashingNoEsNull() {
        String password = "testPassword123";
        String hash = ConfigManager.hashPassword(password);
        assertNotNull(hash, "El hash no debería ser null");
        assertNotEquals("", hash, "El hash no debería estar vacío");
    }

    @Test
    void testHashingEsConsistente() {
        String password = "miPassword";
        String hash1 = ConfigManager.hashPassword(password);
        String hash2 = ConfigManager.hashPassword(password);
        assertEquals(hash1, hash2, "El mismo password debe producir el mismo hash");
    }

    @Test
    void testHashesDiferentesParaPasswordsDiferentes() {
        String hash1 = ConfigManager.hashPassword("password1");
        String hash2 = ConfigManager.hashPassword("password2");
        assertNotEquals(hash1, hash2, "Passwords diferentes deben tener hashes diferentes");
    }

    @Test
    void testHashSHA256Longitud() {
        // SHA-256 produce un hash de 64 caracteres hexadecimales
        String password = "testPassword";
        String hash = ConfigManager.hashPassword(password);
        assertEquals(64, hash.length(), "El hash SHA-256 debe tener 64 caracteres");
    }

    @Test
    void testPropertyGetDefault() {
        String valor = ConfigManager.getProperty("clave.inexistente", "valorPorDefecto");
        assertEquals("valorPorDefecto", valor, "Debería retornar el valor por defecto");
    }

    @Test
    void testRepositorioPath() {
        String ruta = "/path/compartido/test";
        ConfigManager.setRepositorioPath(ruta);
        String recuperado = ConfigManager.getRepositorioPath();
        assertEquals(ruta, recuperado, "La ruta debería guardarse y recuperarse");
    }

    @Test
    void testRepositorioPathVacio() {
        ConfigManager.setRepositorioPath("");
        String recuperado = ConfigManager.getRepositorioPath();
        assertEquals("", recuperado, "Debería permitir ruta vacía");
    }

    @Test
    void testRepositorioPathNull() {
        ConfigManager.setRepositorioPath(null);
        String recuperado = ConfigManager.getRepositorioPath();
        // Si pasamos null, debería remover la propiedad (getProperty retorna default)
        assertEquals("", recuperado, "Null debería resultar en vacío");
    }

    @Test
    void testReload() {
        String ruta1 = "/path/1";
        ConfigManager.setRepositorioPath(ruta1);
        
        // Reload debería mantener el valor si está guardado
        ConfigManager.reload();
        String recuperado = ConfigManager.getRepositorioPath();
        
        // Dependiendo de si config.properties está guardado en disco
        // esto puede variar en tests, pero no debería causar excepción
        assertNotNull(recuperado, "Reload no debería causar null");
    }
}
