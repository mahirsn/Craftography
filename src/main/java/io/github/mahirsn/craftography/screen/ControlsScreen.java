package io.github.mahirsn.craftography.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.CameraSettings.Aspect;
import io.github.mahirsn.craftography.CameraSettings.Guide;
import io.github.mahirsn.craftography.CameraSettings.Weather;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.Tripod;
import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.hud.CameraBar;
import io.github.mahirsn.craftography.hud.AspectMask;
import io.github.mahirsn.craftography.hud.LevelGauge;
import io.github.mahirsn.craftography.hud.TimeBar;
import io.github.mahirsn.craftography.hud.ViewfinderHud;
import io.github.mahirsn.craftography.screen.widget.BarButton;
import io.github.mahirsn.craftography.screen.widget.Control;
import io.github.mahirsn.craftography.screen.widget.Dial;
import io.github.mahirsn.craftography.screen.widget.SettingRow;
import io.github.mahirsn.craftography.screen.widget.Slider;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.ExposureMath;
import io.github.mahirsn.craftography.util.LensMath;
import io.github.mahirsn.craftography.util.Rect;
import io.github.mahirsn.craftography.util.TimeMath;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Camera controls with a free cursor, on top of the viewfinder HUD. The level gauge, time bar and camera bar of the HUD
 * become draggable and clickable, and the side panels hold the settings as camera-bar plates. Two ways to open it:
 * <ul>
 * <li>interactive mode (toggle key): stays open until the toggle key or Esc; the settings sheet opens with the menu
 * button;</li>
 * <li>held panel (panel key): open while the key is held, with the settings sheet shown.</li>
 * </ul>
 * While it is open the player does not move or look around, because vanilla sends no game input to a screen.
 */
public class ControlsScreen extends Screen {
	/** Settings columns: left and right x, width, top, row pitch and plate height, in GUI units. */
	private double leftX, rightX, colW, top, pitch, rowH;

	private final boolean held;
	private boolean sheetOpen;
	/** Controls that are always there; the settings sheet only while it is shown. */
	private final List<Control> controls = new ArrayList<>(), sheet = new ArrayList<>();
	private Control dragging, lastPressed;
	private Aspect laidOutFor;

	public ControlsScreen(boolean held) {
		super(Component.translatable(held ? "craftography.panel" : "craftography.interactive"));
		this.held = held;
		this.sheetOpen = true; // the side panels are part of the interactive mode as well
	}

	@Override
	protected void init() {
		layout();
	}

