package com.labotones;

import com.labotones.Class_help.ButtonService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ButtonServiceTest {
    @Test
    void testServiceCanBeCreated() {
        ButtonService service = new ButtonService();
        assertNotNull(service);
    }

    @Test
    void testGetAllDoesNotThrow() {
        ButtonService service = new ButtonService();
        assertDoesNotThrow(() -> service.getAll());
    }
}