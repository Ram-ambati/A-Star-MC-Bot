package com.bot.client.render;

import com.bot.client.movement.MovementController;
import com.bot.client.world.NavigationNode;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;

/**
 * Renders a navigation overlay for the active movement path.
 *
 * Stage 2 visualization:
 * - cyan line segments between block-centered navigation nodes
 * - yellow marker box at the current destination node
 * - optional path preview updates in real time as the active node sequence changes
 */
public class TrajectoryRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PATH_COLOR = 0xFF00FFFF;
    private static final int TARGET_COLOR = 0xFFFFFF00;
    private static final int NODE_COLOR = 0xFF66CCFF;
    private static final float PATH_LINE_WIDTH = 4.0F;
    private static final float MARKER_LINE_WIDTH = 2.0F;
    private static final float NODE_MARKER_LINE_WIDTH = 1.5F;

    private final MovementController movementController;
    private boolean disabledDueToRenderError;

    public TrajectoryRenderer(MovementController movementController) {
        this.movementController = movementController;
    }

    public void register() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(this::onBeforeDebugRender);
    }

    private void onBeforeDebugRender(WorldRenderContext context) {
        if (disabledDueToRenderError) {
            return;
        }

        try {
            renderTrajectory(context);
        } catch (Throwable t) {
            disabledDueToRenderError = true;
            LOGGER.error("Disabling trajectory overlay after render exception", t);
        }
    }

    private void renderTrajectory(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        List<NavigationNode> path = movementController.getActivePathSnapshot();
        if (path.isEmpty()) {
            return;
        }

        VertexConsumer consumer = context.consumers().getBuffer(RenderLayers.lines());
        Vec3d cameraPos = context.worldState().cameraRenderState.pos;

        Vec3d playerCenter = blockCenter(BlockPos.ofFloored(client.player.getX(), client.player.getY(), client.player.getZ()));
        Vec3d previous = toRelative(playerCenter, cameraPos);

        // If there is only one node, render a simple block-centered guide to it.
        if (path.size() == 1) {
            Vec3d targetCenter = toRelative(path.getFirst().center(), cameraPos);
            drawLine(consumer, (float) previous.x, (float) previous.y, (float) previous.z,
                    (float) targetCenter.x, (float) targetCenter.y, (float) targetCenter.z,
                    PATH_COLOR, PATH_LINE_WIDTH);
            drawNodeMarker(consumer, targetCenter, TARGET_COLOR, MARKER_LINE_WIDTH);
            return;
        }

        // Multi-node path: draw block-to-block segments.
        for (int i = 0; i < path.size(); i++) {
            NavigationNode node = path.get(i);
            Vec3d nodeCenter = toRelative(node.center(), cameraPos);

            if (i == 0) {
                drawLine(consumer,
                        (float) previous.x, (float) previous.y, (float) previous.z,
                        (float) nodeCenter.x, (float) nodeCenter.y, (float) nodeCenter.z,
                        PATH_COLOR, PATH_LINE_WIDTH);
            } else {
                Vec3d prevNodeCenter = toRelative(path.get(i - 1).center(), cameraPos);
                drawLine(consumer,
                        (float) prevNodeCenter.x, (float) prevNodeCenter.y, (float) prevNodeCenter.z,
                        (float) nodeCenter.x, (float) nodeCenter.y, (float) nodeCenter.z,
                        PATH_COLOR, PATH_LINE_WIDTH);
            }

            drawNodeMarker(consumer, nodeCenter, i == path.size() - 1 ? TARGET_COLOR : NODE_COLOR,
                    i == path.size() - 1 ? MARKER_LINE_WIDTH : NODE_MARKER_LINE_WIDTH);
        }
    }

    private static Vec3d toRelative(Vec3d worldPos, Vec3d cameraPos) {
        return worldPos.subtract(cameraPos);
    }

    private static Vec3d blockCenter(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private static void drawNodeMarker(VertexConsumer consumer, Vec3d center, int color, float lineWidth) {
        float minX = (float) center.x - 0.2F;
        float minY = (float) center.y - 0.2F;
        float minZ = (float) center.z - 0.2F;
        float maxX = (float) center.x + 0.2F;
        float maxY = (float) center.y + 0.2F;
        float maxZ = (float) center.z + 0.2F;

        drawLine(consumer, minX, minY, minZ, maxX, minY, minZ, color, lineWidth);
        drawLine(consumer, maxX, minY, minZ, maxX, minY, maxZ, color, lineWidth);
        drawLine(consumer, maxX, minY, maxZ, minX, minY, maxZ, color, lineWidth);
        drawLine(consumer, minX, minY, maxZ, minX, minY, minZ, color, lineWidth);

        drawLine(consumer, minX, maxY, minZ, maxX, maxY, minZ, color, lineWidth);
        drawLine(consumer, maxX, maxY, minZ, maxX, maxY, maxZ, color, lineWidth);
        drawLine(consumer, maxX, maxY, maxZ, minX, maxY, maxZ, color, lineWidth);
        drawLine(consumer, minX, maxY, maxZ, minX, maxY, minZ, color, lineWidth);

        drawLine(consumer, minX, minY, minZ, minX, maxY, minZ, color, lineWidth);
        drawLine(consumer, maxX, minY, minZ, maxX, maxY, minZ, color, lineWidth);
        drawLine(consumer, maxX, minY, maxZ, maxX, maxY, maxZ, color, lineWidth);
        drawLine(consumer, minX, minY, maxZ, minX, maxY, maxZ, color, lineWidth);
    }

    private static void drawLine(VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, int color, float lineWidth) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        float nx = 0.0F;
        float ny = 1.0F;
        float nz = 0.0F;

        if (length > 0.0001F) {
            nx = dx / length;
            ny = dy / length;
            nz = dz / length;
        }

        consumer.vertex(x1, y1, z1)
                .color(color)
                .normal(nx, ny, nz)
                .lineWidth(lineWidth);

        consumer.vertex(x2, y2, z2)
                .color(color)
                .normal(nx, ny, nz)
                .lineWidth(lineWidth);
    }
}
