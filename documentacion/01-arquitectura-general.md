# 01 — Arquitectura general: de un Controller a varios gestores

## Contexto

La primera versión de `Controller.java` concentraba prácticamente toda la lógica de la ventana principal: grid de botones, ventanas mini, sincronización con el repositorio, bloqueo de botones, temas... Un único "god object" de más de 950 líneas, con métodos que tocaban tres o cuatro responsabilidades distintas a la vez.

Los síntomas concretos que llevaron al refactor:
- Cambiar algo de sincronización obligaba a leer medio fichero para entender si afectaba también al grid o al bloqueo.
- Los tests eran casi imposibles de escribir de forma aislada: probar la lógica de bloqueo arrastraba dependencias de JavaFX (Stage, Scene) que no tenían nada que ver.
- Cualquier bug de concurrencia (ver `02-concurrencia-sincronizacion.md`) era difícil de rastrear porque el estado mutable estaba disperso por todo el fichero sin un dueño claro.

## Decisión

Se extrajo la lógica en gestores especializados, cada uno con una responsabilidad y un estado que le pertenece solo a él:

- **`BotonesGridManager`** — construcción y refresco del grid principal, filtrado/búsqueda.
- **`MiniWindowManager`** — apertura, cierre y sincronización de las mini-ventanas.
- **`SyncController`** — orquesta la sincronización con el repositorio compartido (delega en `SyncManager` para el trabajo pesado).
- **`BloqueoManager`** — estado de bloqueo de botones y qué botones quedan excluidos.

`Controller.java` pasó de ~956 a ~530 líneas, y ahora actúa como *fachada*: recibe los eventos de FXML y los reparte al gestor correspondiente, sin implementar la lógica él mismo.

## Por qué así y no de otra forma

- **¿Por qué no un patrón MVC/MVVM completo con un framework?** Para el tamaño de la app (una ventana principal + mini-ventanas + un par de diálogos) habría sido sobreingeniería. Los gestores dan la separación de responsabilidades que hacía falta sin añadir una capa de abstracción nueva que aprender.
- **¿Por qué no dividir por pantalla en vez de por responsabilidad?** Porque varias pantallas comparten responsabilidades (el grid principal y las mini-ventanas comparten lógica de botones, por ejemplo). Dividir por responsabilidad evita duplicar código entre "pantallas".

## Trade-offs

- `Controller` sigue siendo el punto de entrada de todos los eventos FXML, así que sigue siendo el fichero más grande del proyecto. Se aceptó porque JavaFX obliga a tener *algún* controller enlazado al FXML; lo importante era que dejara de contener lógica de negocio.
- Los gestores se comunican entre sí a través de `Controller`, no directamente. Esto añade algo de indirección, pero evita acoplar `BotonesGridManager` con `SyncController` (por ejemplo) de forma que sea difícil sustituir uno sin tocar el otro.
