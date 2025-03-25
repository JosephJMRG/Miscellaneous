/**
 * @file MiningmodClient.java
 * @brief Mod para la minería AFK en Minecraft utilizando Fabric.
 */
package net.joseph.miningmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * @class MiningmodClient
 * @brief Implementación del mod de minería AFK.
 *
 * Esta clase se encarga de registrar la keybinding que alterna la activación del algoritmo
 * de minería AFK y de ejecutar dicha secuencia. Además, incorpora un hilo anti-AFK que pausa
 * la acción para evitar que el jugador sea desconectado por inactividad.
 */
public class MiningmodClient implements ClientModInitializer {
    // Constantes de configuración
    private static final double MIN_SEQUENCE_TIMEOUT = 15.0;
    private static final double MAX_SEQUENCE_TIMEOUT = 23.0;
    private static final double MIN_KEY_DURATION = 0.3;
    private static final double MAX_KEY_DURATION = 0.8;

    private static KeyBinding toggleKey;
    private volatile boolean active = false;
    private Thread algorithmThread;
    private Thread antiAFKThread;

    // Variables de sincronización para pausar la secuencia principal
    private final Object pauseLock = new Object();
    private volatile boolean paused = false;

    /**
     * @brief Inicializa el mod en el cliente.
     *
     * Registra la keybinding para la tecla "K", inicializa el DebugHud para mostrar mensajes
     * en pantalla y configura el evento de tick para alternar el algoritmo de minería y vigilar
     * el estado del jugador.
     */
    @Override
    public void onInitializeClient() {
        // Registrar la keybinding para la tecla K
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.antiafkmod.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.antiafkmod"));

        // Inicializar DebugHud para renderizar mensajes en el HUD
        DebugHud.init();

        // Variable para espaciar la comprobación (cada 500 ms)
        final long[] lastCheckTime = {0};

        // Registrar eventos de tick del cliente
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Alterna el algoritmo al presionar la tecla
            while (toggleKey.wasPressed()) {
                active = !active;
                if (active) {
                    startAlgorithm();
                } else {
                    stopAlgorithm();
                }
            }
            // Cada 500 ms se comprueba la vida del jugador
            long now = System.currentTimeMillis();
            if (now - lastCheckTime[0] >= 500) {
                lastCheckTime[0] = now;
                if (client.player != null && !client.player.isAlive() && active) {
                    debug("Player had died, AFK Mining Disabled.");
                    active = false;
                    stopAlgorithm();
                }
            }
        });
    }

    /**
     * @brief Muestra un mensaje en el HUD durante 5 segundos.
     *
     * @param message Mensaje a mostrar.
     */
    private void debug(String message) {
        DebugHud.showDebugMessage(message, 5000);
    }

    /**
     * @brief Espera en pequeños intervalos si el algoritmo está pausado.
     *
     * Este método bloquea la ejecución del hilo hasta que la pausa se desactive, liberando
     * las teclas de movimiento en caso de que se encuentren presionadas.
     *
     * @param client Instancia actual de MinecraftClient.
     * @throws InterruptedException Si se interrumpe el hilo.
     */
    private void waitIfPaused(MinecraftClient client) throws InterruptedException {
        synchronized (pauseLock) {
            while (paused) {
                // Liberar teclas en caso de que estén presionadas
                client.options.attackKey.setPressed(false);
                client.options.forwardKey.setPressed(false);
                client.options.backKey.setPressed(false);
                pauseLock.wait();
            }
        }
    }

    /**
     * @brief Duerme en incrementos de 100 ms, permitiendo salir si se activa la pausa.
     *
     * Permite que el hilo "duerma" de forma segura, revisando periódicamente si se ha activado
     * la pausa para interrumpir el sueño.
     *
     * @param millis Tiempo total a dormir en milisegundos.
     * @param client Instancia actual de MinecraftClient.
     * @throws InterruptedException Si se interrumpe el hilo.
     */
    private void safeSleep(long millis, MinecraftClient client) throws InterruptedException {
        long slept = 0;
        while (slept < millis) {
            synchronized (pauseLock) {
                if (paused) {
                    // Si se detecta pausa, salir inmediatamente sin esperar el resto.
                    return;
                }
            }
            long sleepTime = Math.min(100, millis - slept);
            Thread.sleep(sleepTime);
            slept += sleepTime;
        }
    }

    /**
     * @brief Espera cancelable entre secuencias, permitiendo cancelación si se pausa.
     *
     * Este método espera el tiempo especificado en milisegundos, verificando periódicamente
     * si se ha activado la pausa para interrumpir la espera.
     *
     * @param millis Tiempo a esperar en milisegundos.
     * @param client Instancia actual de MinecraftClient.
     * @throws InterruptedException Si se interrumpe el hilo.
     */
    private void sleepSequenceWait(long millis, MinecraftClient client) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < millis) {
            synchronized (pauseLock) {
                if (paused) {
                    // Si se activa la pausa, se cancela la espera actual
                    return;
                }
            }
            long interval = Math.min(100, millis - (System.currentTimeMillis() - start));
            Thread.sleep(interval);
        }
    }

    /**
     * @brief Inicia el hilo del algoritmo de minería AFK.
     *
     * Lanza un hilo que ejecuta la secuencia principal del algoritmo. Incluye:
     * - Un countdown inicial de 7 segundos.
     * - El inicio de un hilo anti-AFK que pausa y reanuda la secuencia para evitar inactividad.
     *
     * Los pasos clave son:
     *  1. Mostrar mensaje de activación y esperar 1 segundo.
     *  2. Realizar un countdown de 7 segundos, informando al usuario.
     *  3. Iniciar el hilo anti-AFK que gestiona pausas aleatorias.
     *  4. Ejecutar la secuencia principal indefinidamente.
     */
    private void startAlgorithm() {
        if (algorithmThread != null && algorithmThread.isAlive())
            return;

        algorithmThread = new Thread(() -> {
            try {
                debug("AFK Mining Enabled.");
                Thread.sleep(1000); // Espera de 1 segundo antes de comenzar

                // Countdown de 7 segundos antes de iniciar la secuencia principal
                int countdown = 7;
                while (countdown > 0 && !Thread.currentThread().isInterrupted()) {
                    debug("La secuencia principal comenzará en " + countdown + " segundos");
                    Thread.sleep(1000);
                    countdown--;
                }

                // Iniciar el hilo anti-AFK (que nunca se detiene)
                antiAFKThread = new Thread(() -> {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            // Espera aleatoria entre 25 y 40 segundos antes de pausar
                            double waitBeforePause = 25 + Math.random() * (40 - 30); // 25-40 s
                            debug("[AntiAFK] Esperando " + String.format("%.2f", waitBeforePause) + " segundos antes de pausar el algoritmo");
                            Thread.sleep((long)(waitBeforePause * 1000));

                            // Pausar el algoritmo principal
                            synchronized (pauseLock) {
                                paused = true;
                            }

                            // Pausa del algoritmo principal durante 4 a 7 segundos
                            double pauseDuration = 4 + Math.random() * 3; // 4-7 s
                            debug("[AntiAFK] Pausa del algoritmo durante " + String.format("%.2f", pauseDuration) + " segundos para evitar AFK");
                            Thread.sleep((long)(pauseDuration * 1000));

                            // Reanudar el algoritmo principal
                            synchronized (pauseLock) {
                                paused = false;
                                pauseLock.notifyAll();
                            }
                            // Pequeña pausa extra para estabilizar
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                antiAFKThread.setDaemon(true);
                antiAFKThread.start();

                // Ejecutar la secuencia principal indefinidamente
                MinecraftClient client = MinecraftClient.getInstance();
                while (!Thread.currentThread().isInterrupted()) {
                    waitIfPaused(client);
                    runAFKSequence();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        algorithmThread.start();
    }

    /**
     * @brief Detiene el algoritmo de minería AFK y libera recursos.
     *
     * Este método:
     *  - Muestra un mensaje de desactivación.
     *  - Libera la tecla de ataque.
     *  - Interrumpe y anula los hilos del algoritmo y del anti-AFK.
     */
    private void stopAlgorithm() {
        debug("AFK Mining Disabled.");
        MinecraftClient client = MinecraftClient.getInstance();
        // Liberar la tecla de ataque
        if (client.options.attackKey != null) {
            client.options.attackKey.setPressed(false);
        }
        if (algorithmThread != null && algorithmThread.isAlive()) {
            algorithmThread.interrupt();
            algorithmThread = null;
        }
        if (antiAFKThread != null && antiAFKThread.isAlive()) {
            antiAFKThread.interrupt();
            antiAFKThread = null;
        }
    }

    /**
     * @brief Ejecuta la secuencia principal de acciones AFK.
     *
     * La secuencia simula acciones de minería:
     *  1. Simula el click izquierdo (ataque) para iniciar la acción.
     *  2. Presiona la tecla "s" (mover hacia atrás) durante un tiempo aleatorio.
     *  3. Presiona la tecla "w" (mover hacia adelante) durante un tiempo aleatorio.
     *  4. Espera un intervalo aleatorio entre secuencias.
     *  5. Suelta el click izquierdo para finalizar la acción.
     *
     * @throws InterruptedException Si se interrumpe el hilo durante la secuencia.
     */
    private void runAFKSequence() throws InterruptedException {
        MinecraftClient client = MinecraftClient.getInstance();

        // Simular click izquierdo: Inicia la acción de ataque
        waitIfPaused(client);
        debug("Manteniendo click izquierdo");
        client.options.attackKey.setPressed(true);
        Thread.sleep(500);

        // Simular presionar "s" (tecla atrás) por una duración aleatoria
        waitIfPaused(client);
        double durationS = MIN_KEY_DURATION + Math.random() * (MAX_KEY_DURATION - MIN_KEY_DURATION);
        debug("Presionando 's' durante " + String.format("%.2f", durationS) + " segundos");
        client.options.backKey.setPressed(true);
        safeSleep((long)(durationS * 1000), client);
        client.options.backKey.setPressed(false);
        Thread.sleep(500);

        // Simular presionar "w" (tecla adelante) por una duración aleatoria
        waitIfPaused(client);
        double durationW = MIN_KEY_DURATION + Math.random() * (MAX_KEY_DURATION - MIN_KEY_DURATION);
        debug("Presionando 'w' durante " + String.format("%.2f", durationW) + " segundos");
        client.options.forwardKey.setPressed(true);
        safeSleep((long)(durationW * 1000), client);
        client.options.forwardKey.setPressed(false);
        Thread.sleep(500);

        // Espera entre secuencias (15 a 23 segundos) usando el método cancelable
        waitIfPaused(client);
        double sequenceTimeout = MIN_SEQUENCE_TIMEOUT + Math.random() * (MAX_SEQUENCE_TIMEOUT - MIN_SEQUENCE_TIMEOUT);
        debug("Esperando " + String.format("%.2f", sequenceTimeout) + " segundos antes de repetir la secuencia");
        sleepSequenceWait((long)(sequenceTimeout * 1000), client);

        // Soltar click izquierdo para finalizar la acción de ataque
        waitIfPaused(client);
        debug("Soltando click izquierdo");
        client.options.attackKey.setPressed(false);
        Thread.sleep(500);
    }
}
