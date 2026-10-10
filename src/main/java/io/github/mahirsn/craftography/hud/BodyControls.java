package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.Autofocus;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.CameraSettings.Aspect;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.Tripod;
import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.hud.BodyLayout.Part;
import io.github.mahirsn.craftography.render.LensEffects;
import io.github.mahirsn.craftography.util.ExposureMath;
import io.github.mahirsn.craftography.util.LensMath;
import io.github.mahirsn.craftography.util.TimeMath;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

/**
 * What every body control does and shows. Each control reads and writes {@link CameraSettings} directly, so the
 * HUD, the controls screen, the keys and the mouse wheel always show and change one and the same value; no
 * control keeps a copy of a setting.
 */
public final class BodyControls {
	/** Hardware mode keys select the parameters on the camcorder's rear control bank. */
	public enum Page {
		EXPOSURE("EXP", new Part[]{Part.ISO, Part.APERTURE, Part.SHUTTER_SPEED, Part.NOISE, Part.HISTOGRAM}),
		COLOUR("WB", new Part[]{Part.KELVIN, Part.TINT}),
		FOCUS("FOCUS", new Part[]{Part.FOCUS, Part.FOCUS_MODE, Part.PEAKING}),
		FRAME("FRAME", new Part[]{Part.ASPECT, Part.GUIDE, Part.LEVEL, Part.MASK, Part.GRID}),
		WORLD("TIME", new Part[]{Part.TIME, Part.WEATHER, Part.MOON, Part.SERVER_TIME, Part.LOCK}),
		DRIVE("DRIVE", new Part[]{Part.TIMER, Part.TRIPOD, Part.PREVIEW});
		public final String label;
		public final Part[] parts;
		Page(String label, Part[] parts) { this.label = label; this.parts = parts; }
	}
	private static Page page = Page.EXPOSURE;
	public static Page page() { return page; }
	public static void setPage(Page next) { page = next; }

	public enum Kind {
		/** Lens ring: drag sideways or scroll; double-click resets. */
		RING,
		/** Top-plate dial: drag sideways or scroll; double-click resets. */
		DIAL,
		/** Thumb wheel: drag sideways or scroll; double-click resets. */
		WHEEL,
		/** Push button: click (or scroll) runs it. */
		BUTTON,
		/** Two-position lever: click flips it. */
		SWITCH,
		/** Shutter release. */
		RELEASE,
		/** Read-only status display. */
		DISPLAY
	}

	/**
	 * @param step  scroll or drag steps (positive = up/right = higher value), null for buttons
	 * @param click click action, null for dials and rings
	 * @param reset double-click action
	 * @param lit   whether a button's lamp is on (the setting differs from its default)
	 */
	public record Control(Part part, Kind kind, String name, Supplier<KeyMapping> key, Supplier<Component> value, Supplier<String> icon,
						  DoubleConsumer step, Runnable click, Runnable reset, BooleanSupplier lit) {
	}

	private static final Map<Part, Control> CONTROLS = new EnumMap<>(Part.class);

	private static CameraSettings s() {
		return CameraSettings.get();
	}

	private static void change(Consumer<CameraSettings> c) {
		CraftographyClient.change(c);
	}

	private static int whole(double steps) {
		return (int) Math.signum(steps) * Math.max(1, (int) Math.abs(Math.round(steps)));
	}

	private static void adjust(Part part, String name, Supplier<KeyMapping> key, Kind kind, Supplier<Component> value, Supplier<String> icon,
							   DoubleConsumer step, Runnable reset) {
		CONTROLS.put(part, new Control(part, kind, name, key, value, icon, step, null, reset, () -> false));
	}

	private static void button(Part part, String name, Supplier<KeyMapping> key, Supplier<Component> value, Supplier<String> icon, Runnable click, BooleanSupplier lit) {
		CONTROLS.put(part, new Control(part, Kind.BUTTON, name, key, value, icon, null, click, click, lit));
	}

	private static Component text(String s) {
		return Component.literal(s);
	}

