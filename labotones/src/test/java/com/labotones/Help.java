package com.labotones;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HelpTest {
    @Test
    void testLogDoesNotThrow() {
        // Solo comprobamos que escribir un log no lanza excepción
        assertDoesNotThrow(() -> Help.log("Mensaje de prueba"));
    }

    @Test
    void testLeerLogsDoesNotThrow() {
        // Leer logs puede devolver cualquier cosa, pero no debe lanzar excepción
        assertDoesNotThrow(() -> Help.leerLogs());
    }
}