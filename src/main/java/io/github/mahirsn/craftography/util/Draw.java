package io.github.mahirsn.craftography.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Hairline drawing in GUI coordinates: lines are drawn in window pixels so they stay thin at every GUI scale. */
public final class Draw {
	private Draw() {
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
}
