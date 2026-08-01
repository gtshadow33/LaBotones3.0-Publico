package com.labotones;

import com.labotones.Class_help.Button_prop;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Button_propTest {

    @Test
    void testGettersAndSetters() {
        Button_prop btn = new Button_prop();
        btn.setID(10);
        btn.setName("Test");
        btn.setDescription("Desc");
        btn.setLocateBat("script.bat");
        btn.setIcono("icon.png");
        btn.setArgumento(true);

        assertEquals(10, btn.getID());
        assertEquals("Test", btn.getName());
        assertEquals("Desc", btn.getDescription());
        assertEquals("script.bat", btn.getLocateBat());
        assertEquals("icon.png", btn.getIcono());
        assertTrue(btn.isArgumento());
    }

    @Test
    void testIsValid() {
        Button_prop valid = new Button_prop();
        valid.setName("Name");
        valid.setLocateBat("bat.bat");
        assertTrue(valid.isValid());

        Button_prop noName = new Button_prop();
        noName.setLocateBat("bat.bat");
        assertFalse(noName.isValid());

        Button_prop noBat = new Button_prop();
        noBat.setName("Name");
        assertFalse(noBat.isValid());
    }

    @Test
    void testCopy() {
        Button_prop original = new Button_prop();
        original.setID(1);
        original.setName("Original");
        original.setDescription("Desc");
        original.setLocateBat("run.bat");
        original.setIcono("icon.png");
        original.setArgumento(true);

        Button_prop copia = original.copy();
        assertEquals(original.getID(), copia.getID());
        assertEquals(original.getName(), copia.getName());
        assertEquals(original.getDescription(), copia.getDescription());
        assertEquals(original.getLocateBat(), copia.getLocateBat());
        assertEquals(original.getIcono(), copia.getIcono());
        assertEquals(original.isArgumento(), copia.isArgumento());

        copia.setName("Modificado");
        assertEquals("Original", original.getName());
    }

    @Test
    void testConstructorWithNulls() {
        Button_prop btn = new Button_prop(1, null, null, null, null, false, false, null, false);
        assertEquals("", btn.getName());
        assertEquals("", btn.getDescription());
        assertEquals("", btn.getLocateBat());
        assertEquals("", btn.getIcono());
        assertFalse(btn.isArgumento());
    }
}