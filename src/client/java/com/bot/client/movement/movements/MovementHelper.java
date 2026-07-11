package com.bot.client.movement.movements;

import com.bot.client.world.NavigationNode;

public class MovementHelper {
    public static IMovement createMovement(NavigationNode startNode, NavigationNode endNode) {
        if (startNode == null || endNode == null) return null;

        int dx = endNode.position().getX() - startNode.position().getX();
        int dy = endNode.position().getY() - startNode.position().getY();
        int dz = endNode.position().getZ() - startNode.position().getZ();

        if (dy > 0) {
            return new MovementAscend(startNode, endNode);
        } else if (dy < 0) {
            return new MovementDescend(startNode, endNode);
        } else {
            if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
                return new MovementDiagonal(startNode, endNode);
            }
            return new MovementTraverse(startNode, endNode);
        }
    }
}
