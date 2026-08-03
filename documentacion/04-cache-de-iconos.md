# 04 — Caché de iconos

## Contexto

Cargar un icono con `new Image(file.toURI().toString())` usa la caché interna de JavaFX, indexada solo por URL. Esto genera un bug sutil: si el usuario cambia el icono de un botón manteniendo el mismo nombre de fichero, la app sigue mostrando la imagen vieja hasta reiniciar, porque JavaFX no sabe que el contenido del fichero cambió.

La solución obvia — dejar de usar caché y releer siempre de disco — soluciona ese bug pero crea uno de rendimiento: el grid se repinta en cada tecla que se escribe en el buscador (ver `06-busqueda-y-rendimiento-ui.md`), así que sin ninguna caché se releerían todos los iconos del disco en cada pulsación de tecla. Con una lista grande de botones, esto se nota.

## Decisión

`IconCache` implementa una caché propia indexada por **ruta absoluta + última fecha de modificación** del fichero:

```java
record Entrada(long lastModified, Image image) {}
Map<String, Entrada> CACHE = new ConcurrentHashMap<>();
```

Al pedir un icono: si ya hay una entrada para esa ruta y el `lastModified()` del fichero coincide con el guardado, se devuelve la imagen en memoria (rápido, sin tocar disco). Si el fichero cambió (o es la primera vez que se pide), se relee y se actualiza la entrada.

Esto resuelve las dos cosas a la vez: los cambios de icono se reflejan sin reiniciar la app, y no se relee del disco en cada repintado si nada cambió.

## Por qué así y no de otra forma

- **¿Por qué `ConcurrentHashMap` y no un `HashMap` con `synchronized`?** El grid puede pedir iconos desde el hilo de carga en segundo plano (`gridExecutor`) mientras la UI sigue pintando con datos ya cacheados; `ConcurrentHashMap` da acceso concurrente seguro sin bloquear todo el mapa en cada lectura.
- **¿Por qué no invalidar por hash del contenido (checksum) en vez de `lastModified()`?** `lastModified()` es prácticamente gratis (metadato del sistema de ficheros) frente a tener que leer y hashear el fichero entero solo para saber si cambió, lo cual habría anulado la ventaja de tener caché.

## Trade-offs

- Si algo modifica un fichero de icono sin cambiar su `lastModified` (poco común, pero posible con ciertas herramientas que preservan timestamps), la caché no lo detectaría. Se aceptó como caso extremadamente raro frente al beneficio de no hashear contenido constantemente.
- La caché nunca se purga de entradas antiguas (iconos borrados o renombrados quedan en memoria). Para el volumen de iconos de esta app (decenas, no miles) el coste de memoria es despreciable.
