# 14 — Estrategia de tests: aislamiento y "carreras controladas"

## Contexto

Dos problemas concretos a resolver al testear este proyecto:

1. El estado de `ButtonService` es `static` y compartido entre instancias por diseño (ver `05-persistencia-y-datos.md`), lo que significa que si un test deja botones "de prueba" en la caché, el siguiente test puede arrastrarlos y dar resultados falsos o inconsistentes según el orden de ejecución.
2. Probar código de concurrencia (¿de verdad `AtomicBoolean.compareAndSet` evita que dos sincronizaciones se pisen?) no es tan simple como llamar al método dos veces seguidas desde el mismo hilo — hay que forzar de verdad una condición de carrera real entre hilos distintos para que el test signifique algo.

## Decisión

**Aislamiento de estado entre tests**

Según qué se testea, se usan dos estrategias distintas:
- **`@TempDir` de JUnit 5** (en `SyncManagerTest`, por ejemplo): cada test recibe un directorio temporal propio, así que los ficheros que crea no interfieren con otros tests ni con el entorno real.
- **Backup/restore explícito del fichero real + `reload()` forzado** (en `ButtonServiceConcurrencyTest`): como la caché de `ButtonService` es estática, antes de cada test se hace copia de seguridad del `botones.json` real, se vacía el fichero a un estado limpio conocido (`"[]"`) y se fuerza una recarga (`new ButtonService().reload()`) para partir de cero. Al terminar, se restaura el contenido original desde la copia de seguridad.

**Tests de concurrencia con doble `CountDownLatch`**

Para forzar una condición de carrera real (no simulada) entre varios hilos:

```java
CountDownLatch salida = new CountDownLatch(1);       // "pistoletazo de salida"
CountDownLatch terminados = new CountDownLatch(hilos); // cuenta atrás hasta que todos acaben

for (int i = 0; i < hilos; i++) {
    pool.submit(() -> {
        salida.await();       // todos los hilos esperan aquí a la vez
        // ... operación a testear (alta de un botón, por ejemplo) ...
        terminados.countDown();
    });
}
salida.countDown();  // libera a todos los hilos a la vez, de golpe
terminados.await(10, TimeUnit.SECONDS);
```

El primer `CountDownLatch` retiene a todos los hilos justo antes de la operación crítica hasta que se liberan todos a la vez con un único `countDown()`, maximizando la probabilidad de que la condición de carrera ocurra de verdad (en vez de que, por azar de scheduling, los hilos se ejecuten uno detrás de otro sin llegar a solaparse nunca). El segundo latch espera a que todos terminen, con un timeout de 10 segundos para que el test falle explícitamente en vez de colgarse para siempre si algo se bloquea.

Después, las aserciones comprueban invariantes que solo se rompen si hubo una condición de carrera real: número exacto de botones creados (sin pérdidas), IDs únicos (sin duplicados), y cero excepciones inesperadas capturadas durante la ejecución concurrente.

## Por qué así y no de otra forma

- **¿Por qué no `Thread.sleep` para simular concurrencia?** Un `sleep` no garantiza que los hilos se solapen de verdad en el momento crítico; el doble latch sí lo garantiza, porque libera a todos los hilos exactamente en el mismo instante.
- **¿Por qué comprobar "sin pérdidas" y "sin duplicados" en vez de solo "no lanzó excepción"?** Una condición de carrera mal gestionada en código concurrente a menudo *no* lanza ninguna excepción — simplemente pierde datos silenciosamente (dos hilos escriben y uno pisa al otro sin que nadie se entere). Comprobar solo que no hay excepción daría una falsa sensación de seguridad.

## Trade-offs

- El patrón de backup/restore de fichero real en `ButtonServiceConcurrencyTest` es más frágil que usar `@TempDir` (si el test se interrumpe a mitad, por ejemplo con un `kill -9`, el `@AfterEach` no llega a restaurar el fichero original). Se aceptó en ese test concreto porque la caché estática de `ButtonService` no permite fácilmente apuntar a un directorio temporal distinto sin cambiar también el diseño de producción solo para hacerlo testeable.
