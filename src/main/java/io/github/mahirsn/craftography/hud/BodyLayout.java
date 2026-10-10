package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.util.Rect;
import java.util.EnumMap;
import java.util.Map;

/** Shared geometry of the handheld camera rear, its LCD and physical controls. */
public final class BodyLayout {
	public enum Part {
		ZOOM, FOCUS, APERTURE, TIME, ISO, SHUTTER_SPEED, SHUTTER, TOP_LCD,
		GUIDE, ASPECT, WEATHER, MOON, TIMER, TRIPOD, SERVER_TIME,
		KELVIN, TINT, FOCUS_MODE, HISTOGRAM, PEAKING, NOISE, LEVEL, LOCK, PREVIEW, MASK, GRID
	}
	public static final int DESIGN_WIDTH = 480, DESIGN_HEIGHT = 270;
	public final double width, height, scale, x, y;
	public final Rect lcd, body, grip;
	private final Map<Part, Rect> parts = new EnumMap<>(Part.class);

	private BodyLayout(double width, double height) {
		this.width = width; this.height = height;
		scale = Math.min(width / DESIGN_WIDTH, height / DESIGN_HEIGHT);
		x = (width - DESIGN_WIDTH * scale) / 2;
		y = (height - DESIGN_HEIGHT * scale) / 2;
		lcd = rect(92, 100, 224, 114);
		body = rect(78, 47, 321, 207);
		grip = rect(374, 169, 30, 101);
		parts.put(Part.ZOOM, rect(210, 68, 54, 14));
		parts.put(Part.SHUTTER, rect(280, 65, 20, 17));
		parts.put(Part.TOP_LCD, rect(331, 64, 47, 12));
		// Each page shares the same five physical control positions on the rear body.
		for (BodyControls.Page page : BodyControls.Page.values()) {
			Part[] row = page.parts;
			for (int i = 0; i < row.length; i++) parts.put(row[i], rect(334, 77 + 35 * i, 39, 31));
		}
	}
	public Rect rect(double dx, double dy, double w, double h) {
		return new Rect(x + dx * scale, y + dy * scale, w * scale, h * scale);
	}
	public Rect pageButton(int index) { return rect(91 + index * 37, 227, 32, 14); }
	public Rect get(Part part) { return parts.get(part); }
	public static BodyLayout of(double width, double height) { return new BodyLayout(width, height); }
}
