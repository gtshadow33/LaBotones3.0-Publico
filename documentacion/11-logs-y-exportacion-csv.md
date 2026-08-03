# 11 — Logs y exportación a CSV

## Contexto

La app escribe logs de texto plano de la actividad (acciones ejecutadas, sincronizaciones, errores). Para auditoría o soporte, un técnico necesita poder extraer esos logs filtrados por fecha en un formato que se pueda abrir en Excel u otra herramienta, sin tener que instalar nada adicional.

## Decisión

- El formato de fecha en los logs sigue un patrón fijo y parseable: `DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")`, ISO-like, para que el filtrado por fecha sea una comparación directa de cadenas/objetos `LocalDate` sin ambigüedad de formato (nada de `dd/MM/yy` que puede confundirse con `MM/dd/yy` según la configuración regional de quien lo lea después).
- `exportarLogsCSV(destino, filtro)` recibe un filtro de fecha opcional (`LocalDate`, `null` = todas las fechas) y escribe solo las líneas que coinciden con esa fecha al CSV de destino.
- **Tolerancia a líneas mal formadas**: si una línea del log no sigue el formato esperado (por ejemplo, una traza de excepción multilínea que se coló en el fichero de texto), no se descarta ni rompe la exportación entera — se exporta tal cual, como una fila más, en vez de fallar todo el proceso por una línea inesperada.
- El nombre del fichero exportado incluye la fecha filtrada (`logs_2026-08-02.csv`) o se marca como `logs_completo.csv` si no se aplicó filtro, para que quede claro con solo mirar el nombre del fichero qué contiene.

## Por qué así y no de otra forma

- **¿Por qué logs.txt en texto plano y no una tabla en el JSON de datos o un logger estructurado (SLF4J + fichero rotado)?** Para un log de auditoría que un técnico va a leer directamente (no solo procesar por máquina), texto plano con timestamp legible es más rápido de inspeccionar a ojo sin herramientas adicionales que un JSON estructurado. La exportación a CSV cubre el caso de necesitar procesarlo en Excel.
- **¿Por qué tolerar líneas mal formadas en vez de exigir formato estricto?** Un log de producción con meses de historial va a tener, tarde o temprano, alguna línea que no encaja (una traza de excepción, un corte de energía a mitad de escritura). Fallar toda la exportación por una línea así sería peor que incluirla tal cual y dejar que el usuario la revise.

## Trade-offs

- No hay rotación automática del fichero de logs, así que puede crecer indefinidamente con el tiempo. Aceptable mientras el volumen se mantenga manejable para el uso previsto (consulta y exportación puntual, no procesamiento masivo).
