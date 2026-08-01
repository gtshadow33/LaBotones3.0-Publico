package com.labotones.logic;

import java.io.File;
import java.io.IOException;

public class Logic {

    public static void ejecutar(String nombreBat) {
        ejecutar(nombreBat, null);
    }

    public static void ejecutar(String nombreBat, String argumento) {

        try {

            File bat = new File("bats", nombreBat);

            ProcessBuilder pb = (argumento == null || argumento.isBlank())
                    ? new ProcessBuilder("cmd", "/c",  bat.getAbsolutePath())
                    : new ProcessBuilder("cmd", "/c",  bat.getAbsolutePath(), argumento);

            pb.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}