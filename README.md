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
- Pixel-art camera body: a bezel around the frame and a row of camera buttons under it.
- Interactive mode: a free cursor and a button for every camera action. Side panels hold the settings, each on a sideways thumbwheel you roll by dragging or scrolling:
  ISO, shutter, aperture, white balance, tint, focus, grain, histogram and peaking on the left; aspect ratio,
  guide, weather, moon, mask and guide opacity on the right. The same panels open while the control panel key is held.
- Tripod: the camera stays where you set it down, and you can walk into the shot.
- Self-timer: 3 or 10 seconds, with a beep and a blinking lamp.
- Shutter that saves a PNG cropped to the frame, without HUD, to `screenshots/craftography/`.
- Settings are saved in `config/craftography.json`. The HUD sprites can be changed with a resource pack
  (`assets/craftography/textures/gui/sprites/viewfinder/`).
- Works with Sodium and Iris, with shaders on or off.

## Controls

Only F7, F8 and F9 have default keys. Bind the rest in Options > Controls > Craftography.

| Key | Action |
|---|---|
| F7 | Camera mode on/off |
| F8 | Interactive mode on/off (camera mode only; Esc also closes it) |
| F9 | Tripod: set the camera down here, or give it back to the player (camera mode only) |
| Scroll | Zoom (Shift: fine) |
| Alt + Scroll | Roll (Shift: fine) |
| Ctrl + Scroll | Time of day, 100 ticks per step (Shift: 10) |
| Middle click | Reset lens to 35 mm |
| Unbound | Shutter, control panel (hold), next guide, next aspect ratio, next weather, next self-timer, reset roll, back to server time |

A key you bind works only in camera mode. While camera mode is on, a vanilla key on the same key is blocked.
In interactive mode the player does not move or look around. Click a camera button, or scroll over it. Drag the lens
and time plates sideways, or scroll over them. The menu button shows or hides the side panels. Drag a control or scroll over
it; double-click a slider, the dial, the time bar or a plate to reset it.

To take a photo of yourself: press F9, walk into the frame, set the self-timer and press the shutter. Without a bound
shutter key, open interactive mode and click the shutter button.

## Install

Requires Fabric Loader and Fabric API for Minecraft 26.2. Put the jar in your `mods` folder.

## Build

```
./gradlew build
```

The jar is written to `build/libs/`.

## License

MIT. The camera control button, plate and bezel sprites are adapted from
[Exposure](https://github.com/mortuusars/Exposure) by mortuusars (MIT); see `THIRD_PARTY_NOTICES.md`.
