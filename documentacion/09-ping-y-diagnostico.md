# 09 — Comprobación de conectividad (ping)

## Contexto

Antes de ejecutar una acción sobre un equipo remoto, conviene saber si ese equipo está encendido y accesible en red. Hacerlo de forma síncrona (bloqueando el hilo de la UI mientras se espera respuesta del `ping`) habría congelado la interfaz durante ese tiempo, algo especialmente molesto si el equipo no responde y hay que esperar al timeout completo.

## Decisión

`PingService.ping(host, callback)` es asíncrono de punta a punta:

1. Notifica inmediatamente `Estado.COMPROBANDO` en el hilo de JavaFX (`Platform.runLater`), para que la UI pueda mostrar un indicador de "comprobando..." al instante.
2. Lanza el comando `ping` del sistema en un `CompletableFuture.supplyAsync`, fuera del hilo de la UI.
3. Cuando termina, vuelve a `Platform.runLater` para notificar `ALCANZABLE` o `NO_ALCANZABLE`, que es la única forma segura de tocar nodos de JavaFX desde un callback que se ejecutó en otro hilo.

Para decidir si el host es alcanzable no basta con mirar el código de salida del proceso: en Windows, el comando `ping` a veces devuelve código de salida `0` aunque el mensaje real sea "Destination host unreachable". Por eso se combina el código de salida con una búsqueda de patrones de éxito en el texto de salida (`Respuesta desde` / `Reply from`, presencia de `TTL=`, presencia de `tiempo<Nms`), y solo se considera alcanzable si el código de salida es 0 **y** al menos una de esas señales de texto aparece.

Además, se aplica un timeout doble: uno a nivel del propio comando `ping` (`-w 400`, 400ms por intento) y otro a nivel de proceso Java (`process.waitFor(5, SECONDS)`) que mata el proceso a la fuerza (`destroyForcibly()`) si no ha terminado a tiempo, para no dejar procesos `ping` colgados si el sistema operativo se comporta de forma inesperada.

## Por qué así y no de otra forma

- **¿Por qué invocar el binario `ping` del sistema en vez de usar `InetAddress.isReachable()` de Java?** `isReachable()` depende de si el firewall permite ICMP echo o (como fallback) un intento de conexión TCP al puerto 7, y en redes corporativas con políticas de firewall estrictas suele dar falsos negativos. Usar el `ping` real del sistema operativo da un resultado más fiable porque es el mismo mecanismo que usaría un técnico manualmente para comprobar un equipo.
- **¿Por qué doble validación (código de salida + texto) en vez de confiar solo en el código de salida?** Por la inconsistencia observada de Windows devolviendo éxito (`0`) en escenarios de host inalcanzable. Confiar solo en el código de salida daría falsos positivos.

## Trade-offs

- Depender del formato de texto de salida del comando `ping` (en español o inglés según el idioma del sistema) es fràgil ante cambios de localización del sistema operativo. Se mitiga aceptando ambos patrones ("respuesta desde" / "reply from") con `(?i)` (case-insensitive), pero un sistema en un tercer idioma podría no encajar con ninguno de los dos.
