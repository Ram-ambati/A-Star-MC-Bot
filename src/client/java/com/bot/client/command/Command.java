package com.bot.client.command;

import com.bot.client.movement.MovementController;
import com.bot.client.pathfinding.LocalRoutePlanner;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

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

                                            MinecraftClient client = MinecraftClient.getInstance();
                                            ClientWorld world = client.world;
                                            if (world == null || client.player == null) {
                                                return 0;
                                            }

                                             BlockPos start = client.player.getBlockPos();
                                             BlockPos goal = BlockPos.ofFloored(x, y, z);
                                             com.bot.client.pathfinding.PathfinderState state = LocalRoutePlanner.beginRoute(world, start, goal, 1.5D);
                                             if (state != null) {
                                                 movementController.startPathfinding(state);
                                                 if (client.inGameHud != null) {
                                                     client.inGameHud.getChatHud().addMessage(Text.literal("§eCalculating path to " + x + " " + y + " " + z + "..."));
                                                 }
                                             } else {
                                                 if (client.inGameHud != null) {
                                                     client.inGameHud.getChatHud().addMessage(Text.literal("§cFailed to initialize pathfinder."));
                                                 }
                                             }
                                             return 1;
                                        }))));

        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess) -> dispatcher.register(goCommand));
    }
}
