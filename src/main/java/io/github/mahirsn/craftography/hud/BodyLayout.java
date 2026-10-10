package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.util.Rect;

import java.util.EnumMap;
import java.util.Map;

/**
 * Where every part of the camera body sits on a screen of a given size, in GUI units. The camera is seen from
 * behind and a little above: the lens barrel shows beyond the top plate, the rear LCD shows the live view.
 * One place computes it; the HUD draws from it, the controls screen hit-tests with it and the shutter crops
 * with its LCD rectangle.
 */
public final class BodyLayout {
	/** Every control on the body, in drawing order. */
	public enum Part {
		ZOOM, FOCUS, APERTURE, TIME, ISO, SHUTTER_SPEED, SHUTTER, TOP_LCD,
		GUIDE, ASPECT, WEATHER, MOON, TIMER, TRIPOD, SERVER_TIME,
		KELVIN, TINT, FOCUS_MODE, HISTOGRAM, PEAKING, NOISE, LEVEL, LOCK, PREVIEW, MASK, GRID
	}

	public static final int RING = 11, LENS_FRONT = 8, MOUNT = 5, TOP = 22, BEZEL = 8;
	public static final int LEFT = 38, RIGHT = 74, GRIP = 18;
	public static final int BUTTON_W = 20, BUTTON_H = 16, DIAL_W = 36, DIAL_H = 22, WHEEL_H = 10, CONTROL_W = 42;

	public final double width, height;
	public final Rect body, grip, topPlate, hump, lens, mount, lcd;
	private final Map<Part, Rect> parts = new EnumMap<>(Part.class);

	private BodyLayout(double width, double height) {
		this.width = width;
		this.height = height;
		int w = (int) width, h = (int) height;
		int lensW = Math.clamp(Math.round(w * 0.32f), 120, 190);
		int topY = 2 + LENS_FRONT + 3 * RING + MOUNT + 4;
		topPlate = new Rect(4, topY, w - 8, TOP);
		hump = new Rect((w - 64) / 2, topY - 4, 64, 28);
		body = new Rect(4, topY + TOP - 4, w - 8, h - 3 - (topY + TOP - 4));
		grip = new Rect(w - 4 - GRIP, body.y(), GRIP, body.h());

		// Lens rings from the body outwards (upwards on screen), each a little narrower: further away.
		int mountY = topY - 4 - MOUNT;
		mount = new Rect((w - lensW) / 2, mountY, lensW, MOUNT);
		put(Part.APERTURE, centred(w, lensW - 4, mountY - RING, RING));
		put(Part.FOCUS, centred(w, lensW - 8, mountY - 2 * RING, RING));
		put(Part.ZOOM, centred(w, lensW - 12, mountY - 3 * RING, RING));
		lens = centred(w, lensW - 16, mountY - 3 * RING - LENS_FRONT, LENS_FRONT);

		// Top plate: time and ISO dials left of the viewfinder hump, shutter speed dial and status LCD right of it.
		int dialY = topY - 8;
		Rect iso = new Rect(hump.x() - 6 - DIAL_W, dialY, DIAL_W, DIAL_H);
		put(Part.ISO, iso);
		put(Part.TIME, new Rect(iso.x() - 4 - DIAL_W, dialY, DIAL_W, DIAL_H));
		Rect speed = new Rect(hump.right() + 6, dialY, DIAL_W, DIAL_H);
		put(Part.SHUTTER_SPEED, speed);
		Rect release = new Rect(w - 4 - GRIP / 2.0 - 10, topY - 6, 20, 14);
		put(Part.SHUTTER, release);
		put(Part.TOP_LCD, new Rect(speed.right() + 5, topY + 1, release.x() - 4 - (speed.right() + 5), 20));

		// Rear LCD between the button column and the thumb area.
		lcd = new Rect(4 + LEFT, body.y() + BEZEL + 4, w - 8 - LEFT - RIGHT, body.bottom() - 6 - BEZEL - (body.y() + BEZEL + 4));

		// Left column: seven buttons spread along the LCD.
		Part[] left = {Part.GUIDE, Part.ASPECT, Part.WEATHER, Part.MOON, Part.TIMER, Part.TRIPOD, Part.SERVER_TIME};
		double span = lcd.h() + 2 * BEZEL - BUTTON_H, x = 4 + (LEFT - BEZEL - BUTTON_W) / 2.0 + 1;
		for (int i = 0; i < left.length; i++) {
			put(left[i], new Rect(Math.round(x), Math.round(lcd.y() - BEZEL + span * i / (left.length - 1)), BUTTON_W, BUTTON_H));
		}

		// Right column (thumb area beside the grip): wheels, the focus mode lever and a 2-wide grid of buttons.
		double cx = lcd.right() + BEZEL + 4, top = lcd.y() - BEZEL, bottom = lcd.bottom() + BEZEL;
		Object[][] rows = {
			{Part.KELVIN, WHEEL_H}, {Part.TINT, WHEEL_H}, {Part.FOCUS_MODE, 12},
			{new Part[]{Part.HISTOGRAM, Part.PEAKING}, BUTTON_H}, {new Part[]{Part.NOISE, Part.LEVEL}, BUTTON_H}, {new Part[]{Part.LOCK, Part.PREVIEW}, BUTTON_H},
			{Part.MASK, WHEEL_H}, {Part.GRID, WHEEL_H}};
		int total = 0;
		for (Object[] row : rows) total += (int) row[1];
		double gap = Math.max(2, (bottom - top - total) / (rows.length - 1));
		double y = top;
		for (Object[] row : rows) {
			int rh = (int) row[1];
			if (row[0] instanceof Part[] pair) {
				put(pair[0], new Rect(cx, Math.round(y), BUTTON_W, rh));
				put(pair[1], new Rect(cx + BUTTON_W + 2, Math.round(y), BUTTON_W, rh));
			} else {
				put((Part) row[0], new Rect(cx, Math.round(y), CONTROL_W, rh));
			}
			y += rh + gap;
		}
	}

	private static Rect centred(int screenWidth, int w, int y, int h) {
		return new Rect((screenWidth - w) / 2, y, w, h);
	}

	private void put(Part part, Rect area) {
		parts.put(part, area);
	}

	public Rect get(Part part) {
		return parts.get(part);
	}

	private static BodyLayout last;

	/** Layout for a screen size; the last one is kept, since the size rarely changes. */
	public static BodyLayout of(double width, double height) {
		BodyLayout l = last;
		if (l == null || l.width != width || l.height != height) last = l = new BodyLayout(width, height);
		return l;
	}
}