	private void layout() {
		CameraSettings s = CameraSettings.get();
		laidOutFor = s.aspect;
		controls.clear();
		sheet.clear();
		addBar(s);
		Rect f = AspectMask.frame(width, height, s.aspect);

		// Right: roll dial around the level gauge.
		controls.add(new Dial(LevelGauge.centerX(f, width), LevelGauge.centerY(f), 22, () -> s.roll, v -> s.roll = v,
			(v, amount, fine) -> {
				s.addRoll(amount * (fine ? 0.1 : 1));
				return s.roll;
			}));

		// Bottom: the time bar.
		Rect bar = TimeBar.bar(f, width, height);
		controls.add(new Slider(new Rect(bar.x(), bar.y() - 6, bar.w(), 12), 0, TimeMath.DAY,
			() -> ViewfinderHud.visibleDayTicks(s), v -> s.timeOfDay = TimeMath.dayTicks(Math.round(v / 10) * 10), v -> v,
			(v, amount, fine) -> {
				s.timeOfDay = TimeMath.dayTicks(Math.round(v) + Math.round(amount * (fine ? 10 : 100)));
				return s.timeOfDay;
			}, () -> s.timeOfDay = -1));

		// Settings on the two sides of the frame, one camera-bar style plate per setting: camera on the left, scene on
		// the right. Each column fills its gutter from the picture's top edge down to the camera bar; rows shrink to fit.
		colW = AspectMask.gutter(width) - 12;
		leftX = 6;
		rightX = width - 6 - colW;
		top = AspectMask.BEZEL;
		pitch = Math.min(20, (CameraBar.top(height) - 3 - top) / 10);
		rowH = Math.max(10, Math.min(CameraBar.HEIGHT, Math.floor(pitch) - 1));

		sheet.add(value(slot(false, 0), "craftography.panel.iso", () -> String.valueOf(s.iso),
			n -> s.iso = ExposureMath.step(ExposureMath.ISO, s.iso, whole(n)),
			() -> s.iso = ExposureMath.DEFAULT_ISO, () -> s.iso != ExposureMath.DEFAULT_ISO));
		sheet.add(value(slot(false, 1), "craftography.panel.shutter", () -> ExposureMath.formatShutter(s.shutter),
			n -> s.shutter = ExposureMath.step(ExposureMath.SHUTTER, s.shutter, whole(n)), () -> s.shutter = ExposureMath.DEFAULT_SHUTTER,
			() -> ExposureMath.nearest(ExposureMath.SHUTTER, s.shutter) != ExposureMath.nearest(ExposureMath.SHUTTER, ExposureMath.DEFAULT_SHUTTER)));
		sheet.add(value(slot(false, 2), "craftography.panel.aperture", () -> ExposureMath.formatAperture(s.aperture),
			n -> s.aperture = ExposureMath.step(ExposureMath.APERTURE, s.aperture, whole(n)), () -> s.aperture = ExposureMath.DEFAULT_APERTURE,
			() -> ExposureMath.nearest(ExposureMath.APERTURE, s.aperture) != ExposureMath.nearest(ExposureMath.APERTURE, ExposureMath.DEFAULT_APERTURE)));
		sheet.add(value(slot(false, 3), "craftography.panel.white_balance", () -> s.kelvin + "K",
			steps -> s.kelvin = (int) Math.clamp(s.kelvin + Math.round(steps * (fine() ? 50 : 200)), ExposureMath.MIN_KELVIN, ExposureMath.MAX_KELVIN),
			() -> s.kelvin = ExposureMath.DEFAULT_KELVIN, () -> s.kelvin != ExposureMath.DEFAULT_KELVIN));
		sheet.add(value(slot(false, 4), "craftography.panel.tint", () -> (s.tint > 0 ? "M" : s.tint < 0 ? "G" : "") + Math.abs(s.tint),
			steps -> s.tint = (int) Math.clamp(s.tint + Math.round(steps * (fine() ? 1 : 5)), -ExposureMath.MAX_TINT, ExposureMath.MAX_TINT),
			() -> s.tint = 0, () -> s.tint != 0));
		sheet.add(value(slot(false, 5), "craftography.panel.focus", () -> s.autofocus ? "AF" : ExposureMath.formatFocus(s.focusDistance),
			steps -> {
				s.autofocus = false;
				s.focusDistance = Math.clamp(s.focusDistance * Math.pow(1.15, fine() ? steps / 4 : steps), ExposureMath.MIN_FOCUS, ExposureMath.FOCUS_INFINITY);
			}, () -> s.autofocus = true, () -> !s.autofocus));
		sheet.add(toggle(slot(false, 6), "craftography.panel.autofocus", () -> s.autofocus, v -> s.autofocus = v, true));
		sheet.add(toggle(slot(false, 7), "craftography.panel.grain", () -> s.noise, v -> s.noise = v, true));
		sheet.add(toggle(slot(false, 8), "craftography.panel.histogram", () -> s.histogram, v -> s.histogram = v, false));
		sheet.add(toggle(slot(false, 9), "craftography.panel.peaking", () -> s.peaking, v -> s.peaking = v, false));

		Aspect[] aspects = Aspect.values();
		sheet.add(choice(slot(true, 0), "craftography.panel.ratio", aspects.length, () -> s.aspect.ordinal(), i -> s.aspect = aspects[i],
			Aspect.R3_2.ordinal(), i -> aspectName(aspects[i]), () -> aspectName(s.aspect).getString(), null));
		Guide[] guides = Guide.values();
		sheet.add(choice(slot(true, 1), "craftography.panel.guide", guides.length, () -> s.guide.ordinal(), i -> s.guide = guides[i],
			Guide.THIRDS.ordinal(), i -> Component.translatable(guides[i].translationKey()), null,
			(g, x, y) -> CameraBar.engravedIcon(g, "guide_" + s.guide.name().toLowerCase(Locale.ROOT), x, y)));
		Weather[] weathers = Weather.values();
		sheet.add(choice(slot(true, 2), "craftography.panel.weather", weathers.length, () -> s.weather.ordinal(), i -> s.weather = weathers[i],
			Weather.SERVER.ordinal(), i -> Component.translatable(weathers[i].translationKey()), null,
			(g, x, y) -> CameraBar.engravedIcon(g, "weather_" + s.weather.name().toLowerCase(Locale.ROOT), x, y)));
		sheet.add(choice(slot(true, 3), "craftography.panel.moon", TimeMath.MOON_PHASES + 1, () -> s.moonPhase + 1, i -> s.moonPhase = i - 1,
			0, i -> Component.translatable(i == 0 ? "craftography.moon.server" : "craftography.moon." + (i - 1)), null,
			(g, x, y) -> engravedSprite(g, s.moonPhase < 0 ? "weather_server" : "moon_" + s.moonPhase, x + 1, y)));
		sheet.add(opacity(slot(true, 4), "craftography.panel.mask_opacity", () -> s.maskOpacity, v -> s.maskOpacity = v, 0.75f));
		sheet.add(opacity(slot(true, 5), "craftography.panel.guide_opacity", () -> s.guideOpacity, v -> s.guideOpacity = v, 0.45f));
		sheet.add(toggle(slot(true, 6), "craftography.panel.lock", () -> s.lockTimeWeather, v -> s.lockTimeWeather = v, false));
		sheet.add(toggle(slot(true, 7), "craftography.panel.preview", () -> s.showPreview, v -> s.showPreview = v, true));
	}

