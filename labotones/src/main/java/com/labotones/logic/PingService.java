package com.labotones.logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;



import javafx.application.Platform;

public class PingService {

    public enum Estado {
        COMPROBANDO,
        ALCANZABLE,
        NO_ALCANZABLE
    }

    private static final int TIMEOUT_SEGUNDOS = 5;

    // Patrón para detectar respuesta exitosa en la salida del ping (Windows)
    // Ejemplos: "Respuesta desde 192.168.1.1:", "Reply from 192.168.1.1: bytes=32 tiempo<1ms TTL=128"
    private static final Pattern REPLY_PATTERN = Pattern.compile(
        "(?i)(respuesta desde|reply from)"
    );

    public static void ping(String host, Consumer<Estado> callback) {

    Platform.runLater(() ->
        callback.accept(Estado.COMPROBANDO)
    );

    CompletableFuture
        .supplyAsync(() -> comprobarPing(host))
        .thenAccept(alcanzable ->
            Platform.runLater(() ->
                callback.accept(
                    alcanzable
                        ? Estado.ALCANZABLE
                        : Estado.NO_ALCANZABLE
                )
            )
        );
}

    private static boolean comprobarPing(String host) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ping",
                "-n", "1",
                "-w", "400",   // tiempo de espera por respuesta en milisegundos
                host
            );
            pb.redirectErrorStream(true); // unificar stdout y stderr
            Process process = pb.start();

            boolean terminado = process.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            if (!terminado) {
                process.destroyForcibly();
                return false;
            }

            // Leer la salida completa del comando
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            String salidaCompleta = output.toString();

            // Si el código de salida es 0 Y además hay una línea que indica respuesta, es alcanzable
            // En Windows, a veces el código es 0 pero el mensaje es "Destination host unreachable"
            boolean exitOk = (process.exitValue() == 0);
            boolean tieneRespuesta = REPLY_PATTERN.matcher(salidaCompleta).find();

            // También podemos buscar "TTL=" o "tiempo<1ms" como refuerzo
            boolean tieneTtl = salidaCompleta.matches("(?s).*\\bTTL=\\d+\\b.*");
            boolean tieneTiempo = salidaCompleta.matches("(?s).*tiempo[<=]\\d+ms.*");

            return exitOk && (tieneRespuesta || tieneTtl || tieneTiempo);

        } catch (Exception e) {
            System.err.println("Error haciendo ping a " + host);
            e.printStackTrace();
            return false;
        }
    }
}