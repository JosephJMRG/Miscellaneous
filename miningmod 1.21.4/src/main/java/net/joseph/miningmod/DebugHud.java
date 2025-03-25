/**
 * @file DebugHud.java
 * @brief Renderiza mensajes de depuración en el HUD del cliente de Minecraft.
 *
 * Esta clase se encarga de mostrar mensajes temporales en el HUD del cliente, con un efecto de fade-out.
 */
package net.joseph.miningmod;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

/**
 * @class DebugHud
 * @brief Clase para renderizar mensajes de depuración en el HUD.
 *
 * Permite mostrar mensajes de texto de depuración en pantalla durante un tiempo determinado,
 * aplicando un efecto de desvanecimiento (fade-out) antes de que desaparezcan.
 */
public class DebugHud {
    // Mensaje actual y tiempo de expiración (en milisegundos)
    private static String currentMessage = "";
    private static long expirationTime = 0;
    // Duración del efecto fade-out (por ejemplo, 1000 ms = 1 segundo)
    private static final long FADE_OUT_DURATION = 1000;

    /**
     * @brief Inicializa el HUD de depuración.
     *
     * Registra un callback para renderizar mensajes en el HUD usando HudRenderCallback.EVENT.
     * Durante cada tick, se verifica si hay un mensaje activo y, de ser así, se renderiza en el HUD
     * con un efecto de fade-out basado en el tiempo restante.
     */
    @SuppressWarnings("deprecation")
    public static void init() {
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
            // Obtener la instancia actual del cliente de Minecraft
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null)
                return; // No renderizar si el jugador no está presente

            // Obtener el tiempo actual en milisegundos
            long currentTime = System.currentTimeMillis();

            // Verificar si hay un mensaje activo y que aún no haya expirado
            if (!currentMessage.isEmpty() && currentTime < expirationTime) {
                // Obtener dimensiones de la ventana
                int screenWidth = client.getWindow().getScaledWidth();
                int screenHeight = client.getWindow().getScaledHeight();
                // Calcular el ancho del mensaje
                int textWidth = client.textRenderer.getWidth(currentMessage);
                // Posición centrada horizontalmente, justo encima de la hotbar
                int x = (screenWidth - textWidth) / 2;
                int y = screenHeight - 59;

                // Calcular el tiempo restante para el fade-out
                long remainingTime = expirationTime - currentTime;
                int alpha = 255;
                if (remainingTime < FADE_OUT_DURATION) {
                    // El valor alfa disminuye proporcionalmente a medida que se agota el tiempo
                    alpha = (int)(255 * ((double)remainingTime / FADE_OUT_DURATION));
                }
                // Combinar el canal alfa con el color blanco (0xFFFFFF)
                int color = (alpha << 24) | 0xFFFFFF;

                // Renderizar el mensaje en el HUD
                client.textRenderer.draw(currentMessage,                                        // Texto a renderizar.
                                         (float)x,                                              // Posición X (horizontal) donde se inicia el renderizado.
                                         (float)y,                                              // Posición Y (vertical) donde se inicia el renderizado.
                                         color,                                                 // Color del texto, incluyendo canal alfa para transparencia.
                                         false,                                                 // Sin sombra: indica que no se dibuja sombra detrás del texto.
                                         matrices.getMatrices().peek().getPositionMatrix(),     // Matriz de transformación para la posición y escala del texto.
                                         client.getBufferBuilders().getEntityVertexConsumers(), // Buffer de vértices para el renderizado del texto.
                                         TextRenderer.TextLayerType.NORMAL,                     // Tipo de capa del texto (NORMAL para renderizado estándar).
                                         0,                                                     // Color de fondo (0 significa sin fondo).
                                         15728640                                               // Nivel de iluminación para el renderizado del texto.
                );
            }
        });
    }

    /**
     * @brief Muestra un mensaje en el HUD durante un período especificado.
     *
     * Actualiza el mensaje actual y establece el tiempo de expiración en base a la duración
     * proporcionada, de forma que el mensaje se muestre y luego se desvanezca.
     *
     * @param message Mensaje a mostrar en el HUD.
     * @param durationMillis Duración en milisegundos durante la cual el mensaje será visible.
     */
    public static void showDebugMessage(String message, long durationMillis) {
        currentMessage = message;
        expirationTime = System.currentTimeMillis() + durationMillis;
    }
}
