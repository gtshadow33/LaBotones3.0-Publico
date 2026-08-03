# 13 — Empaquetado: jar "fat" + runtime portable, sin JRE externo

## Contexto

Los técnicos de UCA no tienen por qué tener Java instalado en sus equipos, ni permisos para instalar software adicional. La app tiene que poder copiarse a una carpeta y ejecutarse directamente, sin pasos de instalación ni dependencias externas que alguien tenga que preinstalar a mano.

## Decisión

El empaquetado combina dos piezas:

1. **`maven-shade-plugin`** genera un jar "fat" (uber-jar) que incluye Jackson y el resto de dependencias no-JavaFX dentro del propio `.jar`, con el `mainClass` (`com.labotones.App`) inyectado en el manifiesto para poder ejecutarlo directamente con `java -jar`.

2. **JavaFX se excluye explícitamente del jar** (`<exclude>org.openjfx:*</exclude>` en el `artifactSet`), porque JavaFX no se distribuye dentro del jar sino como un **runtime-image** generado aparte con `jlink`/`jpackage`: una carpeta autocontenida con un JRE mínimo (solo los módulos que la app realmente usa, no el JDK completo) más JavaFX ya integrado. El resultado es una carpeta portable que se copia y se ejecuta con un `.exe`/binario nativo, sin que el equipo de destino necesite tener Java instalado en absoluto.

## Por qué así y no de otra forma

- **¿Por qué no simplemente pedir que cada técnico instale un JRE?** En un entorno corporativo de aeropuerto, instalar software (aunque sea un JRE) en cada puesto suele requerir permisos de administrador y pasar por gestión de TI, lo que añade fricción y tiempo para desplegar cada actualización. Un runtime-image portable evita depender de eso: se copia una carpeta y funciona.
- **¿Por qué excluir JavaFX del jar "fat" en vez de meterlo también ahí?** JavaFX tiene módulos nativos específicos de plataforma (Windows/Linux/macOS); meterlos todos en un único jar multiplataforma infla el tamaño innecesariamente para cada instalación concreta, cuando `jlink` ya genera un runtime específico para la plataforma de destino con exactamente lo que hace falta.

## Trade-offs

- Hay que generar (y mantener) un runtime-image distinto por sistema operativo objetivo (Windows y Linux, según el README). Cada actualización de la app implica regenerar esos runtimes, no solo el jar.
- El tamaño total de la carpeta portable es mayor que un simple `.jar` (incluye un JRE mínimo completo), a cambio de no depender de nada preinstalado en el equipo de destino.
