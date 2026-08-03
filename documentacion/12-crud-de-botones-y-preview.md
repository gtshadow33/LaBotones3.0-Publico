# 12 — CRUD de botones y preview de iconos en el combo

## Contexto

Al crear o editar un botón, hay que elegir su icono de una lista de ficheros disponibles en la carpeta `icons/`. Mostrar solo el nombre del fichero (`restart.png`, `disk.png`...) obliga a adivinar o recordar qué imagen es cada uno; para un formulario que se usa a menudo, eso añade fricción innecesaria.

## Decisión

El `ComboBox` de iconos usa una `cellFactory` personalizada que renderiza, para cada opción, el propio icono en miniatura junto a su nombre — no solo texto:

```java
comboIcono.setCellFactory(cellFactory);
comboIcono.setButtonCell(cellFactory.call(null)); // también en la celda "cerrada" del combo
```

Cada celda carga la imagen a través de `IconCache.obtener(file, 20, 20)` (ver `04-cache-de-iconos.md`) en vez de crear una `Image` nueva cada vez que JavaFX repinta la celda — el combo se repinta con frecuencia al desplegar/hacer scroll, así que reutilizar la caché evita releer del disco en cada repintado.

Se añade explícitamente una opción vacía (`""`) al principio de la lista, mostrada como "(Sin icono)", para permitir botones sin icono sin forzar a elegir uno.

## Por qué así y no de otra forma

- **¿Por qué `setButtonCell` además de `setCellFactory`?** `setCellFactory` solo controla cómo se ven las opciones *desplegadas*; sin `setButtonCell`, la celda que se ve cuando el combo está cerrado (mostrando la opción seleccionada) seguiría siendo solo texto, sin el icono. Hacía falta configurar ambas para que la previsualización fuera consistente en los dos estados del combo.
- **¿Por qué reusar `IconCache` en vez de cargar la imagen directamente aquí?** Evita duplicar la lógica de caché/invalidación por fecha de modificación en dos sitios distintos del código; el CRUD y el grid principal comparten la misma caché de iconos.

## Trade-offs

- La `cellFactory` se ejecuta por cada celda visible al desplegar el combo, así que con una carpeta de iconos muy grande (cientos de ficheros) el desplegado podría notarse más lento. Para el volumen real de iconos del proyecto (decenas), no es un problema práctico.
