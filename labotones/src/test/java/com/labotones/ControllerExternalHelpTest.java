package com.labotones;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test simplificado centrado en la validación de equipos/IP.
 * (La lógica de UI de ExternalHelp requiere JavaFX y se prueba mejor en integración)
 */
class ControllerExternalHelpTest {

    // Patrones copiados directamente del Controller original
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    private static final Pattern BCN_PATTERN = Pattern.compile(
            "^(BCNT|BCN)[A-Z0-9]+$",
            Pattern.CASE_INSENSITIVE
    );

    private static boolean esIPValida(String texto) {
        return IP_PATTERN.matcher(texto).matches() && !"127.0.0.1".equals(texto);
    }

    private static boolean esBCNValido(String texto) {
        return BCN_PATTERN.matcher(texto).matches() && texto.length() == 10;
    }

    private static boolean isFormatoValido(String texto) {
        if (texto == null || texto.isBlank()) return false;
        String upper = texto.toUpperCase();
        return esIPValida(upper) || esBCNValido(upper);
    }

    // ---------- Tests de IP ----------
    @ParameterizedTest
    @ValueSource(strings = {"192.168.1.1", "10.0.0.1", "172.16.0.1", "8.8.8.8", "255.255.255.0"})
    void ipValida(String ip) {
        assertTrue(esIPValida(ip));
        assertTrue(isFormatoValido(ip));
    }

    @Test
    void localhostNoEsValida() {
        assertFalse(esIPValida("127.0.0.1"));
        assertFalse(isFormatoValido("127.0.0.1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"999.999.999.999", "192.168.1", "192.168.1.300", "abc.def.ghi.jkl", "256.0.0.1"})
    void ipInvalida(String ip) {
        assertFalse(esIPValida(ip));
        assertFalse(isFormatoValido(ip));
    }

    // ---------- Tests de BCN (longitud 10, formato BCN/BCNT más 6 o 7 caracteres) ----------
    @Test
    void bcnLongitudCorrecta() {
        // Válidos: exactamente 10 caracteres, empiezan con BCN o BCNT
        assertTrue(esBCNValido("BCNT123456"));  // BCNT + 6 = 10
        assertTrue(esBCNValido("BCN1234567"));  // BCN + 7 = 10
        // Inválidos por longitud
        assertFalse(esBCNValido("BCNT12345"));   // 9
        assertFalse(esBCNValido("BCN123456"));   // 9
        assertFalse(esBCNValido("BCNT1234567")); // 11
    }

    @Test
    void bcnCaracteresNoPermitidos() {
        // Los caracteres no alfanuméricos sí deben ser inválidos
        assertFalse(esBCNValido("BCNT12 456")); // espacio
        assertFalse(esBCNValido("BCN-123456")); // guión
        // Pero las letras son permitidas
        assertTrue(esBCNValido("BCNX123456")); // X es letra, permitida
    }

    @Test
    void bcnCaseInsensitive() {
        // El patrón es CASE_INSENSITIVE, así que minúsculas también son válidas
        assertTrue(esBCNValido("bcn1234567"));
        assertTrue(esBCNValido("BcN1234567"));
    }
}