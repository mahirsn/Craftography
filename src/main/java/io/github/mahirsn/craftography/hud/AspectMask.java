package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraSettings.Aspect;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The frame rectangle inside the live view and the mask around it. The guides and the shutter crop use the same rectangle. */
public final class AspectMask {
	private static final int CORNER_SIZE = 5;

	private AspectMask() {
	}

	/** Largest rectangle with the aspect's ratio, centred in {@code area} (the live view). FREE uses all of it. */
	public static Rect frame(Rect area, Aspect aspect) {
		if (aspect.ratio <= 0) return area;
		double fw, fh;
		if (area.w() / area.h() > aspect.ratio) {
			fh = area.h();
			fw = area.h() * aspect.ratio;
		} else {
			fw = area.w();
			fh = area.w() / aspect.ratio;
		}
		return new Rect(area.x() + (area.w() - fw) / 2, area.y() + (area.h() - fh) / 2, fw, fh);
	}

	/** Mask between the live view edge and the frame, and corner marks inside the frame. */
	public static void draw(GuiGraphicsExtractor g, Rect area, Rect f, float maskAlpha, float lineAlpha) {
		int mask = Draw.argb(maskAlpha, 0x000000);
		Draw.rect(g, area.x(), area.y(), area.right(), f.y(), mask);
		Draw.rect(g, area.x(), f.bottom(), area.right(), area.bottom(), mask);
		Draw.rect(g, area.x(), f.y(), f.x(), f.bottom(), mask);
		Draw.rect(g, f.right(), f.y(), area.right(), f.bottom(), mask);

		// Hairline corner marks, rotated for each corner; clear of the LCD status row.
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
		Draw.line(g, 0, 0, CORNER_SIZE, 0, color);
		Draw.line(g, 0, 0, 0, CORNER_SIZE, color);
		g.pose().popMatrix();
	}
}
