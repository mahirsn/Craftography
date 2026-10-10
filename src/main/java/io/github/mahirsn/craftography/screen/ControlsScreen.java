package io.github.mahirsn.craftography.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.CameraSettings.Aspect;
import io.github.mahirsn.craftography.CameraSettings.Guide;
import io.github.mahirsn.craftography.CameraSettings.Weather;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.hud.AspectMask;
import io.github.mahirsn.craftography.hud.LevelGauge;
import io.github.mahirsn.craftography.hud.TimeBar;
import io.github.mahirsn.craftography.hud.ViewfinderHud;
import io.github.mahirsn.craftography.hud.ZoomScale;
import io.github.mahirsn.craftography.screen.widget.Control;
import io.github.mahirsn.craftography.screen.widget.Dial;
import io.github.mahirsn.craftography.screen.widget.SegmentedSelector;
import io.github.mahirsn.craftography.screen.widget.Slider;
import io.github.mahirsn.craftography.util.LensMath;
import io.github.mahirsn.craftography.util.Rect;
import io.github.mahirsn.craftography.util.TimeMath;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Panel that is open while the panel key is held. The cursor is free; the controls sit on top of the viewfinder HUD,
 * and the zoom ruler, level gauge and time bar of the HUD become draggable.
 */
public class ControlsScreen extends Screen {
	private static final int ROW = 26;

	private final List<Control> controls = new ArrayList<>();
	private Control dragging, lastPressed;
	private Aspect laidOutFor;

	public ControlsScreen() {
		super(Component.translatable("craftography.panel"));
	}

	@Override
	protected void init() {
		layout();
	}

	private void layout() {
		CameraSettings s = CameraSettings.get();
		laidOutFor = s.aspect;
		controls.clear();
		Rect f = AspectMask.frame(width, height, s.aspect);

		// Left: the zoom ruler, log scale, soft snap to presets.
		controls.add(new Slider(ZoomScale.ruler(f), true, true, LensMath.MIN_MM, LensMath.MAX_MM,
			() -> s.focalLength, v -> s.focalLength = LensMath.clamp(v), ControlsScreen::snapToPreset,
			(v, amount, fine) -> LensMath.zoom(v, fine ? amount / 4 : amount), () -> s.focalLength = LensMath.DEFAULT_MM, null));

		// Right: roll dial around the level gauge.
		controls.add(new Dial(LevelGauge.centerX(f, width), LevelGauge.centerY(f), 22, () -> s.roll, v -> s.roll = v,
			(v, amount, fine) -> {
				s.addRoll(amount * (fine ? 0.1 : 1));
				return s.roll;
			}));

		// Bottom: the time bar.
		Rect bar = TimeBar.bar(f, width, height);
		controls.add(new Slider(new Rect(bar.x() - 3, bar.y() - 6, bar.w() + 6, 12), false, false, 0, TimeMath.DAY,
			() -> ViewfinderHud.visibleDayTicks(s), v -> s.timeOfDay = TimeMath.dayTicks(Math.round(v / 10) * 10), v -> v,
			(v, amount, fine) -> {
				s.timeOfDay = TimeMath.dayTicks(Math.round(v) + Math.round(amount * (fine ? 10 : 100)));
				return s.timeOfDay;
			}, () -> s.timeOfDay = -1, null));

		// Center column.
		double w = Math.min(240, width - 2 * (ZoomScale.ruler(f).right() + 8));
		double x = (width - w) / 2, y = Math.max(f.y() + 8, 4);
		Aspect[] aspects = Aspect.values();
		controls.add(new SegmentedSelector(new Rect(x, y, w, ROW - 4), Component.translatable("craftography.panel.aspect"),
			Arrays.stream(aspects).map(a -> (Component) (a == Aspect.FREE ? Component.translatable("craftography.aspect.free") : Component.literal(a.label))).toList(),
			() -> s.aspect.ordinal(), i -> s.aspect = aspects[i], null));
		y += ROW;
		Guide[] guides = Guide.values();
		controls.add(new SegmentedSelector(new Rect(x, y, w, ROW - 4), Component.translatable("craftography.panel.guide"),
			Arrays.stream(guides).map(gd -> (Component) Component.translatable(gd.translationKey())).toList(),
			() -> s.guide.ordinal(), i -> s.guide = guides[i],
			(g, i, box, color) -> icon(g, "guide_" + guides[i].name().toLowerCase(java.util.Locale.ROOT), box, 16, 11, color)));
		y += ROW;
		Weather[] weathers = Weather.values();
		double weatherW = w * 0.35;
		controls.add(new SegmentedSelector(new Rect(x, y, weatherW, ROW - 4), Component.translatable("craftography.panel.weather"),
			Arrays.stream(weathers).map(wt -> (Component) Component.translatable(wt.translationKey())).toList(),
			() -> s.weather.ordinal(), i -> s.weather = weathers[i],
			(g, i, box, color) -> icon(g, "weather_" + weathers[i].name().toLowerCase(java.util.Locale.ROOT), box, 11, 11, color)));
		List<Component> moons = new ArrayList<>();
		moons.add(Component.translatable("craftography.moon.server"));
		for (int p = 0; p < TimeMath.MOON_PHASES; p++) moons.add(Component.translatable("craftography.moon." + p));
		controls.add(new SegmentedSelector(new Rect(x + weatherW + 12, y, w - weatherW - 12, ROW - 4), Component.translatable("craftography.panel.moon"),
			moons, () -> s.moonPhase + 1, i -> s.moonPhase = i - 1,
			(g, i, box, color) -> icon(g, i == 0 ? "weather_server" : "moon_" + (i - 1), box, 11, 11, color)));
		y += ROW;
		double half = (w - 12) / 2;
		controls.add(opacity(new Rect(x, y, half, ROW - 6), "craftography.panel.mask_opacity", () -> s.maskOpacity, v -> s.maskOpacity = v, 0.75f));
		controls.add(opacity(new Rect(x + half + 12, y, half, ROW - 6), "craftography.panel.guide_opacity", () -> s.guideOpacity, v -> s.guideOpacity = v, 0.45f));
		y += ROW;
		List<Component> offOn = List.of(Component.translatable("options.off"), Component.translatable("options.on"));
		controls.add(new SegmentedSelector(new Rect(x, y, half, ROW - 4), Component.translatable("craftography.panel.lock"),
			offOn, () -> s.lockTimeWeather ? 1 : 0, i -> s.lockTimeWeather = i == 1, null));
		controls.add(new SegmentedSelector(new Rect(x + half + 12, y, half, ROW - 4), Component.translatable("craftography.panel.preview"),
			offOn, () -> s.showPreview ? 1 : 0, i -> s.showPreview = i == 1, null));
	}

