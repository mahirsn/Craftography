package io.github.mahirsn.craftography.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Hairline drawing in GUI coordinates: lines are drawn in window pixels so they stay thin at every GUI scale. */
public final class Draw {
	private Draw() {
	}

	/** A pixel-art sprite from assets/craftography/textures/gui/sprites/viewfinder, tinted with {@code color}. */
	public static Identifier sprite(String name) {
		return Identifier.fromNamespaceAndPath("craftography", "viewfinder/" + name);
	}

	public static void sprite(GuiGraphicsExtractor g, String name, double x, double y, int w, int h, int color) {
		g.pose().pushMatrix();
		g.pose().translate((float) x, (float) y);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(name), 0, 0, w, h, color);
		g.pose().popMatrix();
	}

	/** A sprite from assets/craftography/textures/gui/sprites/camera, at integer GUI coordinates. */
	public static void camera(GuiGraphicsExtractor g, String name, double x, double y, int w, int h, int color) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("craftography", "camera/" + name), (int) x, (int) y, w, h, color);
	}

	public static int argb(float alpha, int rgb) {
		int a = Math.round(Easing.clamp01(alpha) * 255f);
		return a << 24 | rgb & 0xFFFFFF;
	}

	public static float pixel() {
		return 1f / Minecraft.getInstance().getWindow().getGuiScale();
	}

	/** Line thickness in window pixels for the current GUI scale. */
	private static int thickness(int scale) {
		return Math.max(1, scale / 2);
	}

	public static void line(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, int color) {
		int s = Minecraft.getInstance().getWindow().getGuiScale();
		double dx = (x2 - x1) * s, dy = (y2 - y1) * s;
		int len = (int) Math.round(Math.hypot(dx, dy));
		if (len == 0) return;
		int t = thickness(s);
		g.pose().pushMatrix();
		g.pose().scale(1f / s, 1f / s);
		g.pose().translate((float) (x1 * s), (float) (y1 * s));
		g.pose().rotate((float) Math.atan2(dy, dx));
		g.fill(0, -t / 2, len, -t / 2 + t, color);
		g.pose().popMatrix();
	}

	/** Filled rectangle with fractional GUI coordinates, snapped to window pixels. */
	public static void rect(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, int color) {
		int s = Minecraft.getInstance().getWindow().getGuiScale();
		g.pose().pushMatrix();
		g.pose().scale(1f / s, 1f / s);
		g.fill((int) Math.round(x1 * s), (int) Math.round(y1 * s), (int) Math.round(x2 * s), (int) Math.round(y2 * s), color);
		g.pose().popMatrix();
	}

	/** Exposure's engraved print: dark text with a light line under it, as on the camera bar. */
	public static final int PRINT = 0x2B2622, PRINT_LIGHT = 0x7A736C;

	public static void engraved(GuiGraphicsExtractor g, String text, double x, double y) {
		var font = net.minecraft.client.Minecraft.getInstance().font;
		g.text(font, text, (int) x, (int) y + 1, argb(1f, PRINT_LIGHT), false);
		g.text(font, text, (int) x, (int) y, argb(1f, PRINT), false);
	}

	/** A grey camera-bar plate behind a side-panel control, slightly larger than its hit area. */
	public static void plate(GuiGraphicsExtractor g, io.github.mahirsn.craftography.util.Rect area, boolean hovered) {
		camera(g, hovered ? "plate_highlighted" : "plate", Math.floor(area.x()) - 4, Math.floor(area.y()) - 5,
			(int) Math.ceil(area.w()) + 8, (int) Math.ceil(area.h()) + 7, -1);
	}

	private static int mix(int a, int b, double t) {
		t = Math.max(0, Math.min(1, t));
		int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int gg = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | r << 16 | gg << 8 | bl;
	}

	/**
	 * A thumbwheel seen edge-on, like Exposure's shutter speed dial: a ridged cylinder that turns sideways with the
	 * value (ridges bunch up toward the ends, brightest where they face you), with the value in a window in the middle.
	 */
	public static void thumbwheel(GuiGraphicsExtractor g, double x, double y, double w, double h, double turn, boolean hovered, String value) {
		int x0 = (int) Math.round(x), y0 = (int) Math.round(y), x1 = (int) Math.round(x + w), y1 = (int) Math.round(y + h);
		rect(g, x0, y0, x1, y1, 0xFF2B2622);
		rect(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFF3A3430);
		double cx = (x0 + x1) / 2.0, radius = (x1 - x0) / 2.0 - 2;
		int ridges = 36;
		int dark = 0xFF4A443F, light = hovered ? 0xFFC9C0B6 : 0xFFA39A92;
		for (int k = 0; k < ridges; k++) {
			double a = 2 * Math.PI * k / ridges + turn * 2 * Math.PI;
			double facing = Math.cos(a);
			if (facing <= 0.05) continue;
			int px = (int) Math.round(cx + radius * Math.sin(a));
			rect(g, px, y0 + 2, px + 1, y1 - 2, mix(dark, light, facing));
			if (facing > 0.5) rect(g, px + 1, y0 + 2, px + 2, y1 - 2, 0xFF2F2A26);
		}
		rect(g, x0 + 1, y0 + 1, x1 - 1, y0 + 2, 0xFF5C554F);
		rect(g, x0 + 1, y1 - 2, x1 - 1, y1 - 1, 0xFF2F2A26);
		if (value == null) return;
		var font = net.minecraft.client.Minecraft.getInstance().font;
		int ww = font.width(value) + 10, wx0 = (int) Math.round(cx - ww / 2.0), wx1 = wx0 + ww;
		rect(g, wx0, y0 + 1, wx1, y1 - 1, 0xFF6D6560);
		rect(g, wx0, y0 + 1, wx1, y0 + 2, 0xFF8F8780);
		rect(g, wx0, y0 + 1, wx0 + 1, y1 - 1, 0xFF8F8780);
		rect(g, wx1 - 1, y0 + 1, wx1, y1 - 1, 0xFF4A443F);
		int ty = (int) Math.round(y0 + (h - 8) / 2.0);
		g.text(font, value, wx0 + 5, ty + 1, argb(1f, PRINT_LIGHT), false);
		g.text(font, value, wx0 + 5, ty, argb(1f, PRINT), false);
	}
}
