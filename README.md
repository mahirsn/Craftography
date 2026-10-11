# Craftography

A client-side Fabric mod for Minecraft 26.2 that turns the screen into a camera viewfinder.
Frame a shot, pick a lens, level the horizon, set the time of day and take a clean photo.

Everything happens on your client. The mod sends no packets and changes nothing on the server:
the time, weather and moon phase you pick are visual only.

## Features

- A 3D camcorder in your hand when camera mode is on, with a see-through flip-out screen. The lean key (right mouse
  button) brings it up to your eye and hands over to the viewfinder; the lens effects, zoom and roll fade in as you
  lean, so just holding the camera costs nothing. A photo is taken once you are leaned in (the shutter leans in by
  itself). The camera is only drawn on your screen: your real items and the server are not touched. It can be turned
  off in the settings, which brings back the plain viewfinder.
- Camera mode with aspect ratio masks: 16:9, 3:2, 4:3, 1:1, 4:5, 9:16, 2.39:1 and free.
- Composition guides inside the frame: rule of thirds, phi grid, center cross, diagonals, quarters.
- Lens from 10 mm to 200 mm (full-frame, vertical FOV), log-scale zoom that stops at the common focal lengths.
  Mouse sensitivity follows the zoom.
- Camera roll from -90° to +90° on a roll plate in the camera bar, with an engraved level that tilts with it.
- Client-side time of day bar with golden and blue hour bands, visual weather and moon phase.
- Pixel-art camera body: a bezel around the frame, and one band at the bottom of the screen with a row of camera
  buttons and three top dials beside it.
- Layout: the picture takes the whole screen above that band, for every aspect ratio, and nothing ever covers it.
- Top dials with pages: exposure (ISO, shutter speed, aperture), colour (white balance, tint, grain), focus (focus
  distance, autofocus, focus peaking), view (mask and guide opacity, histogram) and scene (moon phase, keep time and
  weather, photo preview). The menu button turns the page. A dial's name turns amber while it is off its default.
- Interactive mode: a free cursor to press every button and turn every dial and plate. It also opens while the
  control panel key is held.
- Tripod: the camera stays where you set it down, and you can walk into the shot.
- Self-timer: 3 or 10 seconds, with a beep and a blinking lamp.
- Shutter that saves a PNG cropped to the frame, without HUD, to `screenshots/craftography/`.
- A settings screen with tabs (held camera, image, viewfinder, scene). It opens from Mod Menu, the settings key, or a
  right-click on the dial page button in interactive mode.
- Settings are saved in `config/craftography.json`. The HUD sprites can be changed with a resource pack
  (`assets/craftography/textures/gui/sprites/camera/` and `.../viewfinder/`).
- Works with Sodium and Iris, with shaders on or off.

## Controls

Only F7, F8, F9 and the right mouse button have default keys. Bind the rest in Options > Controls > Craftography.

| Key | Action |
|---|---|
| F7 | Camera mode on/off |
| F8 | Interactive mode on/off (camera mode only; Esc also closes it) |
| F9 | Tripod: set the camera down here, or give it back to the player (camera mode only) |
| Right mouse button | Lean in to the camera screen (camera mode only). In the settings: hold, toggle, or tap to toggle and hold to lean while held |
| Scroll | Zoom (Shift: fine) |
| Alt + Scroll | Roll (Shift: fine) |
| Ctrl + Scroll | Time of day, 100 ticks per step (Shift: 10) |
| Middle click | Reset lens to 35 mm |
| Unbound | Shutter, settings, control panel (hold), next guide, next aspect ratio, next weather, next self-timer, reset roll, back to server time |

A key you bind works only in camera mode. While camera mode is on, a vanilla key on the same key is blocked.
In interactive mode the player does not move or look around. Click a camera button, or scroll over it. Drag the lens,
roll and time plates and the dials sideways, or scroll over them; click a dial that holds a choice or a switch to change
it. Double-click a dial that holds a value, a plate or the time bar to reset it. The menu button turns the dials to their next page.

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
