# Immersive Autopilot

NeoForge 1.21.1 mod that adds the **Tower** block for sending route programs to Immersive Aircraft vehicles.

## Features
- Tower block with GUI
- Scan nearby aircraft and bind targets
- Create/edit route programs (waypoints + speed + hold)
- Pilot receives accept/decline prompt
- Route data stored on aircraft

## Build
```bash
./gradlew build
```
Output JARs are in `build/libs`.

## GitHub Actions
CI builds the mod and uploads the JAR as an artifact named `immersive-autopilot-jar`.

## Notes
Textures are placeholders; add your own:
- `src/main/resources/assets/immersive_autopilot/textures/block/tower.png`
