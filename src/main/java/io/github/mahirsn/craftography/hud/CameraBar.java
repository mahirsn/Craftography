package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.Tripod;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import io.github.mahirsn.craftography.util.TimeMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

/**
 * Row of camera body controls under the viewfinder, in the pixel-art style of Exposure's camera controls: 15x18
 * buttons with engraved icons, 1 px separators and a wide plate at each end, with the {@link DialBank} top dials
 * beside it. The HUD draws it as a status row; the controls screen makes each item clickable. Bar and dials sit in one
 * band at the bottom of the screen; the picture gets everything above {@link #bandTop}.
 */
public final class CameraBar {
	public enum Item {
		ZOOM(49), GUIDE(15), ASPECT(15), WEATHER(15), ROLL(49), SERVER_TIME(15), TIMER(15), TRIPOD(15), MENU(15), SHUTTER(21), TIME(49);

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

	/** Space between the dials, and between the dials and the bar. */
	private static final int DIAL_GAP = 2, GROUP_GAP = 6;

	public static int width() {
		int total = Item.values().length - 1;
		for (Item i : Item.values()) total += i.width;
		return total;
	}

	private static int dialsWidth() {
		return DialBank.SLOTS * DialBank.WIDTH + (DialBank.SLOTS - 1) * DIAL_GAP;
	}

	/** Whether the dials fit beside the bar; on a narrow screen they sit above it, where the time bar would be. */
	public static boolean wide(double screenWidth) {
		return dialsWidth() + GROUP_GAP + width() <= screenWidth - 8;
	}

	/** Left edge of the bar: dials and bar are centred together as one group. */
	public static double x(double screenWidth) {
		if (!wide(screenWidth)) return Math.floor((screenWidth - width()) / 2);
		return Math.floor((screenWidth - dialsWidth() - GROUP_GAP - width()) / 2) + dialsWidth() + GROUP_GAP;
	}

	/** Rectangle of one item in GUI units, when the bar is fully open. */
	public static Rect area(Item item, double screenWidth, double screenHeight) {
		double x = x(screenWidth), y = top(screenHeight);
		for (Item i : Item.values()) {
			if (i == item) return new Rect(x, y, i.width, HEIGHT);
			x += i.width + 1;
		}
		throw new IllegalArgumentException(item.name());
	}

	/** Rectangle of a top dial: left of the bar and level with its bottom, or centred above it on a narrow screen. */
	public static Rect dial(int slot, double screenWidth, double screenHeight) {
		boolean wide = wide(screenWidth);
		double x = wide ? x(screenWidth) - GROUP_GAP - dialsWidth() : Math.floor((screenWidth - dialsWidth()) / 2);
		double y = wide ? top(screenHeight) + HEIGHT - DialBank.HEIGHT : top(screenHeight) - 3 - DialBank.HEIGHT;
		return new Rect(x + slot * (DialBank.WIDTH + DIAL_GAP), y, DialBank.WIDTH, DialBank.HEIGHT);
	}

	/** Top edge of the bar. */
	public static double top(double screenHeight) {
		return Math.floor(screenHeight - HEIGHT - 2);
	}

	/** Top edge of the whole band (bar, dials and their names, time bar); the picture stays above it. */
	public static double bandTop(double screenWidth, double screenHeight) {
		return wide(screenWidth) ? top(screenHeight) - 20 : dial(0, screenWidth, screenHeight).y() - DialBank.LABEL - 3;
	}

	public static void draw(GuiGraphicsExtractor g, CameraSettings s, double mm, int dayTicks, float progress) {
		g.pose().pushMatrix();
		// Slides up from under the screen edge while camera mode opens.
		g.pose().translate(0, Math.round((1 - progress) * (HEIGHT + 4)));
		for (int i = 0; i < DialBank.SLOTS; i++) DialBank.draw(g, i, dial(i, g.guiWidth(), g.guiHeight()), false);
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
			case ROLL -> {
				Draw.camera(g, "button" + state, x, y, item.width, HEIGHT, -1);
				level(g, x + 8, y + 9, s.roll);
				engravedText(g, String.format(Locale.ROOT, "%+.1f°", s.roll), x + 16, 31, y);
				if (s.roll != 0) led(g, x + 6, y);
			}
			case MENU -> {
				Draw.camera(g, "button" + state, x, y, item.width, HEIGHT, -1);
				icon(g, "menu", x + 2, y);
				// One dot per dial page, the current one lit.
				for (int p = 0; p < DialBank.Page.values().length; p++) {
					g.fill(x + 3 + 2 * p, y + 15, x + 4 + 2 * p, y + 16, Draw.argb(1f, p == DialBank.page().ordinal() ? LED : TEXT));
				}
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
			case SERVER_TIME -> "server_time";
			case TIMER -> s.selfTimer == 0 ? "timer_off" : "timer_" + s.selfTimer;
			case TRIPOD -> "tripod";
			default -> "menu";
		};
	}

	/** An amber lamp on the button's top edge: the setting differs from the server or the default. */
	private static boolean changed(Item item, CameraSettings s) {
		return switch (item) {
			case WEATHER -> s.weather != CameraSettings.Weather.SERVER;
			case SERVER_TIME -> s.timeOfDay >= 0;
			case TIMER -> s.selfTimer > 0;
			case TRIPOD -> Tripod.isActive();
			default -> false;
		};
	}

	/**
	 * A spirit level engraved in the plate: two fixed marks for the camera's horizontal and a line that tilts with the
	 * roll, the way the picture is tilted.
	 */
	private static void level(GuiGraphicsExtractor g, int cx, int cy, double roll) {
		for (int dy = 1; dy >= 0; dy--) {
			int color = Draw.argb(1f, dy == 1 ? ENGRAVE : TEXT);
			g.fill(cx - 7, cy + dy, cx - 5, cy + dy + 1, color);
			g.fill(cx + 5, cy + dy, cx + 7, cy + dy + 1, color);
			g.pose().pushMatrix();
			g.pose().translate(cx + 0.5f, cy + dy + 0.5f);
			g.pose().rotate((float) Math.toRadians(roll));
			g.fill(-4, 0, 4, 1, color);
			g.pose().popMatrix();
		}
	}

	private static void led(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y + 2, x + 3, y + 3, Draw.argb(1f, LED));
	}

	/** An 11x9 icon in the button's icon area, engraved like Exposure's icons. */
	private static void icon(GuiGraphicsExtractor g, String name, int x, int y) {
		engravedIcon(g, name, x, y + 5);
	}

	/** An engraved 11x9 camera icon ({@code icon_<name>}) with its top-left corner at (x, y). */
	public static void engravedIcon(GuiGraphicsExtractor g, String name, int x, int y) {
		Draw.camera(g, "icon_" + name, x, y + 1, 11, 9, Draw.argb(1f, ENGRAVE));
		Draw.camera(g, "icon_" + name, x, y, 11, 9, Draw.argb(1f, TEXT));
	}

	/** Text centered in a plate's text area. */
	private static void engravedText(GuiGraphicsExtractor g, String text, int left, int width, int y) {
		Font font = Minecraft.getInstance().font;
		int x = left + (width - font.width(text)) / 2;
		g.text(font, text, x, y + 8, Draw.argb(1f, ENGRAVE), false);
		g.text(font, text, x, y + 7, Draw.argb(1f, TEXT), false);
	}
}