	/** Draws a sprite centered in a segment box, at its pixel size if it fits. */
	private static void icon(GuiGraphicsExtractor g, String name, Rect box, int w, int h, int color) {
		double scale = Math.min(1, Math.min(box.w() / w, box.h() / h));
		int sw = (int) Math.round(w * scale), sh = (int) Math.round(h * scale);
		io.github.mahirsn.craftography.util.Draw.sprite(g, name, Math.round(box.x() + (box.w() - sw) / 2), Math.round(box.y() + (box.h() - sh) / 2), sw, sh, color);
	}

	private interface FloatSetter {
		void set(float v);
	}

	private static Slider opacity(Rect area, String key, java.util.function.DoubleSupplier get, FloatSetter set, float def) {
		return new Slider(area, false, false, 0, 1, get, v -> set.set((float) Math.max(0, Math.min(1, v))), v -> v,
			(v, amount, fine) -> Math.max(0, Math.min(1, v + amount * (fine ? 0.01 : 0.05))), () -> set.set(def), Component.translatable(key));
	}

	/** Drag snap: within 3 % of a preset focal length, use the preset. */
	private static double snapToPreset(double mm) {
		for (double p : LensMath.PRESETS) {
			if (Math.abs(mm / p - 1) < 0.03) return p;
		}
		return LensMath.clamp(mm);
	}

	private Control at(double x, double y) {
		for (Control c : controls) {
			if (c.contains(x, y)) return c;
		}
		return null;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		if (laidOutFor != CameraSettings.get().aspect) layout();
		Control hovered = dragging != null ? dragging : at(mouseX, mouseY);
		for (Control c : controls) c.draw(g, c == hovered);
		if (dragging == null && hovered instanceof SegmentedSelector selector) {
			Component label = selector.hoveredLabel(mouseX);
			if (label != null) g.setTooltipForNextFrame(font, label, mouseX, mouseY);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		// No background: the picture stays visible while adjusting.
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
		Control c = at(event.x(), event.y());
		if (c == null) return false;
		// A double click counts only on the control that got the first click.
		if (doubleClick && c == lastPressed) {
			c.reset();
			lastPressed = null;
		} else {
			c.press(event.x(), event.y());
			dragging = c;
			lastPressed = c;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) return false;
		dragging.drag(event.x(), event.y());
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		double amount = scrollY != 0 ? scrollY : -scrollX;
		if (amount == 0) return false;
		Control c = at(x, y);
		if (c != null) {
			c.scroll(amount, CraftographyClient.shiftDown());
		} else {
			CraftographyClient.onScroll(amount);
		}
		return true;
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (CraftographyClient.panelKey.matches(event)) {
			onClose();
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public void tick() {
		if (!CameraMode.isOpen() || !panelKeyHeld()) onClose();
	}

	private boolean panelKeyHeld() {
		InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(CraftographyClient.panelKey);
		if (key.getType() == InputConstants.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
		}
		return key.getType() != InputConstants.Type.KEYSYM || CraftographyClient.isDown(key.getValue());
	}

	@Override
	public void removed() {
		CameraSettings.save();
	}
}
