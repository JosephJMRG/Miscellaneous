# MiningMod

MiningMod es un mod para Minecraft basado en Fabric que automatiza acciones de minería de forma AFK (Away From Keyboard) y que incorpora mecanismos anti-AFK para evitar desconexiones por inactividad. El mod simula interacciones como clics y movimientos de teclado, mostrando mensajes en pantalla mediante un HUD de depuración.

---

## Características

- **Automatización de minería AFK:**  
  Simula clics de ataque (minería) y movimientos (adelante y atrás) para emular la acción de minar de forma continua.

- **Mecanismo anti-AFK:**  
  Un hilo secundario pausa periódicamente la secuencia principal para evitar que el jugador sea detectado como inactivo, reduciendo así el riesgo de desconexión.

- **Debug HUD:**  
  Muestra mensajes en pantalla con un efecto de fade-out para informar al usuario del estado actual del mod (por ejemplo, activación, pausas, countdown).

- **Activación/desactivación rápida:**  
  Utiliza la tecla **K** para alternar entre el modo AFK Mining habilitado y deshabilitado.

---

## Requisitos

- **Minecraft:** Versión compatible con Fabric (p.ej., 1.21.4).
- **Fabric Loader y Fabric API:** Instala [Fabric Loader](https://fabricmc.net/use/) y la [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) correspondientes a la versión de Minecraft.
- **Java:** JDK 8 o superior.

---

## Instalación

1. **Descargar el mod:**  
   Descarga el archivo `miningmod-1.21.4.zip` y extrae el contenido o utiliza el JAR generado.

2. **Instalar Fabric:**  
   Asegúrate de tener instalado el Fabric Loader y la Fabric API en tu instalación de Minecraft.

3. **Copiar el mod:**  
   Coloca el archivo JAR del mod en la carpeta `mods` de tu instalación de Minecraft.

4. **Iniciar Minecraft:**  
   Ejecuta Minecraft utilizando el perfil de Fabric para cargar el mod.

---

## Uso

- **Activar/Desactivar el mod:**  
  Durante el juego, presiona la tecla **K** para activar o desactivar el algoritmo de minería AFK.
  
- **Visualización de mensajes:**  
  El mod mostrará mensajes en pantalla (a través del Debug HUD) informándote sobre el estado del mod, como la activación, pausas y otros eventos importantes.

- **Anti-AFK:**  
  El mod incorporará pausas aleatorias para simular actividad, evitando así la desconexión automática por inactividad.

---

## Descripción de los archivos principales

### `MiningmodClient.java`
- **Función Principal:**  
  Inicializa el mod en el cliente de Minecraft y gestiona el ciclo de vida de la automatización AFK.
- **Características Clave:**  
  - **KeyBinding:** Registra la tecla **K** para alternar la activación del mod.
  - **Hilo de Algoritmo:** Ejecuta la secuencia principal que simula acciones de minería (clic izquierdo, movimiento hacia atrás y adelante).
  - **Hilo Anti-AFK:** Pausa la secuencia principal en intervalos aleatorios para evitar la detección de inactividad.
  - **Sincronización y Pausa:** Utiliza objetos de sincronización para manejar pausas seguras durante la ejecución del algoritmo.
  - **Mensajes de Depuración:** Muestra mensajes en pantalla para notificar eventos y estados del mod.

### `DebugHud.java`
- **Función Principal:**  
  Renderiza mensajes de depuración en el HUD del cliente.
- **Características Clave:**  
  - **Renderizado de Texto:** Utiliza el `HudRenderCallback` para dibujar mensajes en la pantalla.
  - **Efecto Fade-Out:** Los mensajes se desvanecen gradualmente antes de desaparecer.
  - **Método de Mensaje:** Permite actualizar y mostrar mensajes durante un tiempo específico en el HUD.

---

## Contribuciones

Si deseas contribuir a este proyecto, siéntete libre de enviar *pull requests* o reportar problemas en el repositorio.

---

## Licencia

Distribuido bajo [Indicar Licencia Aquí] (por ejemplo, MIT License).

---

¡Disfruta de tu minería automatizada y mantente activo en el juego!
