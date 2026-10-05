# Recipe Graph Viewer (RGV)

Recipe Graph Viewer (RGV) is a lightweight, modular recipe viewer, crafting tree calculator, and item index for Minecraft Forge.

## Features

- **Cross-Version Architecture**: Core business logic, recipe graph solver, search indexing, and widget rendering reside in `:core` with zero Minecraft runtime dependencies. Subprojects bridge Forge 1.7.10 and Forge 1.12.2 through the unified `RgvPlatform` API.
- **Client-Side Recipe Transfer**: Crafting recipe auto-fill (`+` button) works on vanilla and modded servers without requiring RGV installed server-side. Hand movement emulation uses vanilla window click packets to place items into the crafting grid (both 3x3 workbench and 2x2 player inventory).
- **Cheat Mode**: Left-click to give 1 item, `Ctrl + Left Click` to give a full stack (64 items). Uses vanilla creative inventory packet fallback when running on servers without RGV server-side.
- **Recipe Tree & Solver**: Calculates full crafting dependencies, raw materials needed, and step-by-step crafting chains for complex items.
- **Search & Filter**: Real-time filtering by item name, mod ID (`@modid`), tooltip text (`#tooltip`), and OreDictionary / tags (`$tag`).
- **Favorite & Bookmark System**: Pin items to the sidebar with hotkey `A` for quick access.
- **NEI / JEI Coexistence**: Runs alongside NotEnoughItems (1.7.10) and JustEnoughItems (1.12.2) without conflicts, dynamically adjusting viewport boundaries and respecting recipe panel visibility.

## Controls & Keybindings

| Action | Key / Input | Description |
|---|---|---|
| View Recipe | `R` (hovering over item) | Shows how to craft or obtain the item |
| View Usage | `U` (hovering over item) | Shows what recipes consume this item |
| Bookmark Item | `A` (hovering over item) | Pins the item to the bookmark sidebar |
| Transfer Recipe | `Left Click [+]` | Auto-moves materials for 1 craft into grid |
| Max Transfer | `Right Click [+]` | Auto-moves max possible materials into grid |
| Cheat Item (1) | `Left Click` (Cheat mode) | Gives 1 of the item |
| Cheat Stack (64) | `Ctrl + Left Click` (Cheat mode) | Gives a full stack (64) of the item |
| Clear Search | `Right Click` search box | Resets search query |

## Project Structure

```
rgv/
├── core/                  # Pure Java API, solver, data models, GUI & search logic
├── forge-1.7.10/          # Minecraft 1.7.10 Forge implementation & NEI integration
├── forge-1.12.2/          # Minecraft 1.12.2 Forge implementation & JEI integration
└── build/                 # Output directory for release artifacts
```

- **`:core`**: Abstract cross-version API, recipe solver, tree calculator, data structures, renderer abstractions, and custom GUI. Unit-tested with JUnit 5.
- **`:forge-1.7.10`**: Minecraft 1.7.10 Forge implementation. Handles OpenGL 1.1 rendering, Forge OreDictionary registry bridging, NEI hook integration, and client-side recipe transfer emulation for 1.7.10.
- **`:forge-1.12.2`**: Minecraft 1.12.2 Forge implementation. Handles modern Forge registry bridging, recipe matching, JEI integration, GlStateManager rendering, and client-side recipe transfer emulation for 1.12.2.

## Building from Source

### Prerequisites
- JDK 8 (64-bit)
- Git

### Running Unit Tests
```bash
./gradlew :core:test
```

### Build for All Platforms
To build mod jars for all supported platforms and copy them to the root `build/` directory:
```bash
./gradlew buildAll
```

Output artifacts:
- `build/RGV-1.0.0-forge-1.7.10.jar`
- `build/RGV-1.0.0-forge-1.12.2.jar`

### Individual Platform Builds
- Forge 1.7.10: `./gradlew :forge-1.7.10:build`
- Forge 1.12.2: `./gradlew :forge-1.12.2:build`

### Running in Development Environment
- Forge 1.7.10 client: `./gradlew run1710`
- Forge 1.12.2 client: `./gradlew run1122`

## License

This project is licensed under the [MIT License](LICENSE).
Copyright (c) 2026 NexSqaud.
