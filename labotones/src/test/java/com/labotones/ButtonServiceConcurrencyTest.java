package com.labotones;

import com.labotones.Class_help.Button_prop;
import com.labotones.Class_help.ButtonService;
import com.labotones.Class_help.ButtonService.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ButtonServiceConcurrencyTest {

    private static final File BOTONES_JSON = new File("botones.json");

    private byte[] backupOriginal;
    private boolean existiaOriginal;

    @BeforeEach
    void backupYLimpiar() throws Exception {
        existiaOriginal = BOTONES_JSON.exists();
        if (existiaOriginal) {
            backupOriginal = Files.readAllBytes(BOTONES_JSON.toPath());
        }
        // Vaciar el fichero para partir de un estado limpio y conocido
        Files.write(BOTONES_JSON.toPath(), "[]".getBytes());

        // Como la caché es static, hay que forzar una recarga desde el
        // fichero limpio antes de cada test; si no, arrastraríamos el estado
        // (y los IDs) del test anterior.
        new ButtonService().reload();
    }

    @AfterEach
    void restaurarOriginal() throws Exception {
        if (existiaOriginal) {
            Files.write(BOTONES_JSON.toPath(), backupOriginal);
        } else {
            Files.deleteIfExists(BOTONES_JSON.toPath());
        }
        Files.deleteIfExists(new File(BOTONES_JSON.getAbsolutePath() + ".tmp").toPath());
        // Dejar la caché en un estado consistente con lo restaurado, para no
        // "contaminar" al resto de tests de la suite (p.ej. ButtonServiceTest).
        new ButtonService().reload();
    }

    private Button_prop nuevoBoton(String nombre) {
        Button_prop b = new Button_prop();
        b.setName(nombre);
        b.setLocateBat("script.bat");
        b.setDescription("Botón de test");
        return b;
    }

    @Test
    void testAltasConcurrentesNoPierdenBotonesNiDuplicanId() throws InterruptedException {
        final int hilos = 20;
        ButtonService service = new ButtonService();

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        CountDownLatch terminados = new CountDownLatch(hilos);
        AtomicInteger fallos = new AtomicInteger(0);

        for (int i = 0; i < hilos; i++) {
            final int indice = i;
            pool.submit(() -> {
                try {
                    // Todos los hilos esperan aquí y arrancan a la vez,
                    // para maximizar la contención real sobre la caché.
                    salida.await();
                    Result r = service.add(nuevoBoton("Boton-" + indice));
                    if (r != Result.OK) fallos.incrementAndGet();
                } catch (Exception e) {
                    fallos.incrementAndGet();
                } finally {
                    terminados.countDown();
                }
            });
        }

        salida.countDown(); // ¡ya!
        boolean acabaronATiempo = terminados.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(acabaronATiempo, "Los hilos no terminaron a tiempo (posible interbloqueo)");
        assertEquals(0, fallos.get(), "Alguna alta concurrente falló o lanzó excepción");

        List<Button_prop> todos = service.getAll();
        assertEquals(hilos, todos.size(), "Se han perdido altas concurrentes (race condition)");

        Set<Integer> idsUnicos = todos.stream().map(Button_prop::getID).collect(Collectors.toSet());
        assertEquals(hilos, idsUnicos.size(), "Hay IDs duplicados entre botones creados a la vez");

        Set<String> nombresEsperados = IntStream.range(0, hilos)
                .mapToObj(i -> "Boton-" + i)
                .collect(Collectors.toSet());
        Set<String> nombresReales = todos.stream().map(Button_prop::getName).collect(Collectors.toSet());
        assertEquals(nombresEsperados, nombresReales, "Faltan o sobran botones tras las altas concurrentes");
    }

    @Test
    void testLecturasConcurrentesDuranteEscriturasNoRompen() throws InterruptedException {
        ButtonService service = new ButtonService();
        // Un puñado de botones de partida para que getAll() tenga algo que copiar.
        for (int i = 0; i < 5; i++) {
            service.add(nuevoBoton("Inicial-" + i));
        }

        final int hilosLectores = 15;
        final int hilosEscritores = 5;
        ExecutorService pool = Executors.newFixedThreadPool(hilosLectores + hilosEscritores);
        CountDownLatch salida = new CountDownLatch(1);
        CountDownLatch terminados = new CountDownLatch(hilosLectores + hilosEscritores);
        AtomicInteger fallos = new AtomicInteger(0);

        // Lectores: solo deben ver siempre una foto consistente, nunca petar.
        for (int i = 0; i < hilosLectores; i++) {
            pool.submit(() -> {
                try {
                    salida.await();
                    for (int j = 0; j < 50; j++) {
                        List<Button_prop> snapshot = service.getAll();
                        // No debe poder mutarse la lista devuelta (copia defensiva real)
                        assertThrows(UnsupportedOperationException.class,
                                () -> snapshot.add(new Button_prop()));
                    }
                } catch (Exception e) {
                    fallos.incrementAndGet();
                } finally {
                    terminados.countDown();
                }
            });
        }

        // Escritores: van añadiendo botones nuevos mientras los lectores leen.
        for (int i = 0; i < hilosEscritores; i++) {
            final int indice = i;
            pool.submit(() -> {
                try {
                    salida.await();
                    Result r = service.add(nuevoBoton("Concurrente-" + indice));
                    if (r != Result.OK) fallos.incrementAndGet();
                } catch (Exception e) {
                    fallos.incrementAndGet();
                } finally {
                    terminados.countDown();
                }
            });
        }

        salida.countDown();
        boolean acabaronATiempo = terminados.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(acabaronATiempo, "Los hilos no terminaron a tiempo (posible interbloqueo)");
        assertEquals(0, fallos.get(), "Alguna lectura/escritura concurrente falló o lanzó excepción inesperada");
        assertEquals(5 + hilosEscritores, service.getAll().size(),
                "El número final de botones no cuadra tras lecturas/escrituras concurrentes");
    }

    @Test
    void testReloadConcurrenteConEscrituraNoCorrompeEstado() throws InterruptedException {
        ButtonService service = new ButtonService();
        service.add(nuevoBoton("Base"));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch salida = new CountDownLatch(1);
        CountDownLatch terminados = new CountDownLatch(4);
        AtomicInteger fallos = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            final int indice = i;
            pool.submit(() -> {
                try {
                    salida.await();
                    service.add(nuevoBoton("Nuevo-" + indice));
                } catch (Exception e) {
                    fallos.incrementAndGet();
                } finally {
                    terminados.countDown();
                }
            });
        }
        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    salida.await();
                    service.reload();
                    service.getAll(); // no debe lanzar nada tras el reload
                } catch (Exception e) {
                    fallos.incrementAndGet();
                } finally {
                    terminados.countDown();
                }
            });
        }

        salida.countDown();
        boolean acabaronATiempo = terminados.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(acabaronATiempo, "Los hilos no terminaron a tiempo (posible interbloqueo)");
        assertEquals(0, fallos.get(), "reload() concurrente con add() rompió algo");
        // No afirmamos un tamaño exacto (depende del orden real de ejecución),
        // solo que el estado quedó consistente y legible.
        assertDoesNotThrow(() -> service.getAll());
    }
}