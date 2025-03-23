package ru.nern.anglewrap.managers;

import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import ru.nern.anglewrap.model.WarpPoint;

import static net.minecraft.client.render.RenderPhase.*;
import static ru.nern.anglewrap.AngleWarp.*;

public class RenderManager {
    private static final Identifier PROGRESSBAR_LAYER = Identifier.of("anglewarp", "progressbar_layer");

    private static final int BAR_WIDTH = 20; // Width of the progress bar
    private static final int BAR_HEIGHT = 2; // Height of the progress bar

    private static final RenderLayer WARP_POINT_MARKER_LAYER = RenderLayer.of(
            "anglewarp:warp_point_marker",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.TRIANGLE_STRIP,
            1536,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(POSITION_COLOR_PROGRAM)
                    .transparency(NO_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .depthTest(ALWAYS_DEPTH_TEST)
                    .writeMaskState(COLOR_MASK)
                    .build(false)
    );


    public static void init() {
        WorldRenderEvents.LAST.register(context -> {
            if(isWarpingKeyPressed) {
                int textBackgroundColor = (int)(MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F) * 255.0F) << 24;
                VertexConsumerProvider.Immediate provider = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
                MatrixStack stack = context.matrixStack();

                for(WarpPoint point : WarpPointManager.points) {
                    if(point.hidden) continue;

                    renderWarpPoint(point, stack, provider, textBackgroundColor);
                }

            }
        });
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer -> layeredDrawer.attachLayerBefore(IdentifiedLayer.CROSSHAIR, PROGRESSBAR_LAYER, RenderManager::renderProgressBar));
    }


    private static void renderProgressBar(DrawContext drawContext, RenderTickCounter counter) {
        if(config.rendering.renderProgressBar && lastWarped != null && warpProgress > 0 && warpProgress <= lastWarped.warpTicks) {
            int screenWidth = drawContext.getScaledWindowWidth();
            int screenHeight = drawContext.getScaledWindowHeight();
            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2 + 10; // Offset below the crosshair

            int filledWidth = (warpProgress * BAR_WIDTH) / lastWarped.warpTicks;

            drawContext.getMatrices().push();

            // Background (gray)
            drawContext.fill(centerX - BAR_WIDTH / 2, centerY, centerX + BAR_WIDTH / 2, centerY + BAR_HEIGHT, 0xFF555555);

            // Progress (green)
            drawContext.fill(centerX - BAR_WIDTH / 2, centerY, centerX - BAR_WIDTH / 2 + filledWidth, centerY + BAR_HEIGHT, config.rendering.useMarkerColorForProgressBar ? lastWarped.color : 0xFF00FF00);

            drawContext.getMatrices().pop();
        }
    }



    private static void renderWarpPoint(WarpPoint point, MatrixStack matrices, VertexConsumerProvider.Immediate provider, int textBackgroundColor) {
        matrices.push();

        double yawRad = Math.toRadians(point.rotation.y);
        double pitchRad = Math.toRadians(point.rotation.x);
        double cosPitch = Math.cos(pitchRad);

        double dx = -Math.sin(yawRad) * cosPitch;
        double dy = -Math.sin(pitchRad);
        double dz = Math.cos(yawRad) * cosPitch;

        float warpPointDistance = config.rendering.warpPointDistance;
        matrices.translate(dx * warpPointDistance, dy * warpPointDistance, dz * warpPointDistance);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-point.rotation.y));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(point.rotation.x));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));

        // Rendering warp point diamond
        VertexConsumer bufferBuilder = provider.getBuffer(WARP_POINT_MARKER_LAYER);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        bufferBuilder.vertex(matrix4f, 0, -10, 2.5f).color(point.color);  // Bottom vertex
        bufferBuilder.vertex(matrix4f, -7.5f, 0, 2.5f).color(point.color); // Left vertex
        bufferBuilder.vertex(matrix4f, 7.5f, 0, 2.5f).color(point.color);  // Right vertex
        bufferBuilder.vertex(matrix4f, 0, 10, 2.5f).color(point.color);    // Top vertex

        matrices.translate(0, -25, 0);

        drawText(MinecraftClient.getInstance(), point.getDisplayName(), provider, matrix4f, textBackgroundColor);

        provider.draw();

        matrices.pop();
    }

    public static void drawText(MinecraftClient client, String text, VertexConsumerProvider provider, Matrix4f positionMatrix, int textBackgroundColor) {
        TextRenderer textRenderer = client.textRenderer;

        textRenderer.draw(
                Text.of(text),
                -textRenderer.getWidth(text) / 2.0F,
                0,
                0xFFFFFF,
                false,
                positionMatrix,
                provider,
                TextRenderer.TextLayerType.SEE_THROUGH,
                textBackgroundColor,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
        );
    }
}