	/** Rectangle of the {@code i}th row of the left or right settings column. */
	private Rect slot(boolean right, int i) {
		return new Rect(right ? rightX : leftX, Math.floor(top + i * pitch), colW, rowH);
	}

	private static boolean fine() {
		return CraftographyClient.shiftDown();
	}

	/** Whole steps from a scroll or drag amount; a small trackpad scroll still moves one step. */
	private static int whole(double steps) {
		return (int) Math.signum(steps) * Math.max(1, (int) Math.abs(Math.round(steps)));
	}

	private static Component aspectName(Aspect a) {
		return a == Aspect.FREE ? Component.translatable("craftography.aspect.free") : Component.literal(a.label);
	}

	private static Component onOff(boolean on) {
		return Component.translatable(on ? "options.on" : "options.off");
	}

	/** A continuous value: drag or scroll to change, double-click resets. */
	private static SettingRow value(Rect area, String key, Supplier<String> text, DoubleConsumer step, Runnable reset, BooleanSupplier changed) {
		return new SettingRow(area, Component.translatable(key), text, null, step, false, reset, changed, () -> Component.literal(text.get()));
	}

	/** One of a few choices: each click or scroll notch moves to the next one. */
	private static SettingRow choice(Rect area, String key, int count, IntSupplier get, IntConsumer set, int def,
									 IntFunction<Component> words, Supplier<String> text, SettingRow.Icon icon) {
		return new SettingRow(area, Component.translatable(key), text, icon, n -> set.accept(Math.floorMod(get.getAsInt() + whole(n), count)),
			true, null, () -> get.getAsInt() != def, () -> words.apply(get.getAsInt()));
	}

	/** An on/off switch: each click or scroll notch flips it. */
	private static SettingRow toggle(Rect area, String key, BooleanSupplier get, Consumer<Boolean> set, boolean def) {
		return new SettingRow(area, Component.translatable(key), () -> onOff(get.getAsBoolean()).getString(), null,
			n -> set.accept(!get.getAsBoolean()), true, null, () -> get.getAsBoolean() != def, () -> onOff(get.getAsBoolean()));
	}

	private interface FloatSetter {
		void set(float v);
	}

	private static SettingRow opacity(Rect area, String key, DoubleSupplier get, FloatSetter set, float def) {
		return value(area, key, () -> Math.round(get.getAsDouble() * 100) + "%",
			n -> set.set((float) Math.clamp(get.getAsDouble() + n * (fine() ? 0.01 : 0.05), 0, 1)),
			() -> set.set(def), () -> Math.abs(get.getAsDouble() - def) > 0.001);
	}

	/** A viewfinder sprite engraved like the camera icons, drawn 9x9 so it sits in an icon's place. */
	private static void engravedSprite(GuiGraphicsExtractor g, String name, int x, int y) {
		Draw.sprite(g, name, x, y + 1, 9, 9, Draw.argb(1f, Draw.PRINT_LIGHT));
		Draw.sprite(g, name, x, y, 9, 9, Draw.argb(1f, Draw.PRINT));
	}

