package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.ExposureMath;
import io.github.mahirsn.craftography.util.Rect;
import io.github.mahirsn.craftography.util.TimeMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * The three top dials beside the camera bar, like the dials on a camera's top plate. They have pages: exposure (ISO,
 * shutter, aperture) first, then colour, focus, view and scene; the bar's menu button turns to the next page. Each
 * dial shows its value on the top face and its name above it, amber while the setting is off its default.
 */
public final class DialBank {
	public static final int SLOTS = 3, WIDTH = 36, HEIGHT = 22;
	/** How far above the dial its name is printed: level with the time bar's hour labels beside it. */
	public static final int LABEL = 14;

	/**
	 * One dial. {@code text} is the value on the top face, or null when {@code sprite} (a viewfinder sprite) shows it.
	 * {@code step} gets steps (positive = next / more) from drag and scroll. {@code reset} runs on double-click. A dial
	 * without a reset holds a choice or a switch: a click steps it once, and so does a double-click, so quick clicks
	 * keep cycling.
	 */
	public record Dial(String id, Supplier<String> text, Supplier<String> sprite, DoubleConsumer step, Runnable reset,
					   BooleanSupplier changed, Supplier<Component> words) {
		public Component label() {
			return Component.translatable("craftography.dial." + id);
		}

		public Component name() {
			return Component.translatable("craftography.dial." + id + ".name");
		}
	}

	public enum Page {
		EXPOSURE, COLOUR, FOCUS, VIEW, SCENE, PHOTO;

		public Component title() {
			return Component.translatable("craftography.dial.page." + name().toLowerCase(java.util.Locale.ROOT));
		}
	}

	private static final Dial[][] PAGES = {
		{
			value("iso", () -> fit(s().iso),
				n -> s().iso = ExposureMath.step(ExposureMath.ISO, s().iso, whole(n)),
				() -> s().iso = ExposureMath.DEFAULT_ISO, () -> s().iso != ExposureMath.DEFAULT_ISO, () -> "ISO " + s().iso),
			// The dial shows the denominator, as shutter speed dials do; the tooltip has the full speed.
			value("shutter", () -> ExposureMath.formatShutter(s().shutter).replace("1/", ""),
				n -> s().shutter = ExposureMath.step(ExposureMath.SHUTTER, s().shutter, whole(n)), () -> s().shutter = ExposureMath.DEFAULT_SHUTTER,
				() -> ExposureMath.nearest(ExposureMath.SHUTTER, s().shutter) != ExposureMath.nearest(ExposureMath.SHUTTER, ExposureMath.DEFAULT_SHUTTER),
				() -> ExposureMath.formatShutter(s().shutter)),
			value("aperture", () -> ExposureMath.formatAperture(s().aperture),
				n -> s().aperture = ExposureMath.step(ExposureMath.APERTURE, s().aperture, whole(n)), () -> s().aperture = ExposureMath.DEFAULT_APERTURE,
				() -> ExposureMath.nearest(ExposureMath.APERTURE, s().aperture) != ExposureMath.nearest(ExposureMath.APERTURE, ExposureMath.DEFAULT_APERTURE)),
		},
		{
			value("white_balance", () -> fit(s().kelvin),
				n -> s().kelvin = (int) Math.clamp(s().kelvin + Math.round(n * (fine() ? 50 : 200)), ExposureMath.MIN_KELVIN, ExposureMath.MAX_KELVIN),
				() -> s().kelvin = ExposureMath.DEFAULT_KELVIN, () -> s().kelvin != ExposureMath.DEFAULT_KELVIN, () -> s().kelvin + "K"),
			value("tint", () -> (s().tint > 0 ? "M" : s().tint < 0 ? "G" : "") + Math.abs(s().tint),
				n -> s().tint = (int) Math.clamp(s().tint + Math.round(n * (fine() ? 1 : 5)), -ExposureMath.MAX_TINT, ExposureMath.MAX_TINT),
				() -> s().tint = 0, () -> s().tint != 0),
			toggle("grain", () -> s().noise, v -> s().noise = v, true),
		},
		{
			value("focus", () -> s().autofocus ? "AF" : ExposureMath.formatFocus(s().focusDistance),
				n -> {
					s().autofocus = false;
					double d = Math.clamp(s().focusDistance * Math.pow(1.15, fine() ? n / 4 : n), ExposureMath.MIN_FOCUS, ExposureMath.FOCUS_INFINITY);
					// Turning past the last step lands on infinity, not on a few millimetres short of it.
					s().focusDistance = d >= ExposureMath.FOCUS_INFINITY - 0.5 ? ExposureMath.FOCUS_INFINITY : d;
				}, () -> s().autofocus = true, () -> !s().autofocus),
			toggle("autofocus", () -> s().autofocus, v -> s().autofocus = v, true),
			toggle("peaking", () -> s().peaking, v -> s().peaking = v, false),
		},
		{
			percent("mask", () -> s().maskOpacity, v -> s().maskOpacity = v, 0.75f),
			percent("lines", () -> s().guideOpacity, v -> s().guideOpacity = v, 0.45f),
			toggle("histogram", () -> s().histogram, v -> s().histogram = v, false),
		},
		{
			new Dial("moon", null, () -> s().moonPhase < 0 ? "weather_server" : "moon_" + s().moonPhase,
				n -> s().moonPhase = Math.floorMod(s().moonPhase + 1 + whole(n), TimeMath.MOON_PHASES + 1) - 1, null,
				() -> s().moonPhase >= 0,
				() -> Component.translatable(s().moonPhase < 0 ? "craftography.moon.server" : "craftography.moon." + s().moonPhase)),
			toggle("keep", () -> s().lockTimeWeather, v -> s().lockTimeWeather = v, false),
			new Dial("weather", () -> Component.translatable(s().weather.translationKey() + ".short").getString(), null,
				n -> { for (int i = 0; i < Math.floorMod(whole(n), CameraSettings.Weather.values().length); i++) s().cycleWeather(); }, null,
				() -> s().weather != CameraSettings.Weather.SERVER, () -> Component.translatable(s().weather.translationKey())),
		},
		{
			// Photo size: the screen's frame, or the world rendered again at a multiple of it, up to 8K.
			new Dial("size", () -> s().photoScale == 1 ? "1x" : s().photoScale + "x", null,
				n -> s().photoScale = Math.clamp(s().photoScale + whole(n), 1, CameraSettings.MAX_PHOTO_SCALE),
				() -> s().photoScale = 1, () -> s().photoScale != 1, () -> Component.literal(io.github.mahirsn.craftography.capture.Shutter.photoSizeText())),
			new Dial("timer", () -> s().selfTimer == 0 ? Component.translatable("craftography.dial.off").getString() : s().selfTimer + "s", null,
				n -> s().cycleTimer(), null, () -> s().selfTimer != 0,
				() -> s().selfTimer == 0 ? Component.translatable("options.off") : Component.translatable("craftography.button.timer.seconds", s().selfTimer)),
			toggle("preview", () -> s().showPreview, v -> s().showPreview = v, true),
		},
	};

