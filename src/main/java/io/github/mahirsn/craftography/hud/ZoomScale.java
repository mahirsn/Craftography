package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.LensMath;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Vertical focal length ruler on the left of the frame, log scale, presets labelled, arrow at the current value. */
public final class ZoomScale {
	private ZoomScale() {
	}

	public static void draw(GuiGraphicsExtractor g, Rect f, double mm, float alpha) {
		Font font = Minecraft.getInstance().font;
		int white = Draw.argb(alpha * 0.85f, InfoStrip.WHITE), dim = Draw.argb(alpha * 0.45f, InfoStrip.WHITE);
		int amber = Draw.argb(alpha, InfoStrip.AMBER);
		Rect ruler = ruler(f);
		double x = ruler.right(), top = ruler.y(), bottom = ruler.bottom();

		Draw.line(g, x, top, x, bottom, dim);
		for (int v = 10; v <= 200; v += v < 50 ? 5 : v < 100 ? 10 : 25) {
			double y = y(v, top, bottom);
			Draw.line(g, x - 2, y, x, y, dim);
		}
		for (double preset : LensMath.PRESETS) {
			double y = y(preset, top, bottom);
			boolean selected = Math.abs(mm - preset) < 0.5;
			Draw.line(g, x - 4, y, x, y, selected ? amber : white);
			String label = String.valueOf((int) preset);
			g.text(font, label, (int) (x - 6 - font.width(label)), (int) Math.round(y - font.lineHeight / 2.0 + 1), selected ? amber : white, false);
		}

		double y = y(mm, top, bottom);
		for (int i = 0; i <= 3; i++) {
			Draw.line(g, x + 2 + i, y - 3 + i, x + 2 + i, y + 3 - i, amber);
		}
	}

	/** Ruler area: labels on the left, the ruler line on the right edge. The panel drags inside this area. */
	public static Rect ruler(Rect f) {
		Font font = Minecraft.getInstance().font;
		double x = Math.max(f.x() - 5 - AspectMask.BEZEL, font.width("200") + 12);
		double left = x - font.width("200") - 8;
		// Full-screen frame (Free): keep clear of the title row and of the time bar.
		boolean full = f.y() < 16;
		double top = full ? 18 : f.y() + 4, bottom = full ? f.bottom() - 60 : f.bottom() - 4;
		return new Rect(left, top, x - left, bottom - top);
	}

	private static double y(double mm, double top, double bottom) {
		return bottom - LensMath.scalePosition(mm) * (bottom - top);
	}
}
