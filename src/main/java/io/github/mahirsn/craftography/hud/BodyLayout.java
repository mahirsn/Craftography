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
		lcd = rect(76, 106, 244, 137);
		body = rect(53, 31, 385, 222);
		grip = rect(422, 197, 20, 73);
		parts.put(Part.ZOOM, rect(108, 52, 32, 32));
		parts.put(Part.SHUTTER, rect(394, 77, 20, 17));
		parts.put(Part.TOP_LCD, rect(277, 73, 45, 13));
		// Each page reuses the DSLR's top encoders, large rear wheel and lower buttons.
		double[][] hardware = {{338, 110, 31, 29}, {344, 158, 52, 52}, {384, 113, 31, 29}, {343, 223, 28, 22}, {386, 223, 28, 22}};
		for (BodyControls.Page page : BodyControls.Page.values()) {
			Part[] row = page.parts;
			for (int i = 0; i < row.length; i++) parts.put(row[i], rect(hardware[i][0], hardware[i][1], hardware[i][2], hardware[i][3]));
		}
	}
	public Rect rect(double dx, double dy, double w, double h) {
		return new Rect(x + dx * scale, y + dy * scale, w * scale, h * scale);
	}
	public Rect pageButton(int index) { return rect(76 + index * 41, 87, 40, 14); }
	public Rect get(Part part) { return parts.get(part); }
	public static BodyLayout of(double width, double height) { return new BodyLayout(width, height); }
}