	private static int page;

	private DialBank() {
	}

	private static CameraSettings s() {
		return CameraSettings.get();
	}

	private static boolean fine() {
		return CraftographyClient.shiftDown();
	}

	/** Whole steps from a scroll or drag amount; a small trackpad scroll still moves one step. */
	private static int whole(double steps) {
		return (int) Math.signum(steps) * Math.max(1, (int) Math.abs(Math.round(steps)));
	}

	private static Dial value(String id, Supplier<String> text, DoubleConsumer step, Runnable reset, BooleanSupplier changed) {
		return value(id, text, step, reset, changed, text);
	}

	private static Dial value(String id, Supplier<String> text, DoubleConsumer step, Runnable reset, BooleanSupplier changed, Supplier<String> words) {
		return new Dial(id, text, null, step, reset, changed, () -> Component.literal(words.get()));
	}

	/** The top face holds about 27 px of text: five-digit numbers (ISO 12800, 10000 K) are shown in thousands. */
	private static String fit(int v) {
		return v < 10000 ? String.valueOf(v) : String.format(java.util.Locale.ROOT, v % 1000 == 0 ? "%.0fK" : "%.1fK", v / 1000.0);
	}

	private static Component onOff(boolean on) {
		return Component.translatable(on ? "options.on" : "options.off");
	}

	/**
	 * An on/off dial: each click or scroll notch flips it. The face uses the mod's own short words, since some
	 * languages' "off" (Turkish "KAPALI") is wider than the face; the tooltip keeps the game's word.
	 */
	private static Dial toggle(String id, BooleanSupplier get, Consumer<Boolean> set, boolean def) {
		return new Dial(id, () -> Component.translatable(get.getAsBoolean() ? "craftography.dial.on" : "craftography.dial.off").getString(), null,
			n -> set.accept(!get.getAsBoolean()), null, () -> get.getAsBoolean() != def, () -> onOff(get.getAsBoolean()));
	}

	private interface FloatSetter {
		void set(float v);
	}

	private static Dial percent(String id, DoubleSupplier get, FloatSetter set, float def) {
		return value(id, () -> Math.round(get.getAsDouble() * 100) + "%",
			n -> set.set((float) Math.clamp(get.getAsDouble() + n * (fine() ? 0.01 : 0.05), 0, 1)),
			() -> set.set(def), () -> Math.abs(get.getAsDouble() - def) > 0.001);
	}

	public static Page page() {
		return Page.values()[page];
	}

	public static void nextPage() {
		page = (page + 1) % PAGES.length;
	}

	/** The dial in a slot on the current page. */
	public static Dial get(int slot) {
		return PAGES[page][slot];
	}

	public static void draw(GuiGraphicsExtractor g, int slot, Rect r, boolean hovered) {
		Dial d = get(slot);
		Font font = Minecraft.getInstance().font;
		int x = (int) r.x(), y = (int) r.y();
		Draw.camera(g, hovered ? "dial_highlighted" : "dial", x, y, WIDTH, HEIGHT, -1);
		// The value is engraved on the top face, as on the old camera's top plate.
		if (d.sprite() != null) {
			String name = d.sprite().get();
			Draw.sprite(g, name, x + 13, y + 2, 9, 9, Draw.argb(1f, Draw.PRINT_LIGHT));
			Draw.sprite(g, name, x + 13, y + 1, 9, 9, Draw.argb(1f, Draw.PRINT));
		} else {
			String text = d.text().get();
			Draw.engraved(g, text, x + (WIDTH - font.width(text)) / 2.0, y + 2);
		}
		// The name above the dial, printed like the time bar's hour labels and on their line.
		String label = d.label().getString();
		boolean changed = d.changed().getAsBoolean();
		g.text(font, label, x + (WIDTH - font.width(label)) / 2, y - LABEL, Draw.argb(changed ? 1f : 0.7f, changed ? InfoStrip.AMBER : InfoStrip.WHITE), false);
	}
}
