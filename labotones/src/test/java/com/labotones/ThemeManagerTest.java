
package com.labotones;

import com.labotones.Class_help.ThemeManager;
import com.labotones.Class_help.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class ThemeManagerTest {

    @BeforeEach
    void setUp() {
        // Configurar estado inicial conocido para cada prueba
        ThemeManager.setTheme(Theme.LIGHT);
    }

    @Test
    void testGetCurrentTheme() {
        ThemeManager.setTheme(Theme.LIGHT);
        assertEquals(Theme.LIGHT, ThemeManager.getCurrentTheme());

        ThemeManager.setTheme(Theme.DARK);
        assertEquals(Theme.DARK, ThemeManager.getCurrentTheme());

        ThemeManager.setTheme(Theme.DRACULA);
        assertEquals(Theme.DRACULA, ThemeManager.getCurrentTheme());
    }

    @Test
    void testSetTheme() {
        ThemeManager.setTheme(Theme.LIGHT);
        assertEquals(Theme.LIGHT, ThemeManager.getCurrentTheme());

        ThemeManager.setTheme(Theme.DARK);
        assertEquals(Theme.DARK, ThemeManager.getCurrentTheme());

        ThemeManager.setTheme(Theme.DRACULA);
        assertEquals(Theme.DRACULA, ThemeManager.getCurrentTheme());
    }

    @Test
    void testSetThemeNull() {
        // Guardar tema actual
        Theme original = ThemeManager.getCurrentTheme();

        // Intentar establecer null
        ThemeManager.setTheme(null);

        // Verificar que no cambió
        assertEquals(original, ThemeManager.getCurrentTheme());
    }

    /**
     * Ciclo completo de temas:
     * LIGHT -> DARK -> DRACULA -> MATRIX -> NORD
     * -> SUNSET -> AZUL -> ALPHA -> LIGHT
     */
    @Test
    void testToggle() {
        ThemeManager.setTheme(Theme.LIGHT);

        ThemeManager.toggle();
        assertEquals(Theme.DARK, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.DRACULA, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.MATRIX, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.NORD, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.SUNSET, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.AZUL, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.ALPHA, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.LIGHT, ThemeManager.getCurrentTheme());
    }

    @Test
    void testToggleAndPersistence() {
        // Guardar estado original
        Theme original = ThemeManager.getCurrentTheme();

        // Probar el ciclo completo desde LIGHT
        ThemeManager.setTheme(Theme.LIGHT);

        ThemeManager.toggle();
        assertEquals(Theme.DARK, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.DRACULA, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.MATRIX, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.NORD, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.SUNSET, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.AZUL, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.ALPHA, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.LIGHT, ThemeManager.getCurrentTheme());

        // Restaurar estado original
        ThemeManager.setTheme(original);
    }

    @Test
    void testThemeEnumValues() {
        // Verificar que existen los 8 temas
        Theme[] themes = Theme.values();

        assertEquals(8, themes.length);

        assertTrue(containsTheme(themes, Theme.LIGHT));
        assertTrue(containsTheme(themes, Theme.DARK));
        assertTrue(containsTheme(themes, Theme.DRACULA));
        assertTrue(containsTheme(themes, Theme.MATRIX));
        assertTrue(containsTheme(themes, Theme.NORD));
        assertTrue(containsTheme(themes, Theme.SUNSET));
        assertTrue(containsTheme(themes, Theme.AZUL));
        assertTrue(containsTheme(themes, Theme.ALPHA));
    }

    @Test
    void testThemeEnumValueOf() {
        assertEquals(Theme.LIGHT, Theme.valueOf("LIGHT"));
        assertEquals(Theme.DARK, Theme.valueOf("DARK"));
        assertEquals(Theme.DRACULA, Theme.valueOf("DRACULA"));
        assertEquals(Theme.MATRIX, Theme.valueOf("MATRIX"));
        assertEquals(Theme.NORD, Theme.valueOf("NORD"));
        assertEquals(Theme.SUNSET, Theme.valueOf("SUNSET"));
        assertEquals(Theme.AZUL, Theme.valueOf("AZUL"));
        assertEquals(Theme.ALPHA, Theme.valueOf("ALPHA"));
    }

    @Test
    void testFullCycle() {
        // Probar ciclo completo de los 8 temas
        ThemeManager.setTheme(Theme.LIGHT);

        assertEquals(Theme.LIGHT, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.DARK, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.DRACULA, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.MATRIX, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.NORD, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.SUNSET, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.AZUL, ThemeManager.getCurrentTheme());

        ThemeManager.toggle();
        assertEquals(Theme.ALPHA, ThemeManager.getCurrentTheme());

        // Volver al primer tema
        ThemeManager.toggle();
        assertEquals(Theme.LIGHT, ThemeManager.getCurrentTheme());
    }

    /**
     * Método auxiliar para verificar si un tema existe en el array.
     */
    private boolean containsTheme(Theme[] themes, Theme theme) {
        for (Theme t : themes) {
            if (t == theme) {
                return true;
            }
        }

        return false;
    }
}

