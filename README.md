# Craftography

A client-side Fabric mod for Minecraft 26.2 that turns the screen into a camera viewfinder.
Frame a shot, pick a lens, level the horizon, set the time of day and take a clean photo.

Everything happens on your client. The mod sends no packets and changes nothing on the server:
the time, weather and moon phase you pick are visual only.

## Features

- A full-screen camcorder viewfinder with thin corner marks and small readouts for the lens, exposure,
  white balance, focus, time and tripod. The centre stays clear for composing the shot.
- Exposure in real stops: ISO 100-12800, aperture f/1.4-f/22 and shutter speed 1/4000-1 s. The defaults
  (ISO 100, f/5.6, 1/125) leave the game's picture as it is. A centre-weighted light meter and a histogram help.
- White balance in kelvin (2500-10000 K) with a green-magenta tint.
- Depth of field from the focal length, aperture and focus distance (thin-lens model), with autofocus on the frame
  centre or manual focus in the Focus tab.
- A shutter slower than a frame blurs motion. Sensor noise grows with ISO (can be turned off).
- Focus peaking and the histogram are shown in the live view only, never in the photo.
- Aspect ratio masks: 16:9, 3:2, 4:3, 1:1, 4:5, 9:16, 2.39:1 and free. Composition guides: rule of thirds, phi grid,
  center cross, diagonals, quarters.
- Lens from 10 mm to 200 mm (full-frame, vertical FOV). Mouse sensitivity follows the zoom.
- Camera roll from -90° to +90° with an electronic level.
- Client-side time of day, visual weather and moon phase.
- F8 opens a pixel-art settings panel docked to the right, with six tabs: Exposure, Colour, Focus, Frame,
  Time/weather and Timer/tripod. Buttons, sliders, keys and scroll all read the same settings.
- Tripod: the camera stays where you set it down, and you can walk into the shot.
- Self-timer: 3 or 10 seconds, with a beep and a blinking lamp.
- Shutter that saves a PNG cropped to the frame, without HUD, to `screenshots/craftography/`. The photo shows the
  same exposure, colour, depth of field, blur and noise as the live view.
- Settings are saved in `config/craftography.json`. The sprites can be changed with a resource pack
  (`assets/craftography/textures/gui/sprites/`).
- Works with Sodium and Iris. With a shader pack on, depth of field is off (the pack does not give the camera the
  depth of the scene) and the camera says so; all other effects work.

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
| Unbound | Shutter, controls (hold), next guide, next aspect ratio, next weather, next self-timer, reset roll, back to server time |

A key you bind works only in camera mode. While camera mode is on, a vanilla key on the same key is blocked.

In interactive mode the player does not move or look around. Click a tab, drag or scroll a slider,
use the arrows to change a setting, or click Shutter to take a photo. Double-click a slider to reset it.
Scroll the panel labels to reach more settings on smaller screens. Outside the panel, the usual scroll shortcuts
still work. Tab selects a setting and the arrow keys adjust it. F8 or Esc closes the panel.

To take a photo of yourself: press F9, walk into the frame, set the self-timer and press the shutter. Without a bound
shutter key, open interactive mode and click Shutter in the panel.

## Install

Requires Fabric Loader and Fabric API for Minecraft 26.2. Put the jar in your `mods` folder.

## Build

```
./gradlew build
```

The jar is written to `build/libs/`.

## License

MIT. The tab and arrow sprites are adapted from [Exposure](https://github.com/mortuusars/Exposure) by
mortuusars (MIT); see `THIRD_PARTY_NOTICES.md`.
