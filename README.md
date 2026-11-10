# Underground Audio Visualizer (Fabric 1.21.11)

Client-side mod. Draws fading boxes and tracers where selected sounds were played.
Press **Delete** to open the GUI (rebind in Controls). Settings save to `config/underground-audio-visualizer.json`.

## Build
Needs JDK 21 and internet (first build downloads Minecraft + Fabric).

    ./gradlew build        # Windows: gradlew.bat build

Jar: `build/libs/underground-audio-visualizer-1.0.0.jar` -> put in `mods/` with Fabric API.
`./gradlew runClient` launches a dev client.

## GUI
- General: enable, height filter (underground / above / any) + offset, max distance, duration, max markers, ignore own sounds, log to chat
- Filters: toggle + color for Villagers, Steps, Redstone, Doors/Chests, Break/Place, Explosions, Combat, Players, Mobs, plus a custom keyword box
- Visuals: boxes, tracers, box size, tracer width, fade, opacity

## If the build fails (1.21.x render/input APIs move a lot)
- `setLineWidth(width)` in `UndergroundAudioVisualizer.line(...)`: delete it.
- `KeyBindingHelper` import: newer Fabric API may call it `KeyMappingHelper` (package `...client.keymapping.v1`).
- `getMainCamera().getPosition()`: may be `position()`.
- `KeyMapping.Category.MISC`: the key category enum introduced in 1.21.9.
