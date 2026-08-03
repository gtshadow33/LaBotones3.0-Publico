# 02 — Concurrencia y sincronización con el repositorio

## Contexto

`SyncManager` sincroniza los botones locales con un repositorio compartido: descarga automática cada 60 minutos, y subida manual bajo demanda. El riesgo evidente es que ambas cosas ocurran a la vez (por ejemplo, el usuario pulsa "sincronizar ahora" justo cuando salta el timer automático), lo que podría dejar el fichero de botones corrupto o con datos mezclados de forma inconsistente.

## Decisión

**1. Guardia de sincronización con `AtomicBoolean`**

```java
if (!sincronizando.compareAndSet(false, true)) {
    // ya hay una sincronización en curso, se ignora esta llamada
    return;
}
try {
    // ... trabajo de sincronización ...
} finally {
    sincronizando.set(false);
}
```

`compareAndSet` es atómico: si dos hilos llaman a la vez, solo uno consigue pasar de `false` a `true`; el otro ve que ya estaba en `true` y sale sin hacer nada. No hace falta un `synchronized` de bloque completo para esto.

**2. "El repo siempre gana"**

La primera versión intentaba *fusionar* (merge) los botones locales con los del repositorio al descargar. Se abandonó: con varios técnicos editando botones desde distintos puestos, un merge automático puede combinar mal dos ediciones simultáneas y dejar un botón corrupto o duplicado sin que nadie se entere hasta que falla en producción. Se sustituyó por una copia directa: al descargar, el fichero del repositorio sustituye sin más al local. La regla es simple y predecible: "si quieres tus cambios a salvo, súbelos".

**3. Escritura atómica de ficheros**

Los ficheros JSON (botones, config) se escriben primero a un `.tmp` y luego se renombran con `ATOMIC_MOVE`:

```java
Path tmp = destino.resolveSibling(destino.getFileName() + ".tmp");
Files.write(tmp, contenido);
Files.move(tmp, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
```

Así, si la app se cierra o el proceso muere a mitad de escritura, el fichero original nunca queda a medio escribir (corrupto): o bien queda con el contenido antiguo completo, o con el nuevo completo. Nunca un híbrido ilegible.

**4. Lock en `ButtonService`: `ReentrantLock`, no `ReentrantReadWriteLock`**

Se evaluó un `ReentrantReadWriteLock` para permitir lecturas concurrentes de la lista de botones. Se descartó: la lista es pequeña (decenas de elementos) y se lee en microsegundos desde memoria, así que la ventaja teórica de paralelizar lecturas no compensa la complejidad extra que añade un read-write lock (upgrade de lectura a escritura, contadores por hilo). Un `ReentrantLock` exclusivo simple da el mismo resultado práctico con menos superficie de bugs.

## Por qué así y no de otra forma

- **¿Por qué no un `synchronized` a secas?** `AtomicBoolean.compareAndSet` expresa mejor la intención ("solo una sincronización a la vez, las demás se descartan sin esperar") que un `synchronized`, que haría esperar a los hilos en cola en vez de descartar la llamada redundante.
- **¿Por qué no una base de datos con transacciones en vez de JSON + locks manuales?** Ver `05-persistencia-y-datos.md`.

## Trade-offs

- "El repo siempre gana" significa que si alguien sincroniza sin haber subido antes sus cambios locales, los pierde sin aviso explícito más allá de lo que ya indique la UI. Es una decisión consciente de simplicidad sobre "inteligencia" del merge.
