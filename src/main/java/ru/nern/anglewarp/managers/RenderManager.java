package ru.nern.anglewarp.managers;

import com.mojang.blaze3d.systems.RenderSystem;
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
import ru.nern.anglewarp.model.WarpPoint;
import ru.nern.anglewarp.model.WarpPointShape;

import static net.minecraft.client.render.RenderPhase.*;
import static ru.nern.anglewarp.AngleWarp.*;

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
            false,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(POSITION_COLOR_PROGRAM)
                    .transparency(NO_TRANSPARENCY)
                    //.cull(DISABLE_CULLING)
                    .depthTest(ALWAYS_DEPTH_TEST)
                    .writeMaskState(COLOR_MASK)
                    .build(false)
    );


    public static void init() {
        WorldRenderEvents.LAST.register(context -> {
            VertexConsumerProvider.Immediate provider = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
            //VertexConsumerProvider.I provider = context.consumers();
            if(isOverlayEnabled) {
                int textBackgroundColor = (int)(MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F) * 255.0F) << 24;

                MatrixStack stack = context.matrixStack();

                for(WarpPoint point : warpPointManager.points) {
                    if(point.hidden) continue;
                    renderWarpPoint(point, stack, provider, textBackgroundColor);
                }
            } else if(currentlySnapped != null && !currentlySnapped.hidden) {
                int textBackgroundColor = (int)(MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F) * 255.0F) << 24;
                MatrixStack stack = context.matrixStack();
                renderWarpPoint(currentlySnapped, stack, provider, textBackgroundColor);
            }
        });
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer -> layeredDrawer.attachLayerBefore(IdentifiedLayer.CROSSHAIR, PROGRESSBAR_LAYER, RenderManager::renderProgressBar));
    }


    private static void renderProgressBar(DrawContext drawContext, RenderTickCounter counter) {
        if(currentlySnapped != null && config.rendering.renderProgressBar && activationProgress > 0 && activationProgress <= currentlySnapped.warpTicks) {
            int screenWidth = drawContext.getScaledWindowWidth();
            int screenHeight = drawContext.getScaledWindowHeight();
            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2 + 10; // Offset below the crosshair

            int filledWidth = (activationProgress * BAR_WIDTH) / currentlySnapped.warpTicks;

            drawContext.getMatrices().push();

            // Background (gray)
            drawContext.fill(centerX - BAR_WIDTH / 2, centerY, centerX + BAR_WIDTH / 2, centerY + BAR_HEIGHT, 0xFF555555);

            // Progress (green)
            drawContext.fill(centerX - BAR_WIDTH / 2, centerY, centerX - BAR_WIDTH / 2 + filledWidth, centerY + BAR_HEIGHT, config.rendering.useMarkerColorForProgressBar ? currentlySnapped.color : 0xFF00FF00);

            drawContext.getMatrices().pop();
        }
    }



    private static void renderWarpPoint(WarpPoint point, MatrixStack matrices, VertexConsumerProvider.Immediate provider, int textBackgroundColor) {

        RenderSystem.disableDepthTest(); // So, for some reason litematica messes up with the depth testing and this is needed, despite the depth testing being already disabled in the render layer

        matrices.push();

        double yawRad = Math.toRadians(point.rotation.y);
        double pitchRad = Math.toRadians(point.rotation.x);
        double cosPitch = Math.cos(pitchRad);

        double dx = -Math.sin(yawRad) * cosPitch;
        double dy = -Math.sin(pitchRad);
        double dz = Math.cos(yawRad) * cosPitch;

        float distance = config.rendering.warpPointDistance;
        matrices.translate(dx * distance, dy * distance, dz * distance);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-point.rotation.y));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(point.rotation.x));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));

        VertexConsumer bufferBuilder = provider.getBuffer(WARP_POINT_MARKER_LAYER);



        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        renderShape(point.shape, point.color, matrix4f, bufferBuilder);
        matrices.translate(0, -25, 0);
        drawText(MinecraftClient.getInstance(), point.getDisplayName(), provider, matrix4f, textBackgroundColor);


        provider.draw();

        matrices.pop();
        RenderSystem.enableDepthTest();
    }

    private static void renderShape(WarpPointShape shape, int color, Matrix4f matrix4f, VertexConsumer bufferBuilder) {
        switch (shape) {
            case SQUARE -> {
                bufferBuilder.vertex(matrix4f, -7.5f, -7.5f, 2.5f).color(color);  // Bottom-left
                bufferBuilder.vertex(matrix4f, 7.5f, -7.5f, 2.5f).color(color);   // Bottom-right
                bufferBuilder.vertex(matrix4f, -7.5f, 7.5f, 2.5f).color(color);   // Top-left
                bufferBuilder.vertex(matrix4f, 7.5f, 7.5f, 2.5f).color(color);    // Top-right
            }
            case TRIANGLE -> {
                bufferBuilder.vertex(matrix4f, 0, -10, 2.5f).color(color);
                bufferBuilder.vertex(matrix4f, -8.66f, 5, 2.5f).color(color);
                bufferBuilder.vertex(matrix4f, 8.66f, 5, 2.5f).color(color);
            }
            case null, default -> { // Diamond
                bufferBuilder.vertex(matrix4f, 0, -10, 2.5f).color(color);
                bufferBuilder.vertex(matrix4f, -7.5f, 0, 2.5f).color(color);
                bufferBuilder.vertex(matrix4f, 7.5f, 0, 2.5f).color(color);
                bufferBuilder.vertex(matrix4f, 0, 10, 2.5f).color(color);
            }
        }
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
