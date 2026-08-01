package com.labotones.Class_help;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.*;
import javafx.util.Duration;

public class CountdownDialog {

    /** Segundos por defecto de la cuenta atrás (5 segundos) */
    public static final int SEGUNDOS_POR_DEFECTO = 5;

    /** Mantiene compatibilidad: cuenta atrás de 5 segundos  por defecto */
    public static void run(String titulo, String descripcion, Runnable onExecute) {
        run(titulo, descripcion, SEGUNDOS_POR_DEFECTO, onExecute);
    }

    /**
     * Muestra un diálogo de confirmación con cuenta atrás.
     * Si el usuario no responde, la acción se ejecuta automáticamente al llegar a 0.
     * El usuario puede pulsar "Ejecutar ahora" para saltarse la espera o "Cancelar" para abortar.
     *
     * @param titulo       título de la acción (informativo)
     * @param descripcion  descripción mostrada en el cuerpo del diálogo
     * @param segundos     duración de la cuenta atrás en segundos
     * @param onExecute    acción a ejecutar cuando se confirma o expira el tiempo
     */
    public static void run(String titulo, String descripcion, int segundos, Runnable onExecute) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar acción: " + titulo);
        alert.setContentText(descripcion);

        ButtonType ejecutarBtn = new ButtonType("Ejecutar ahora");
        ButtonType cancelarBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(ejecutarBtn, cancelarBtn);

        final int[] tiempo = {Math.max(segundos, 1)};
        final boolean[] ejecutado = {false};
        final Timeline[] timeline = new Timeline[1];

        alert.setHeaderText("Se ejecutará en " + formatearTiempo(tiempo[0]));

        timeline[0] = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            tiempo[0]--;
            if (tiempo[0] > 0) {
                alert.setHeaderText("Se ejecutará en " + formatearTiempo(tiempo[0]));
            } else {
                timeline[0].stop();
                if (!ejecutado[0]) {
                    ejecutado[0] = true;
                    alert.close();
                    onExecute.run();
                }
            }
        }));

        timeline[0].setCycleCount(Math.max(segundos, 1));
        timeline[0].play();

        alert.showAndWait().ifPresent(response -> {
            timeline[0].stop();
            if (response == ejecutarBtn && !ejecutado[0]) {
                ejecutado[0] = true;
                onExecute.run();
            }
        });
    }

    private static String formatearTiempo(int segundosRestantes) {
        if (segundosRestantes < 60) {
            return segundosRestantes + " segundos";
        }
        int minutos = segundosRestantes / 60;
        int segundos = segundosRestantes % 60;
        String textoMin = minutos + (minutos == 1 ? " minuto" : " minutos");
        if (segundos == 0) return textoMin;
        return textoMin + " y " + segundos + (segundos == 1 ? " segundo" : " segundos");
    }
}
