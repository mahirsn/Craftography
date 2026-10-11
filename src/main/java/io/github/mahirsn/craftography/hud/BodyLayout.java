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
		// Model units: the flip LCD screen faces the player on x = 10.5; the rear panel is x = 22,
		// the left side panel the open LCD uncovers is z = 8 (see Craftography-art/camcorder3d.py).
		lcd = HeldCamcorder.project(width,height,new float[][]{lcdPoint(16.5f,-12),lcdPoint(16.5f,6),lcdPoint(7.5f,-12),lcdPoint(7.5f,6)});
		body = rear(0,0,18,20);
		grip = body;
		parts.put(Part.ZOOM, side(10,4,14,10));
		parts.put(Part.SHUTTER, side(14.5f,5,17,9));
		parts.put(Part.TOP_LCD, side(11,0.5f,23,2.5f));
		// Each page: two small dials and the large wheel on the rear panel, two buttons on the side panel.
		Rect[] hardware={rear(3,3,8.5f,8.5f), rear(4.5f,9.5f,13.5f,17.5f), rear(9.5f,3,15,8.5f), side(17.5f,4,21.5f,10), side(22,4,26,10)};
		for (BodyControls.Page page : BodyControls.Page.values()) {
			for(int i=0;i<page.parts.length;i++) parts.put(page.parts[i],hardware[i]);
		}
	}
	/** A point on the LCD screen (x = 10.5), turned 22.5 degrees about its hinge like the model element. */
	private static float[] lcdPoint(float y, float z) {
		double a=Math.toRadians(22.5), dx=10.5-10, dz=z-7.25;
		return new float[]{(float)(10+dx*Math.cos(a)+dz*Math.sin(a)), y, (float)(7.25-dx*Math.sin(a)+dz*Math.cos(a))};
	}
	/** Texel box on the rear panel texture (18x20, u runs from z=17 to z=8, v from y=17 down). */
	private Rect rear(float u0,float v0,float u1,float v1) {
		return HeldCamcorder.surface(width,height,22,17-v1/2,17-v0/2,17-u1/2,17-u0/2);
	}
	/** Texel box on the left side panel texture (28x20, u runs from x=22 to x=8, v from y=17 down). */
	private Rect side(float u0,float v0,float u1,float v1) {
		return HeldCamcorder.surfaceZ(width,height,8,22-u1/2,22-u0/2,17-v1/2,17-v0/2);
	}
	public Rect rect(double dx, double dy, double w, double h) {
		return new Rect(x + dx * scale, y + dy * scale, w * scale, h * scale);
	}
	public Rect pageButton(int index) { return new Rect(lcd.x()+index*lcd.w()/6,lcd.y()+2,lcd.w()/6,10*scale); }
	public Rect get(Part part) { return parts.get(part); }
	public static BodyLayout of(double width, double height) { return new BodyLayout(width, height); }
}
