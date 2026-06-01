# Bot

Bot is a Java 21, Gradle-based Fabric Minecraft mod focused on client-side bot and navigation behavior.

## Features

- Client-side bot behavior for automated movement tasks.
- Navigation/pathing-oriented mod logic for in-game traversal.
- Fabric-based mod structure suitable for iterative gameplay experimentation.

## Requirements

- **Java 21**
- **Minecraft/Fabric versions** defined in `gradle.properties` (for example, `minecraft_version`, loader/API-related properties)
- A **Fabric-compatible development setup** (IDE + Gradle + Fabric Loom workflow)

## Setup

1. Clone this repository.
2. Open the project root in your IDE.
3. Import/sync the Gradle project so Loom/Fabric dependencies resolve.

## Build

```bash
./gradlew build
```

```cmd
gradlew.bat build
```

## Run Client

```bash
./gradlew runClient
```

```cmd
gradlew.bat runClient
```

## Project Structure

- `src/main` - shared/common mod source set.
- `src/client` - client-only bot/navigation code.
- `src/main/resources` - mod resources (including metadata/assets).
- `build.gradle` - Gradle build configuration (Loom/Fabric setup).
- `gradle.properties` - central version/config properties.
- `run/` - local runtime directory used by Loom run tasks.

## Development Notes

- `build/`, `.gradle/`, and `run/` are generated/local directories.
- These directories should remain ignored by Git in normal development workflows.
- Use the included Gradle wrapper scripts (`./gradlew` or `gradlew.bat`) for consistent builds/runs.

## License

This project is licensed under the terms in the [LICENSE](LICENSE) file.
