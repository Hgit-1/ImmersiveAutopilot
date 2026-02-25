# Immersive Autopilot

NeoForge 1.21.1 mod that adds airspace control tools for Immersive Aircraft: **Tower** + **Radar** blocks, route programs, and optional auto routes.

## Requirements
- Minecraft `1.21.1`
- NeoForge `21.1.x`
- Immersive Aircraft `1.2.2+` (NeoForge)

## Features
- Tower block with GUI for scanning aircraft and issuing route programs.
- Radar block that extends tower scan range via nearby radar bonuses.
- Route programs with waypoints, links, and basic guidance (arrow + trigger radius).
- Pilot route offer UI (accept/decline), plus auto routes stored server-side.
- Optional Xaero Minimap integration for temporary waypoint markers.
- Radar range overlay tool for visualizing coverage.

## Blocks & Items
- `Tower` block: airspace control and route editing.
- `Radar` block: range bonus when placed near the tower (5x5x5).
- `Radar Lens`, `Signal Amplifier`, `Radar Range Sensor`, `Radar Identification Module`.

## How To Use
1. Place a `Tower` and open its UI.
1. Create or load a route program.
1. Bind a target aircraft or target all in range.
1. Send the route to the pilot and confirm the offer.
1. (Optional) Use auto routes by listing route names in the aircraft UI.

## Auto Routes (Aircraft UI)
- Each line is a route name. Optional label syntax: `route|label`.
- Auto routes persist server-side and advance when leaving airspace.
- Use the `X` button to clear a single line, or `Clear` to remove all.

## Radar Range Overlay
- Hold `Radar Range Sensor` to display tower coverage.
- Overlap regions render green, single coverage renders gold, boundary is cyan.

## Xaero Minimap
- If Xaero Minimap is installed, temporary waypoints are shown on the minimap.
- This is optional; the mod still works without Xaero.

## Build
```bash
./gradlew build
```
Output JARs are in `build/libs`.

## GitHub Actions
CI builds the mod and uploads the JAR as an artifact named `immersive-autopilot-jar`.

## Notes
- `logo.png` is used as the mod logo (see `src/main/resources/logo.png`).
- Textures live under `src/main/resources/assets/immersive_autopilot/textures/`.
