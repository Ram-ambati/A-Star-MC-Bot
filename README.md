# A* Minecraft Bot Navigation (Fabric 1.21.1)

![Java 21](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Minecraft 1.21.11](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen?style=flat-square)
![Fabric 0.19.2](https://img.shields.io/badge/Fabric-0.19.2-lightgrey?style=flat-square)
![License MIT](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

A Java-based, fully autonomous client-side navigation bot built for Minecraft 1.21.11 using the Fabric API. This project implements a robust A* (A-Star) pathfinding engine, dynamic real-time movement execution, and live 3D trajectory rendering directly into the game world.

## 🚀 Core Features

### 1. Advanced A* Pathfinding Engine
The core of the bot's navigation relies on a highly optimized, weighted A* algorithm implemented in the `LocalRoutePlanner`. 
- **Dynamic Expansion:** Can evaluate thousands of nodes per tick without freezing the client thread.
- **Weighted Heuristics:** Uses a weighted heuristic (default 1.5x) to heavily favor goal-directed exploration, ensuring extremely fast route calculation for long distances while still avoiding obstacles.
- **Terrain Analysis:** Integrates with a custom `BlockAnalyzer` that accurately assesses bounding boxes, determines if blocks are safe to stand on, and safely navigates or avoids fluid states (like water and lava).
- **Movement Capabilities:** Generates valid neighbors taking into account walking, jumping (up to 1 block), and safely dropping down.

### 2. Dynamic Movement Controller
The `MovementController` is responsible for turning planned routes into smooth player movement.
- **Vanilla Speeds:** Perfectly matched speeds to Vanilla Minecraft (Walk: 4.317 blocks/s, Sprint: ~5.61 blocks/s) and seamlessly checks the player's hunger bar (Food Level > 6) before attempting to sprint.
- **Seamless Partial Paths:** If a target is incredibly far away, the bot will generate a "partial path" to get as close as possible. Upon reaching the end of a partial path, the controller dynamically triggers a seamless route recalculation until the true final goal is achieved.
- **Fast-Forwarded Background Paths:** The bot actively calculates new paths in the background while still walking along the old path. When a new path is finalized, the bot instantly fast-forwards the route to match its current physical location, avoiding any backtracking stutter.

### 3. Real-Time 3D Trajectory Rendering
Visualizing what the bot is "thinking" is crucial for development and debugging. The `TrajectoryRenderer` hooks natively into Fabric's `AFTER_ENTITIES` rendering phase to provide:
- **Active Path Visualization:** A bright, glowing cyan line connecting the centers of the blocks that make up the final path.
- **Target Indicators:** A yellow marker box highlighting the final destination node.
- **Algorithm State Rendering:** Optionally visualizes the algorithm's "Open Set" (green) and "Closed Set" (red) in real-time as the pathfinder explores the world, offering incredible insight into how the algorithm traverses terrain.

### 4. Simple Command Interface
You can interact with the bot instantly using the registered client-side commands:
- `/go <x> <y> <z>`: Immediately commands the bot to start calculating and walking to the specified coordinates.

---

## 🛡️ Edge Cases & Error Recovery
The bot is heavily fault-tolerant and is designed to gracefully handle unexpected situations:

- **What happens if a block is placed in front of the bot?**
  If the bot's calculated path is suddenly blocked (or the terrain changes dynamically), the bot will realize it cannot reach its immediate next node. It marks the node as `UNREACHABLE`, instantly halts its movement, and silently triggers a background recalculation to route around the new obstacle.
- **What happens if the bot gets physically stuck?**
  If the bot gets stuck on a corner or entity and fails to progress to the next node within 60 ticks (~3 seconds), its internal watchdog timer fires. The bot will automatically wipe its current route, halt, and perform a full path recalculation to get unstuck.
- **What happens if the destination is literally impossible to reach?**
  If the bot calculates and recalculates but determines there is fundamentally no valid path to the destination (or exhausts its 3 consecutive recalculation attempts), it will gracefully abort, clear its state, and post a `§cPath aborted! Could not reach destination.` message in chat to prevent spamming server resources or hanging indefinitely.
- **What happens when travelling extremely long distances?**
  When travelling far, the bot periodically tracks its distance. Every 30 blocks traveled, it seamlessly initiates a background path calculation to optimize its current trajectory, ensuring it's always taking the most up-to-date route without having to pause or stop moving.

---

## 🛠️ Project Architecture

The bot's codebase is logically separated into highly modular components within `src/client/java/com/bot/client/`:

* `command/` - Handles the parsing and registration of the `/go` command.
* `movement/` - Contains the `MovementController` and distinct implementations for various types of movement (e.g., diagonal movement, straight movement).
* `pathfinding/` - Houses the `LocalRoutePlanner` and `PathfinderState`, representing the brains of the A* algorithm.
* `render/` - Contains the `TrajectoryRenderer` and its OpenGL/VertexConsumer matrix math.
* `world/` - Includes `BlockAnalyzer`, `NeighborGenerator`, and `NavigationNode` which interface directly with Minecraft's world data to determine block safety and reachable positions.

---

## 💻 Requirements & Setup

### Requirements
- **Java 21**
- **Minecraft 1.21.1**
- A Fabric-compatible IDE setup (IntelliJ IDEA or Eclipse)

### Getting Started
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Ram-ambati/A-Star-MC-Bot.git
   ```
2. **Open the project** in your IDE. Gradle will automatically sync and download the required Loom toolchain and Fabric APIs.

### Building
Compile a production-ready JAR file into the `build/libs/` directory:
```bash
# Windows
gradlew.bat build

# macOS / Linux
./gradlew build
```

### Running the Client
Launch a test client directly from the codebase for development:
```bash
# Windows
gradlew.bat runClient

# macOS / Linux
./gradlew runClient
```

---

## 📜 License
This project is licensed under the terms provided in the [LICENSE](LICENSE) file.
