# 08 — Decisiones descartadas

Un resumen de cosas que se probaron y se revirtieron, con el motivo. Útil para no repetir los mismos experimentos si el proyecto sigue evolucionando.

## Merge automático de botones al sincronizar

**Se probó:** al descargar del repositorio compartido, combinar (merge) los botones locales con los remotos en vez de sustituir sin más.

**Se descartó porque:** con varios técnicos editando desde distintos puestos, un merge automático puede combinar mal dos ediciones simultáneas del mismo botón (por ejemplo, si dos personas cambian el mismo botón con criterios distintos) y dejarlo en un estado que nadie valida hasta que falla en producción. Se sustituyó por la regla simple "el repo siempre gana" (ver `02-concurrencia-sincronizacion.md`).

## `ReentrantReadWriteLock` en `ButtonService`

**Se probó:** un lock de lectura/escritura para permitir lecturas concurrentes de la lista de botones.

**Se descartó porque:** la lista es pequeña y se lee en microsegundos desde memoria. La complejidad extra de un read-write lock (upgrade de lectura a escritura, contadores por hilo) no se traduce en ninguna mejora medible frente a un `ReentrantLock` simple, y sí en más superficie para bugs de concurrencia sutiles.

## Caché de iconos de JavaFX por defecto (`new Image(uri)`)

**Se probó:** confiar en la caché interna de JavaFX, indexada por URL.

**Se descartó porque:** esa caché no detecta cambios de contenido en un fichero si el nombre no cambia — un icono editado seguía viéndose con la versión antigua hasta reiniciar la app. Se sustituyó por `IconCache`, indexada por ruta + fecha de modificación (ver `04-cache-de-iconos.md`).

## `Controller` único con toda la lógica

**Se probó (era el diseño original):** un solo `Controller` de ~950 líneas con toda la lógica de grid, mini-ventanas, sincronización y bloqueo.

**Se descartó porque:** dificultaba tanto el mantenimiento como los tests aislados (ver `01-arquitectura-general.md`). Se dividió en gestores especializados.

## Boolean `darkMode` como única opción de tema

**Se probó (diseño original):** un flag booleano claro/oscuro en `config.properties`.

**Se descartó porque:** no escalaba a más de 2 temas. Se sustituyó por un enum `Theme` con 8 valores, manteniendo compatibilidad retroactiva con el flag antiguo en el arranque (ver `07-sistema-de-temas.md`).
