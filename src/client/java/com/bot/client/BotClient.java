package com.bot.client;

import com.bot.client.command.Command;
import com.bot.client.movement.MovementController;
import com.bot.client.render.TrajectoryRenderer;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;

/**
 * Client entrypoint: minimal Stage 1 movement control.
 * Behavior:
 * - Registers a client-side /go <x> <y> <z> command to set a movement target.
 * - Ticks the movement controller every client tick so the player can walk toward the active target.
 */
public class BotClient implements ClientModInitializer {
	private static final Logger LOGGER = LogUtils.getLogger();
	private final MovementController movementController = new MovementController();
	private final TrajectoryRenderer trajectoryRenderer = new TrajectoryRenderer(movementController);

	@Override
	public void onInitializeClient() {
		trajectoryRenderer.register();

		// Register the /go command
		Command.register(movementController);

		// Register a tick listener to update movement each client tick.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			try {
				movementController.tick();
			} catch (Throwable t) {
				// Prevent exceptions from crashing the client tick loop.
				LOGGER.error("Unhandled error in client tick", t);
			}
		});
	}
}
