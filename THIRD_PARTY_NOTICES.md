# Third-party notices

## Exposure

Some camera control sprites of Craftography are adapted from Exposure by mortuusars
(https://github.com/mortuusars/Exposure, branch `1.21.1`). Exposure is published under the MIT license, which
covers its code and its assets.

Adapted files, in `src/main/resources/assets/craftography/textures/gui/sprites/camera/`:

| Craftography file | Exposure source (`common/src/main/resources/assets/exposure/textures/gui/`) | Change |
|---|---|---|
| `button.png`, `button_highlighted.png` | `sprites/camera_controls/composition_guide/none*.png` | icon removed |
| `shutter_button.png`, `shutter_button_highlighted.png` | `sprites/camera_controls/composition_guide/none*.png` | icon removed, widened to 21 px |
| `plate_left.png`, `plate_left_highlighted.png` | `sprites/camera_controls/focal_length*.png` | none |
| `plate_right.png`, `plate_right_highlighted.png` | `sprites/camera_controls/frame_counter*.png` | icon removed |
| `plate.png`, `plate_highlighted.png` | `sprites/camera_controls/focal_length*.png`, `frame_counter*.png` | edges of both joined, icon removed, nine-sliced |
| `separator.png` | `sprites/camera_controls/button_separator.png` | none |
| `bezel.png` | `viewfinder/viewfinder.png` | border halved, center removed, nine-sliced |

The text colors of the camera controls (`#2B2622` with a `#7A736C` line under it) are Exposure's default
viewfinder font colors. All `icon_*.png` sprites and the other textures of Craftography are original.

```
MIT License

Copyright (c) 2025 mortuusars

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
