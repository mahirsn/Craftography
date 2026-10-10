# Craftography

A client-side Fabric mod for Minecraft 26.2 that turns the screen into a camera viewfinder.
Frame a shot, pick a lens, level the horizon, set the time of day and take a clean photo.

Everything happens on your client. The mod sends no packets and changes nothing on the server:
the time, weather and moon phase you pick are visual only.

## Features

- Camera mode with aspect ratio masks: 16:9, 3:2, 4:3, 1:1, 4:5, 9:16, 2.39:1 and free.
- Composition guides inside the frame: rule of thirds, phi grid, center cross, diagonals, quarters.
- Lens from 10 mm to 200 mm (full-frame, vertical FOV), log-scale zoom that stops at the common focal lengths.
  Mouse sensitivity follows the zoom.
- Camera roll from -90° to +90° with a horizon level.
- Client-side time of day bar with golden and blue hour bands, visual weather and moon phase.
- Hold-to-open control panel with sliders, a roll dial and selectors.
- Shutter that saves a PNG cropped to the frame, without HUD, to `screenshots/craftography/`.
- Settings are saved in `config/craftography.json`. The HUD sprites can be changed with a resource pack
  (`assets/craftography/textures/gui/sprites/viewfinder/`).
- Works with Sodium and Iris, with shaders on or off.

## Controls

Only camera mode has a default key. Bind the rest in Options > Controls > Craftography.

| Key | Action |
|---|---|
| F7 | Camera mode on/off |
| Scroll | Zoom (Shift: fine) |
| Alt + Scroll | Roll (Shift: fine) |
| Ctrl + Scroll | Time of day, 100 ticks per step (Shift: 10) |
| Middle click | Reset lens to 35 mm |
| Unbound | Shutter, control panel (hold), next guide, next aspect ratio, next weather, reset roll, back to server time |

A key you bind works only in camera mode. While camera mode is on, a vanilla key on the same key is blocked.
In the control panel, drag a control or scroll over it. Double-click a slider, the dial or the time bar to reset it.

## Install

Requires Fabric Loader and Fabric API for Minecraft 26.2. Put the jar in your `mods` folder.

## Build

```
./gradlew build
```

The jar is written to `build/libs/`.

## License

MIT
