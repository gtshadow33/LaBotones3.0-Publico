# 06 — Búsqueda con debounce y rendimiento del grid

## Contexto

El campo de búsqueda filtra el grid de botones en tiempo real mientras el usuario escribe. Filtrar y repintar el grid en *cada* pulsación de tecla, sin ningún tipo de espera, tiene dos problemas: repinta de más (si escribes "RET" rápido, se repinta con "R", con "RE" y con "RET", cuando solo el último resultado importa) y, combinado con la carga de iconos (ver `04-cache-de-iconos.md`), puede notarse como tirones en listas grandes de botones.

## Decisión

Se usa un `PauseTransition` de JavaFX como debounce: cada pulsación de tecla reinicia un temporizador corto; el filtrado real solo se dispara cuando el usuario deja de teclear durante ese intervalo, no en cada tecla individual.

```java
PauseTransition debounce = new PauseTransition(Duration.millis(200));
debounce.setOnFinished(e -> aplicarFiltro(textoActual));

campoBusqueda.textProperty().addListener((obs, oldVal, newVal) -> {
    debounce.stop();
    debounce.playFromStart();
});
```

## Por qué así y no de otra forma

- **¿Por qué no un `Thread.sleep` manual o un `ScheduledExecutorService`?** `PauseTransition` ya forma parte del toolkit de animaciones de JavaFX y se ejecuta en el hilo de la UI, así que no hace falta preocuparse por hacer `Platform.runLater` para volver al hilo correcto tras el timer, como sí haría falta con un executor genérico.
- **¿Por qué 200ms y no otro valor?** Es un punto intermedio estándar en UIs de escritorio: perceptible como "instantáneo" para quien escribe a ritmo normal, pero suficiente para no repintar en cada tecla durante una ráfaga de escritura rápida.

## Trade-offs

- Con el debounce activo, el resultado del filtro tarda esos ~200ms en aparecer tras la última tecla, en vez de ser instantáneo tecla a tecla. Es una decisión consciente de fluidez percibida global sobre "reactividad" de cada pulsación individual.