	private void addBar(CameraSettings s) {
		for (CameraBar.Item item : CameraBar.Item.values()) {
			Rect area = CameraBar.area(item, width, height);
			BarButton.Painter paint = g -> CameraBar.drawItem(g, item, s, CameraMode.focalLength(), ViewfinderHud.visibleDayTicks(s), true);
			controls.add(switch (item) {
				case ZOOM -> BarButton.plate(area, paint, () -> tooltip("craftography.button.zoom", null, Component.translatable("craftography.button.zoom.hint")),
					steps -> CraftographyClient.change(c -> c.focalLength = LensMath.zoom(c.focalLength, CraftographyClient.shiftDown() ? steps / 4 : steps)),
					() -> CraftographyClient.change(c -> c.focalLength = LensMath.DEFAULT_MM));
				case TIME -> BarButton.plate(area, paint, () -> tooltip("craftography.button.time", null, Component.translatable("craftography.button.time.hint")),
					steps -> CraftographyClient.change(c -> c.timeOfDay = TimeMath.dayTicks(ViewfinderHud.visibleDayTicks(c) + Math.round(steps * (CraftographyClient.shiftDown() ? 10 : 100)))),
					() -> CraftographyClient.change(c -> c.timeOfDay = -1));
				case GUIDE -> BarButton.button(area, paint, () -> tooltip("craftography.panel.guide", CraftographyClient.guideKey,
					Component.translatable(s.guide.translationKey())), () -> CraftographyClient.change(CameraSettings::cycleGuide));
				case ASPECT -> BarButton.button(area, paint, () -> tooltip("craftography.panel.aspect", CraftographyClient.aspectKey,
					s.aspect == Aspect.FREE ? Component.translatable("craftography.aspect.free") : Component.literal(s.aspect.label)),
					() -> CraftographyClient.change(CameraSettings::cycleAspect));
				case WEATHER -> BarButton.button(area, paint, () -> tooltip("craftography.panel.weather", CraftographyClient.weatherKey,
					Component.translatable(s.weather.translationKey())), () -> CraftographyClient.change(CameraSettings::cycleWeather));
				case RESET_ROLL -> BarButton.button(area, paint, () -> tooltip("key.craftography.reset_roll", CraftographyClient.resetRollKey,
					Component.literal(String.format(java.util.Locale.ROOT, "%+.1f°", s.roll))), () -> CraftographyClient.change(c -> c.roll = 0));
				case SERVER_TIME -> BarButton.button(area, paint, () -> tooltip("key.craftography.server_time", CraftographyClient.serverTimeKey, null),
					() -> CraftographyClient.change(c -> c.timeOfDay = -1));
				case TIMER -> BarButton.button(area, paint, () -> tooltip("craftography.button.timer", CraftographyClient.timerKey,
					s.selfTimer == 0 ? Component.translatable("options.off") : Component.translatable("craftography.button.timer.seconds", s.selfTimer)),
					() -> CraftographyClient.change(CameraSettings::cycleTimer));
				case TRIPOD -> BarButton.button(area, paint, () -> tooltip("key.craftography.tripod", CraftographyClient.tripodKey,
					Component.translatable(Tripod.isActive() ? "options.on" : "options.off")), Tripod::toggle);
				case MENU -> BarButton.button(area, paint, () -> tooltip("craftography.button.menu", CraftographyClient.panelKey, null),
					() -> sheetOpen = !sheetOpen);
				case SHUTTER -> BarButton.button(area, paint, () -> tooltip("key.craftography.shutter", CraftographyClient.shutterKey,
					Shutter.timerRunning() ? Component.translatable("craftography.button.shutter.cancel") : null), Shutter::press);
			});
		}
	}

	/** Tooltip lines: the name with its key, if bound, then the current value in gray. */
	private static List<Component> tooltip(String name, KeyMapping key, Component value) {
		MutableComponent title = Component.translatable(name);
		if (key != null && !key.isUnbound()) title.append(Component.literal(" [").append(key.getTranslatedKeyMessage()).append("]").withStyle(ChatFormatting.GRAY));
		List<Component> lines = new ArrayList<>();
		lines.add(title);
		if (value != null) lines.add(value.copy().withStyle(ChatFormatting.GRAY));
		return lines;
	}

	private List<Control> active() {
		if (!sheetOpen) return controls;
		List<Control> all = new ArrayList<>(sheet);
		all.addAll(controls);
		return all;
	}

	private Control at(double x, double y) {
		for (Control c : active()) {
			if (c.contains(x, y)) return c;
		}
		return null;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		if (laidOutFor != CameraSettings.get().aspect) layout();
		Control hovered = dragging != null ? dragging : at(mouseX, mouseY);
		for (Control c : active()) c.draw(g, c == hovered);
		if (dragging == null && hovered instanceof SettingRow row) {
			MutableComponent title = row.name().copy();
			List<Component> lines = new ArrayList<>(List.of(title));
			if (row.tooltip() != null) lines.add(row.tooltip().copy().withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable(row.clickSteps() ? "craftography.panel.hint.click" : "craftography.panel.hint.drag")
				.withStyle(ChatFormatting.DARK_GRAY));
			g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
		}
		if (dragging == null && hovered instanceof BarButton button) g.setComponentTooltipForNextFrame(font, button.tooltip(), mouseX, mouseY);
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
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
			CraftographyClient.onMiddleClick();
			return true;
		}
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
		if (dragging != null) dragging.release();
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
	public boolean keyPressed(KeyEvent event) {
		if (CraftographyClient.toggleKey.matches(event)) {
			CameraMode.set(false);
			onClose();
			return true;
		}
		if (!held && CraftographyClient.interactiveKey.matches(event)) {
			onClose();
			return true;
		}
		// Camera keys still work here: vanilla sends no key clicks while a screen is open.
		return CraftographyClient.runCameraKey(event) || super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (held && CraftographyClient.panelKey.matches(event)) {
			onClose();
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public void tick() {
		if (!CameraMode.isOpen() || held && !panelKeyHeld()) onClose();
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