	static {
		adjust(Part.ZOOM, "craftography.control.zoom", null, Kind.RING, () -> text(Math.round(s().focalLength) + " mm"), null,
			steps -> change(c -> c.focalLength = LensMath.zoom(c.focalLength, CraftographyClient.shiftDown() ? steps / 4 : steps)),
			() -> change(c -> c.focalLength = LensMath.DEFAULT_MM));
		adjust(Part.FOCUS, "craftography.control.focus", null, Kind.RING, BodyControls::focusText, null,
			// Turning the focus ring switches to manual focus at the distance the lens is at now.
			steps -> change(c -> {
				double from = c.autofocus ? Autofocus.distance() : c.focusDistance;
				if (from >= ExposureMath.FOCUS_INFINITY && steps < 0) from = 100;
				double to = from * Math.pow(1.2, steps);
				c.focusDistance = to > 100 ? ExposureMath.FOCUS_INFINITY : Math.max(ExposureMath.MIN_FOCUS, to);
				c.autofocus = false;
			}),
			() -> change(c -> c.autofocus = true));
		adjust(Part.APERTURE, "craftography.control.aperture", null, Kind.RING, () -> text(ExposureMath.formatAperture(s().aperture)), null,
			steps -> change(c -> c.aperture = ExposureMath.step(ExposureMath.APERTURE, c.aperture, whole(steps))),
			() -> change(c -> c.aperture = ExposureMath.DEFAULT_APERTURE));
		adjust(Part.ISO, "craftography.control.iso", null, Kind.DIAL, () -> text("ISO " + s().iso), null,
			steps -> change(c -> c.iso = ExposureMath.step(ExposureMath.ISO, c.iso, whole(steps))),
			() -> change(c -> c.iso = ExposureMath.DEFAULT_ISO));
		adjust(Part.SHUTTER_SPEED, "craftography.control.shutter_speed", null, Kind.DIAL, () -> text(ExposureMath.formatShutter(s().shutter)), null,
			steps -> change(c -> c.shutter = ExposureMath.step(ExposureMath.SHUTTER, c.shutter, whole(steps))),
			() -> change(c -> c.shutter = ExposureMath.DEFAULT_SHUTTER));
		adjust(Part.TIME, "craftography.control.time", () -> CraftographyClient.serverTimeKey, Kind.DIAL,
			() -> text(TimeMath.format(ViewfinderHud.visibleDayTicks(s())) + (s().timeOfDay < 0 ? " (server)" : "")), null,
			steps -> change(c -> c.timeOfDay = TimeMath.dayTicks(ViewfinderHud.visibleDayTicks(c) + Math.round(steps * (CraftographyClient.shiftDown() ? 10 : 100)))),
			() -> change(c -> c.timeOfDay = -1));
		CONTROLS.put(Part.SHUTTER, new Control(Part.SHUTTER, Kind.RELEASE, "key.craftography.shutter", () -> CraftographyClient.shutterKey,
			() -> Shutter.timerRunning() ? Component.translatable("craftography.control.shutter.cancel") : null, null, null, Shutter::press, Shutter::press, Shutter::timerRunning));
		CONTROLS.put(Part.TOP_LCD, new Control(Part.TOP_LCD, Kind.DISPLAY, "craftography.control.top_lcd", null,
			() -> LensEffects.depthOfFieldAvailable() ? null : Component.translatable("craftography.dof.off"), null, null, null, () -> {
		}, () -> false));

		button(Part.GUIDE, "craftography.panel.guide", () -> CraftographyClient.guideKey, () -> Component.translatable(s().guide.translationKey()),
			() -> "guide_" + s().guide.name().toLowerCase(Locale.ROOT), () -> change(CameraSettings::cycleGuide), () -> false);
		button(Part.ASPECT, "craftography.panel.aspect", () -> CraftographyClient.aspectKey,
			() -> s().aspect == Aspect.FREE ? Component.translatable("craftography.aspect.free") : text(s().aspect.label),
			() -> "aspect_" + (s().aspect == Aspect.FREE ? "free" : s().aspect.name().substring(1)), () -> change(CameraSettings::cycleAspect), () -> false);
		button(Part.WEATHER, "craftography.panel.weather", () -> CraftographyClient.weatherKey, () -> Component.translatable(s().weather.translationKey()),
			() -> "weather_" + s().weather.name().toLowerCase(Locale.ROOT), () -> change(CameraSettings::cycleWeather),
			() -> s().weather != CameraSettings.Weather.SERVER);
		button(Part.MOON, "craftography.panel.moon", null,
			() -> Component.translatable(s().moonPhase < 0 ? "craftography.moon.server" : "craftography.moon." + s().moonPhase),
			() -> s().moonPhase < 0 ? "weather_server" : "moon_" + s().moonPhase, () -> change(c -> c.moonPhase = c.moonPhase >= 7 ? -1 : c.moonPhase + 1),
			() -> s().moonPhase >= 0);
		button(Part.TIMER, "craftography.button.timer", () -> CraftographyClient.timerKey,
			() -> s().selfTimer == 0 ? Component.translatable("options.off") : Component.translatable("craftography.button.timer.seconds", s().selfTimer),
			() -> s().selfTimer == 0 ? "timer_off" : "timer_" + s().selfTimer, () -> change(CameraSettings::cycleTimer), () -> s().selfTimer > 0);
		button(Part.TRIPOD, "key.craftography.tripod", () -> CraftographyClient.tripodKey,
			() -> Component.translatable(Tripod.isActive() ? "options.on" : "options.off"), () -> "tripod", Tripod::toggle, Tripod::isActive);
		button(Part.SERVER_TIME, "key.craftography.server_time", () -> CraftographyClient.serverTimeKey, null, () -> "server_time",
			() -> change(c -> c.timeOfDay = -1), () -> s().timeOfDay >= 0);

		adjust(Part.KELVIN, "craftography.control.kelvin", null, Kind.WHEEL, () -> text(s().kelvin + " K"), () -> "kelvin",
			steps -> change(c -> c.kelvin = Math.clamp(c.kelvin + Math.round(steps * (CraftographyClient.shiftDown() ? 10 : 100)), ExposureMath.MIN_KELVIN, ExposureMath.MAX_KELVIN)),
			() -> change(c -> c.kelvin = ExposureMath.DEFAULT_KELVIN));
		adjust(Part.TINT, "craftography.control.tint", null, Kind.WHEEL, BodyControls::tintText, () -> "tint",
			steps -> change(c -> c.tint = (int) Math.clamp(c.tint + Math.round(steps * (CraftographyClient.shiftDown() ? 1 : 5)), -ExposureMath.MAX_TINT, ExposureMath.MAX_TINT)),
			() -> change(c -> c.tint = 0));
		CONTROLS.put(Part.FOCUS_MODE, new Control(Part.FOCUS_MODE, Kind.SWITCH, "craftography.control.focus_mode", null,
			() -> Component.translatable(s().autofocus ? "craftography.focus.auto" : "craftography.focus.manual"), null, null,
			() -> change(c -> {
				// Switching to manual keeps the distance the autofocus found.
				if (c.autofocus) c.focusDistance = Autofocus.distance();
				c.autofocus = !c.autofocus;
			}), () -> change(c -> c.autofocus = true), () -> !s().autofocus));
		button(Part.HISTOGRAM, "craftography.control.histogram", null, () -> Component.translatable(s().histogram ? "options.on" : "options.off"),
			() -> "histogram", () -> change(c -> c.histogram = !c.histogram), () -> s().histogram);
		button(Part.PEAKING, "craftography.control.peaking", null, () -> Component.translatable(s().peaking ? "options.on" : "options.off"),
			() -> "peaking", () -> change(c -> c.peaking = !c.peaking), () -> s().peaking);
		button(Part.NOISE, "craftography.control.noise", null, () -> Component.translatable(s().noise ? "options.on" : "options.off"),
			() -> "noise", () -> change(c -> c.noise = !c.noise), () -> !s().noise);
		button(Part.LEVEL, "key.craftography.reset_roll", () -> CraftographyClient.resetRollKey,
			() -> text(String.format(Locale.ROOT, "%+.1f°", s().roll)), () -> "reset_roll", () -> change(c -> c.roll = 0), () -> s().roll != 0);
		button(Part.LOCK, "craftography.panel.lock", null, () -> Component.translatable(s().lockTimeWeather ? "options.on" : "options.off"),
			() -> "lock", () -> change(c -> c.lockTimeWeather = !c.lockTimeWeather), () -> s().lockTimeWeather);
		button(Part.PREVIEW, "craftography.panel.preview", null, () -> Component.translatable(s().showPreview ? "options.on" : "options.off"),
			() -> "preview", () -> change(c -> c.showPreview = !c.showPreview), () -> !s().showPreview);
		adjust(Part.MASK, "craftography.panel.mask_opacity", null, Kind.WHEEL, () -> text(Math.round(s().maskOpacity * 100) + "%"), () -> "mask",
			steps -> change(c -> c.maskOpacity = (float) Math.clamp(c.maskOpacity + steps * 0.05, 0, 1)), () -> change(c -> c.maskOpacity = 0.75f));
		adjust(Part.GRID, "craftography.panel.guide_opacity", null, Kind.WHEEL, () -> text(Math.round(s().guideOpacity * 100) + "%"), () -> "grid",
			steps -> change(c -> c.guideOpacity = (float) Math.clamp(c.guideOpacity + steps * 0.05, 0, 1)), () -> change(c -> c.guideOpacity = 0.45f));
	}

	private static Component focusText() {
		String d = ExposureMath.formatFocus(Autofocus.distance());
		return Component.translatable(s().autofocus ? "craftography.focus.auto_at" : "craftography.focus.manual_at", d);
	}

	private static Component tintText() {
		int t = s().tint;
		return text(t == 0 ? "0" : (t > 0 ? "M" : "G") + Math.abs(t));
	}

	public static Control get(Part part) {
		return CONTROLS.get(part);
	}

	public static Iterable<Control> all() {
		java.util.List<Control> visible = new java.util.ArrayList<>();
		for (Part p : new Part[]{Part.ZOOM, Part.SHUTTER, Part.TOP_LCD}) visible.add(get(p));
		for (Part p : page.parts) visible.add(get(p));
		return visible;
	}

	private BodyControls() {
	}
}
