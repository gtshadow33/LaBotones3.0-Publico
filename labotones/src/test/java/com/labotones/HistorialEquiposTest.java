package com.labotones;

import com.labotones.Class_help.HistorialEquipos;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HistorialEquiposTest {

    @BeforeEach
    void setUp() {
        HistorialEquipos.limpiar(); // Limpia el archivo real
    }

    @Test
    void testAñadirYGetHistorial() {
        HistorialEquipos.añadir("BCNT1234");
        HistorialEquipos.añadir("BCN5678");
        List<String> historial = HistorialEquipos.getHistorial();
        assertEquals(2, historial.size());
        assertEquals("BCN5678", historial.get(0));
        assertEquals("BCNT1234", historial.get(1));
    }

    @Test
    void testNoDuplicados() {
        HistorialEquipos.añadir("BCNT001");
        HistorialEquipos.añadir("BCNT001");
        List<String> historial = HistorialEquipos.getHistorial();
        assertEquals(1, historial.size());
        assertEquals("BCNT001", historial.get(0));
    }

    @Test
    void testMaximoHistorial() {
        for (int i = 1; i <= 10; i++) {
            HistorialEquipos.añadir("PC" + i);
        }
        List<String> historial = HistorialEquipos.getHistorial();
        assertEquals(3, historial.size());
        assertEquals("PC10", historial.get(0));
        assertEquals("PC9", historial.get(1));
        assertEquals("PC8", historial.get(2));
    }

    @Test
    void testEliminar() {
        HistorialEquipos.añadir("PC1");
        HistorialEquipos.añadir("PC2");
        HistorialEquipos.eliminar("PC1");
        List<String> historial = HistorialEquipos.getHistorial();
        assertEquals(1, historial.size());
        assertEquals("PC2", historial.get(0));
    }

    @Test
    void testLimpiar() {
        HistorialEquipos.añadir("A");
        HistorialEquipos.añadir("B");
        HistorialEquipos.limpiar();
        assertTrue(HistorialEquipos.getHistorial().isEmpty());
    }
}