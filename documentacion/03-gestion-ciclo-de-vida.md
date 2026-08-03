# 03 — Ciclo de vida: el `ExecutorService` zombie y el patrón `Navigator`

## Contexto

Cada `Controller` crea sus propios pools de hilos (`gridExecutor`, `executor`) para no bloquear el hilo de JavaFX al cargar botones, iconos o hacer ping. El problema apareció al navegar entre pantallas: cuando se creaba un nuevo `Controller` (por ejemplo, al reabrir la ventana principal), el antiguo se quedaba sin referencias visibles desde la UI... pero su `ExecutorService` seguía vivo, con hilos non-daemon esperando trabajo indefinidamente. Resultado: la aplicación no terminaba de cerrarse aunque se cerrara la última ventana visible, o acumulaba hilos fantasma con cada navegación.

## Decisión

`App` mantiene una referencia estática al controller "activo" y expone `setActiveController`:

```java
public static void setActiveController(Controller newController) {
    if (controller != null && controller != newController) {
        controller.shutdown();
    }
    controller = newController;
}
```

Cuando se crea un controller nuevo, se llama a este método, que primero apaga (`shutdown()`, cierre de executors incluido) el controller anterior antes de sustituirlo. Así nunca queda un controller "huérfano" con sus pools de hilos corriendo en segundo plano.

Además, `stage.setOnCloseRequest` llama explícitamente a `controller.shutdown()` al cerrar la ventana, para cubrir también el cierre desde el aspa de la ventana (no solo la navegación entre pantallas).

`Navigator` centraliza el acceso al `Stage` principal (`Navigator.initStage(stage)`), evitando que cada pantalla nueva tenga que recibir el `Stage` a mano por parámetros encadenados.

## Por qué así y no de otra forma

- **¿Por qué no un contador de referencias o un `WeakReference` al controller antiguo para que el GC lo recoja solo?** Un `ExecutorService` con hilos vivos nunca es recogido por el GC aunque nada más lo referencie: los propios hilos mantienen vivo el objeto. Hace falta un `shutdown()` explícito sí o sí; no hay atajo con recolección de basura automática aquí.
- **¿Por qué un solo controller "activo" estático y no una pila de navegación (back stack)?** La app no tiene una navegación profunda tipo wizard; solo alterna entre una ventana principal y mini-ventanas/diálogos. Una pila habría añadido complejidad sin necesidad real.

## Trade-offs

- Usar un campo estático (`App.controller`) es un pequeño antipatrón de estado global, pero para una app de una sola ventana principal (no hay múltiples instancias del `Controller` corriendo a la vez de forma legítima) el riesgo es bajo y simplifica mucho el ciclo de vida frente a inyectar el controller activo por todas partes.
