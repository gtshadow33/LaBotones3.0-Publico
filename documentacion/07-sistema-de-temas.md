# 07 — Sistema de temas: un único CSS, clases dinámicas

## Contexto

La app soporta 8 temas visuales (Light, Dark, Dracula, Matrix, Nord, Sunset,  Azul , y Alpha). Aplicarlos tenía que ser: instantáneo (sin parpadeo ni recarga de ventana), fácil de extender con temas nuevos, y consistente entre la ventana principal y las mini-ventanas.

## Decisión

- **Un único fichero `styles.css`**, con un bloque de reglas por tema bajo una clase raíz distinta: `.dark-mode`, `.dracula-mode`, `.nord-mode`, `.sunset-mode`, `.azul-mode`, `.alpha-mode`, etc.
- **`ThemeHelper`** cambia el tema de un nodo quitando todas las clases de tema conocidas y añadiendo solo la del tema activo:

```java
node.getStyleClass().removeAll(
    "light-mode", "dark-mode", "dracula-mode", "matrix-mode", ...
);
node.getStyleClass().add(temaActivo + "-mode");
```

- **`ThemeManager`** guarda el tema activo en `config.properties` (clave `theme`) y expone `toggle()` para rotar en orden circular entre los 8.
- Migración retrocompatible: si en `config.properties` no hay `theme` pero sí el antiguo `darkMode` (boolean de versiones previas con solo claro/oscuro), se traduce automáticamente a `Theme.DARK`/`Theme.LIGHT` y se elimina la clave vieja, sin romper la configuración de quien actualice desde una versión anterior.

## Por qué así y no de otra forma

- **¿Por qué un único CSS de +3000 líneas y no un fichero por tema?** JavaFX permite cargar varias hojas de estilo, pero un único fichero evita tener que gestionar el orden de carga/descarga de hojas al cambiar de tema (con el riesgo de que una regla de un tema "se quede pegada" si no se descarga bien la hoja anterior). Quitar y poner una clase CSS es una operación atómica y sin ese riesgo.
- **¿Por qué clases CSS y no cambiar colores por código (`setStyle` inline)?** Cambiar clases mantiene toda la definición visual declarativa en el CSS, donde es más fácil de mantener y de que alguien sin tocar Java pueda ajustar un color. Con `setStyle` inline, los colores quedarían dispersos por el código Java.

## Trade-offs

- Un CSS de +3000 líneas es un fichero grande de navegar. Se mitiga con una convención de nombres consistente (`-mode` como sufijo de cada tema) que hace el fichero buscable por bloques.
