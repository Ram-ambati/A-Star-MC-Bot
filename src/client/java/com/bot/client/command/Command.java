package com.bot.client.command;

import com.bot.client.movement.MovementController;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;

/**
 * Handles client-side /go command for setting movement targets.
 */
public class Command {

    public static void register(MovementController movementController) {
        LiteralArgumentBuilder<FabricClientCommandSource> goCommand = ClientCommandManager.literal("go")
                .then(ClientCommandManager.argument("x", doubleArg())
                        .then(ClientCommandManager.argument("y", doubleArg())
                                .then(ClientCommandManager.argument("z", doubleArg())
                                        .executes(ctx -> {
                                            double x = getDouble(ctx, "x");
                                            double y = getDouble(ctx, "y");
                                            double z = getDouble(ctx, "z");

                                            movementController.setTarget(x, y, z);

                                            MinecraftClient client = MinecraftClient.getInstance();
                                            if (client != null && client.inGameHud != null) {
                                                String msg = String.format("Going to %.1f %.1f %.1f", x, y, z);
                                                client.inGameHud.getChatHud().addMessage(Text.literal(msg));
                                            }
                                            return 1;
                                        }))));

        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess) -> dispatcher.register(goCommand));
    }
}

