package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import io.github.mahirsn.craftography.util.TimeMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

/**
 * Row of camera body controls under the viewfinder, in the pixel-art style of Exposure's camera controls: 15x18
 * buttons with engraved icons, 1 px separators and a wide plate at each end. The HUD draws it as a status row; the
 * controls screen makes each item clickable.
 */
public final class CameraBar {
	public enum Item {
		ZOOM(49), GUIDE(15), ASPECT(15), WEATHER(15), RESET_ROLL(15), SERVER_TIME(15), MENU(15), SHUTTER(21), TIME(49);

		public final int width;

		Item(int width) {
			this.width = width;
		}
	}

	public static final int HEIGHT = 18;
	/** Exposure's viewfinder font colors: dark text with a light line under it, so it looks engraved. */
	private static final int TEXT = 0x2B2622, ENGRAVE = 0x7A736C;
	private static final int LED = 0xFFB000;

	private CameraBar() {
	}

	private static int totalWidth() {
		int total = Item.values().length - 1;
		for (Item i : Item.values()) total += i.width;
		return total;
	}

	/** Rectangle of one item in GUI units, when the bar is fully open. */
	public static Rect area(Item item, double screenWidth, double screenHeight) {
		double x = Math.floor((screenWidth - totalWidth()) / 2), y = Math.floor(screenHeight - HEIGHT - 2);
		for (Item i : Item.values()) {
			if (i == item) return new Rect(x, y, i.width, HEIGHT);
			x += i.width + 1;
		}
		throw new IllegalArgumentException(item.name());
	}

	/** Top edge of the bar; elements above it keep clear of this line. */
	public static double top(double screenHeight) {
		return Math.floor(screenHeight - HEIGHT - 2);
	}

	public static void draw(GuiGraphicsExtractor g, CameraSettings s, double mm, int dayTicks, float progress) {
		g.pose().pushMatrix();
		// Slides up from under the screen edge while camera mode opens.
		g.pose().translate(0, Math.round((1 - progress) * (HEIGHT + 4)));
		Item[] items = Item.values();
		for (int i = 0; i < items.length; i++) {
			drawItem(g, items[i], s, mm, dayTicks, false);
			if (i < items.length - 1) {
				Rect r = area(items[i], g.guiWidth(), g.guiHeight());
				Draw.camera(g, "separator", r.right(), r.y(), 1, HEIGHT, -1);
			}
		}
		g.pose().popMatrix();
	}

	public static void drawItem(GuiGraphicsExtractor g, Item item, CameraSettings s, double mm, int dayTicks, boolean hovered) {
		Rect r = area(item, g.guiWidth(), g.guiHeight());
		int x = (int) r.x(), y = (int) r.y();
		String state = hovered ? "_highlighted" : "";
		switch (item) {
			case ZOOM -> {
				Draw.camera(g, "plate_left" + state, x, y, item.width, HEIGHT, -1);
				engravedText(g, Math.round(mm) + "mm", x + 17, 29, y);
			}
			case TIME -> {
				Draw.camera(g, "plate_right" + state, x, y, item.width, HEIGHT, -1);
				icon(g, "clock", x + 2, y);
				engravedText(g, TimeMath.format(dayTicks), x + 15, 27, y);
				if (s.timeOfDay >= 0) led(g, x + 6, y);
			}
			case SHUTTER -> {
				Draw.camera(g, "shutter_button" + state, x, y, item.width, HEIGHT, -1);
				icon(g, "shutter", x + 5, y);
			}
			default -> {
				Draw.camera(g, "button" + state, x, y, item.width, HEIGHT, -1);
				icon(g, icon(item, s), x + 2, y);
				if (changed(item, s)) led(g, x + 6, y);
			}
		}
	}

	private static String icon(Item item, CameraSettings s) {
		return switch (item) {
			case GUIDE -> "guide_" + s.guide.name().toLowerCase(Locale.ROOT);
			case ASPECT -> "aspect_" + (s.aspect == CameraSettings.Aspect.FREE ? "free" : s.aspect.name().substring(1));
			case WEATHER -> "weather_" + s.weather.name().toLowerCase(Locale.ROOT);
			case RESET_ROLL -> "reset_roll";
			case SERVER_TIME -> "server_time";
			default -> "menu";
		};
	}

	/** An amber lamp on the button's top edge: the setting differs from the server or the default. */
	private static boolean changed(Item item, CameraSettings s) {
		return switch (item) {
			case WEATHER -> s.weather != CameraSettings.Weather.SERVER;
			case RESET_ROLL -> s.roll != 0;
			case SERVER_TIME -> s.timeOfDay >= 0;
			default -> false;
		};
	}

	private static void led(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y + 2, x + 3, y + 3, Draw.argb(1f, LED));
	}

	/** An 11x9 icon in the button's icon area, engraved like Exposure's icons. */
	private static void icon(GuiGraphicsExtractor g, String name, int x, int y) {
		Draw.camera(g, "icon_" + name, x, y + 6, 11, 9, Draw.argb(1f, ENGRAVE));
		Draw.camera(g, "icon_" + name, x, y + 5, 11, 9, Draw.argb(1f, TEXT));
	}

	/** Text centered in a plate's text area. */
	private static void engravedText(GuiGraphicsExtractor g, String text, int left, int width, int y) {
		Font font = Minecraft.getInstance().font;
		int x = left + (width - font.width(text)) / 2;
		g.text(font, text, x, y + 8, Draw.argb(1f, ENGRAVE), false);
		g.text(font, text, x, y + 7, Draw.argb(1f, TEXT), false);
	}
}
