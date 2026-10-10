package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.render.HeldCamcorder;

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
		lcd = HeldCamcorder.surface(width,height,21,10,17,-6.5f,6.5f);
		body = HeldCamcorder.surface(width,height,22,8,18,8,17);
		grip = body;
		parts.put(Part.ZOOM, panel(.18,.04,.3,.12));
		parts.put(Part.SHUTTER, panel(.63,.04,.19,.12));
		parts.put(Part.TOP_LCD, panel(.38,.02,.2,.08));
		double[][] hardware={{4.0/18,4.0/20,4.0/18,3.0/20}, {6.0/18,8.0/20,6.0/18,4.0/20},
			{10.0/18,4.0/20,4.0/18,3.0/20}, {4.0/18,14.0/20,4.0/18,3.0/20}, {10.0/18,14.0/20,4.0/18,3.0/20}};
		for (BodyControls.Page page : BodyControls.Page.values()) {
			for(int i=0;i<page.parts.length;i++) {
				double[] h=hardware[i];
				parts.put(page.parts[i],panel(h[0],h[1],h[2],h[3]));
			}
		}
	}
	public Rect rect(double dx, double dy, double w, double h) {
		return new Rect(x + dx * scale, y + dy * scale, w * scale, h * scale);
	}
	private Rect panel(double px,double py,double pw,double ph) {
		// Texture UV on the east face runs from z=17 to z=8.
		return HeldCamcorder.surface(width,height,22,
			(float)(18-(py+ph)*10),(float)(18-py*10),(float)(8+px*9),(float)(8+(px+pw)*9));
	}
	public Rect pageButton(int index) { return new Rect(lcd.x()+index*lcd.w()/6,lcd.y()+2,lcd.w()/6,10*scale); }
	public Rect get(Part part) { return parts.get(part); }
	public static BodyLayout of(double width, double height) { return new BodyLayout(width, height); }
}
