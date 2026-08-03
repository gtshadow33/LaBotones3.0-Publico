# 05 — Persistencia y datos: JSON + locks en vez de base de datos

## Contexto

La app necesita persistir botones, configuración e historial de equipos consultados. El volumen de datos es pequeño (decenas de botones, un puñado de propiedades de configuración) y la app es de un solo usuario por instalación (cada técnico tiene su propia copia portable).

## Decisión

Persistencia en ficheros JSON (vía Jackson) y `.properties`, con:
- Caché en memoria cargada una única vez de forma *eager* (al tocar la clase por primera vez), aprovechando que la inicialización de campos estáticos en Java ya es thread-safe por especificación (JLS 12.4.2) — no hace falta ningún flag `loaded` ni doble-check locking manual.
- Un `ReentrantLock` único que protege todo acceso (lectura y escritura) a esa caché.
- Escritura atómica a disco (`.tmp` + `ATOMIC_MOVE`, ver `02-concurrencia-sincronizacion.md`).

## Por qué así y no de otra forma

- **¿Por qué no SQLite u otra base de datos embebida?** Para el volumen de datos de este proyecto (una lista de botones, un historial corto), una base de datos añade una dependencia, un esquema que mantener y una capa de queries para resolver un problema que un `List<Boton>` en memoria con un lock ya resuelve. El coste de una BD solo se justifica cuando el volumen de datos o la necesidad de queries complejas lo piden, y aquí no es el caso.
- **¿Por qué Jackson y no serialización manual o `Gson`?** Jackson ya estaba integrado en el proyecto desde el inicio y da soporte robusto a los `record` de Java (usados en `IconCache`, por ejemplo) y a anotaciones de mapeo cuando el JSON no coincide 1:1 con los campos de la clase.
- **¿Por qué caché estática compartida entre instancias en vez de una instancia inyectada (patrón repositorio clásico)?** Varias partes de la app (`Controller`, `ButtonCRUDController`) crean su propia instancia de `ButtonService`, pero todas deben ver el mismo estado sin depender de que alguien recuerde llamar a `reload()` para sincronizarse. Compartir el estado a nivel de clase evita esa coordinación manual propensa a errores.

## Trade-offs

- Al no haber transacciones ni validación de esquema real (más allá de lo que Jackson valida al deserializar), una edición manual mal hecha del JSON puede dejar el fichero en un estado inconsistente que la app no detecta hasta usarlo. Aceptable porque el fichero no está pensado para edición manual habitual.
- El estado estático compartido (ver también `05`) hace que los tests tengan que aislar explícitamente el directorio de datos por test (con `@TempDir` de JUnit 5) para no interferir entre ellos.
