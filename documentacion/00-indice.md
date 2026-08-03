# Documentación técnica — La Botones 3.0

Esta carpeta reúne las decisiones de diseño más importantes del proyecto: no solo *qué* se hizo, sino *por qué* se hizo así y qué alternativas se descartaron. Pensada tanto para quien retome el proyecto como para explicar el trabajo en una entrevista técnica.

## Índice

1. [01-arquitectura-general.md](01-arquitectura-general.md) — Visión general y por qué se dividió `Controller`.
2. [02-concurrencia-sincronizacion.md](02-concurrencia-sincronizacion.md) — Bloqueos, `AtomicBoolean`, escritura atómica de ficheros.
3. [03-gestion-ciclo-de-vida.md](03-gestion-ciclo-de-vida.md) — El fallo de `ExecutorService` zombie y el patrón `Navigator`.
4. [04-cache-de-iconos.md](04-cache-de-iconos.md) — Por qué la caché de JavaFX no bastaba.
5. [05-persistencia-y-datos.md](05-persistencia-y-datos.md) — Jackson, JSON, locks y por qué no hay base de datos.
6. [06-busqueda-y-rendimiento-ui.md](06-busqueda-y-rendimiento-ui.md) — Debounce de búsqueda y repintado del grid.
7. [07-sistema-de-temas.md](07-sistema-de-temas.md) — Por qué un único CSS y no 8 ficheros.
8. [08-decisiones-descartadas.md](08-decisiones-descartadas.md) — Cosas que se probaron y se tiraron atrás, y por qué.
9. [09-ping-y-diagnostico.md](09-ping-y-diagnostico.md) — Comprobación de conectividad asíncrona.
10. [10-bloqueo-de-botones.md](10-bloqueo-de-botones.md) — Bloqueo con contraseña y botones exentos.
11. [11-logs-y-exportacion-csv.md](11-logs-y-exportacion-csv.md) — Formato de logs y exportación tolerante a fallos.
12. [12-crud-de-botones-y-preview.md](12-crud-de-botones-y-preview.md) — Preview de iconos en el combo del CRUD.
13. [13-empaquetado-distribucion.md](13-empaquetado-distribucion.md) — jar "fat" + runtime portable con jlink/jpackage.
14. [14-estrategia-de-tests.md](14-estrategia-de-tests.md) — Aislamiento de estado y tests de condiciones de carrera.

## Cómo leer esto

Cada documento sigue más o menos la misma estructura:
- **Contexto**: qué problema había.
- **Decisión**: qué se implementó.
- **Por qué así y no de otra forma**: alternativas consideradas y motivo del descarte.
- **Trade-offs**: qué se sacrifica a cambio.