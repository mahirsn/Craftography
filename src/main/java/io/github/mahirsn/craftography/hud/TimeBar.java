package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import io.github.mahirsn.craftography.util.TimeMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 24 hour bar under the frame. Left end = 06:00 (tick 0), then noon, 18:00, midnight, back to 06:00. */
public final class TimeBar {
	private static final int GOLD = 0xFFB000, BLUE = 0x4A7BD0;
	// Bands in day ticks (05:00 = 23000, 06:00 = 0, 18:00 = 12000).
	private static final int[][] GOLDEN = {{0, 1000}, {11000, 12500}};
	private static final int[][] BLUE_HOUR = {{23000, 24000}, {12500, 13500}};
	private static final int[] MARKS = {0, 6000, 12000, 18000};

	private TimeBar() {
	}

	/** Bar line rectangle (height 0) for a frame, in GUI units. Also used to drag the handle. */
	public static Rect bar(Rect f, double screenWidth, double screenHeight) {
		double w = Math.max(160, f.w() * 0.8);
		w = Math.min(w, screenWidth - 16);
		// Below the bezel, above the camera bar.
		double y = Math.min(f.bottom() + AspectMask.BEZEL + 14, CameraBar.top(screenHeight) - 7);
		return new Rect((screenWidth - w) / 2, y, w, 0);
	}

	/** Day tick under a mouse x position, snapped to 10 ticks. */
	public static int tickAt(Rect bar, double mouseX) {
		double t = Math.max(0, Math.min(1, (mouseX - bar.x()) / bar.w()));
		return TimeMath.dayTicks(Math.round(t * TimeMath.DAY / 10.0) * 10);
	}

	public static void draw(GuiGraphicsExtractor g, Rect f, int dayTicks, boolean overridden, float alpha) {
		Font font = Minecraft.getInstance().font;
		Rect bar = bar(f, g.guiWidth(), g.guiHeight());
		double y = bar.y();
		Draw.line(g, bar.x(), y, bar.right(), y, Draw.argb(alpha * 0.5f, InfoStrip.WHITE));
		band(g, bar, GOLDEN, Draw.argb(alpha * 0.8f, GOLD));
		band(g, bar, BLUE_HOUR, Draw.argb(alpha * 0.8f, BLUE));

		int dim = Draw.argb(alpha * 0.7f, InfoStrip.WHITE);
		for (int m : MARKS) {
			double x = x(bar, m);
			Draw.line(g, x, y - 2, x, y + 2, dim);
			String label = TimeMath.format(m).substring(0, 2);
			g.text(font, label, (int) Math.round(x - font.width(label) / 2.0), (int) (y - 3 - font.lineHeight), dim, false);
		}

		Draw.sprite(g, "sun", bar.x() - 12, y - 3, 7, 7, dim);
		Draw.sprite(g, "moon", bar.right() + 5, y - 3, 7, 7, dim);

		int handle = Draw.argb(alpha, overridden ? InfoStrip.AMBER : InfoStrip.WHITE);
		double hx = x(bar, dayTicks);
		Draw.rect(g, hx - 1, y - 4, hx + 1, y + 4, handle);
	}

	private static void band(GuiGraphicsExtractor g, Rect bar, int[][] ranges, int color) {
		for (int[] r : ranges) {
			Draw.rect(g, x(bar, r[0]), bar.y() - 1, x(bar, r[1]), bar.y() + 1, color);
		}
	}

	private static double x(Rect bar, int dayTicks) {
		return bar.x() + bar.w() * dayTicks / TimeMath.DAY;
	}
}
