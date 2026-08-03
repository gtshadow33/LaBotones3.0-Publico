# 10 — Bloqueo de botones con contraseña del repositorio

## Contexto

El modo bloqueo desactiva la ejecución de botones (para evitar pulsaciones accidentales sobre equipos en producción), pero algunas acciones de la propia app —conectar, cambiar de tema, abrir mini-ventana— deben seguir disponibles incluso con el bloqueo activo. Además, desbloquear no puede ser un simple toggle sin fricción: si cualquiera puede desbloquear con un clic, el bloqueo no protege nada.

## Decisión

- **`BloqueoManager`** mantiene el estado `bloqueado` (persistido vía `ConfigManager.isBloqueado()`), y referencias directas a los botones excluidos del bloqueo (`btnConectar`, `btnTheme`, `btnMini`) para que sigan habilitados aunque el resto del grid se desactive.
- **Desbloquear requiere contraseña**, verificada contra la contraseña configurada en el repositorio compartido (`SyncManager.verificarPassword(password)`) — la misma contraseña que protege la sincronización, para no obligar a gestionar dos credenciales distintas.
- **Si el repositorio no tiene contraseña configurada**, en vez de fallar silenciosamente o bloquear la función, se le pregunta explícitamente al usuario si quiere continuar sin contraseña (bloqueo "libre", solo un toggle) o configurar una en ese momento. Esto cubre tanto el caso de un despliegue nuevo (sin repositorio aún configurado) como el de alguien que deliberadamente no quiere usar contraseña.
- **Por botón**, un campo `excluidoBloqueo` permite marcar botones individuales como exentos del bloqueo general (por ejemplo, un botón de "ver estado" que no hace ningún cambio y es seguro dejar siempre disponible).

## Por qué así y no de otra forma

- **¿Por qué reutilizar la contraseña del repositorio en vez de una contraseña de bloqueo independiente?** Menos credenciales que recordar y gestionar para el equipo de técnicos; la contraseña del repositorio ya implica "tienes permiso de administración sobre la configuración compartida", que es el mismo nivel de confianza que hace falta para poder desbloquear botones.
- **¿Por qué no simplemente deshabilitar el bloqueo si no hay contraseña, en vez de preguntar?** Forzar a configurar contraseña sí o sí sería un obstáculo para probar la app o para equipos pequeños que no necesitan esa protección. Preguntar deja la decisión en manos de quien despliega la app.

## Trade-offs

- El bloqueo "libre" (sin contraseña) es, en la práctica, solo una protección contra clics accidentales, no contra alguien que decida desbloquear a propósito. Es una decisión consciente: el nivel de seguridad se adapta a si el despliegue configura contraseña o no.
