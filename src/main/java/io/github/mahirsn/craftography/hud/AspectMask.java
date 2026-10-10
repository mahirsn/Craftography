package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraSettings.Aspect;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The frame rectangle and the mask around it. The guides and the shutter crop use the same rectangle. */
public final class AspectMask {
	// Margins as a fraction of the screen, so the frame has the same proportions in GUI units and window pixels.
	static final double SIDE = 0.07, TOP = 0.08, BOTTOM = 0.17;
	private static final int CORNER_SIZE = 8;
	/** Width of the pixel-art bezel around the frame, in GUI units. */
	public static final int BEZEL = 8;

	private AspectMask() {
	}

	/** Largest rectangle with the aspect's ratio that fits inside the screen margins. FREE uses the whole screen. */
	public static Rect frame(double width, double height, Aspect aspect) {
		if (aspect.ratio <= 0) return new Rect(0, 0, width, height);
		double ax = width * SIDE, ay = height * TOP;
		double aw = width - 2 * ax, ah = height - ay - height * BOTTOM;
		double fw, fh;
		if (aw / ah > aspect.ratio) {
			fh = ah;
			fw = ah * aspect.ratio;
		} else {
			fw = aw;
			fh = aw / aspect.ratio;
		}
		return new Rect(ax + (aw - fw) / 2, ay + (ah - fh) / 2, fw, fh);
	}

	public static void draw(GuiGraphicsExtractor g, Rect f, boolean bezel, float maskAlpha, float lineAlpha, float bezelAlpha) {
		int w = g.guiWidth(), h = g.guiHeight();
		int mask = Draw.argb(maskAlpha, 0x000000);
		Draw.rect(g, 0, 0, w, f.y(), mask);
		Draw.rect(g, 0, f.bottom(), w, h, mask);
		Draw.rect(g, 0, f.y(), f.x(), f.bottom(), mask);
		Draw.rect(g, f.right(), f.y(), w, f.bottom(), mask);
		if (bezel) {
			// Nine-sliced sprite; its inner edge lies on the frame edge, so it never covers the picture.
			int x = (int) Math.round(f.x()), y = (int) Math.round(f.y());
			int fw = (int) Math.round(f.right()) - x, fh = (int) Math.round(f.bottom()) - y;
			Draw.camera(g, "bezel", x - BEZEL, y - BEZEL, fw + 2 * BEZEL, fh + 2 * BEZEL, Draw.argb(bezelAlpha, 0xFFFFFF));
		}

		// Corner marks: one sprite (top-left), turned by 90 degree steps for the other corners.
		int c = Draw.argb(lineAlpha, 0xFFFFFF);
		double x1 = f.x() + 2, y1 = f.y() + 2, x2 = f.right() - 2, y2 = f.bottom() - 2;
		corner(g, x1, y1, 0, c);
		corner(g, x2, y1, 1, c);
		corner(g, x2, y2, 2, c);
		corner(g, x1, y2, 3, c);
	}

	private static void corner(GuiGraphicsExtractor g, double x, double y, int quarterTurns, int color) {
		g.pose().pushMatrix();
		g.pose().translate((float) x, (float) y);
		g.pose().rotate((float) (Math.PI / 2 * quarterTurns));
		Draw.sprite(g, "corner", 0, 0, CORNER_SIZE, CORNER_SIZE, color);
		g.pose().popMatrix();
	}
}
