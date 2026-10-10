package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraSettings.Guide;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Composition guides, drawn inside the frame only. */
public final class GuideOverlay {
	private static final double PHI_SMALL = 1 - 1 / ((1 + Math.sqrt(5)) / 2); // 0.382

	private GuideOverlay() {
	}

	public static void draw(GuiGraphicsExtractor g, Rect f, Guide guide, float alpha) {
		lines(g, f, guide, Draw.argb(alpha, 0xFFFFFF));
		// Small center mark, always shown.
		double cx = f.x() + f.w() / 2, cy = f.y() + f.h() / 2;
		int dot = Draw.argb(Math.max(alpha, 0.5f), 0xFFFFFF);
		Draw.line(g, cx - 2, cy, cx + 2, cy, dot);
		Draw.line(g, cx, cy - 2, cx, cy + 2, dot);
	}

	private static void lines(GuiGraphicsExtractor g, Rect f, Guide guide, int c) {
		switch (guide) {
			case NONE -> {
			}
			case THIRDS -> grid(g, f, 1 / 3.0, c);
			case PHI -> grid(g, f, PHI_SMALL, c);
			case QUARTERS -> grid(g, f, 0.5, c);
			case DIAGONALS -> {
				Draw.line(g, f.x(), f.y(), f.right(), f.bottom(), c);
				Draw.line(g, f.right(), f.y(), f.x(), f.bottom(), c);
			}
			case CENTER -> {
				double cx = f.x() + f.w() / 2, cy = f.y() + f.h() / 2, arm = Math.min(f.w(), f.h()) * 0.06;
				Draw.line(g, cx - arm, cy, cx + arm, cy, c);
				Draw.line(g, cx, cy - arm, cx, cy + arm, c);
			}
		}
	}

	/** Two vertical and two horizontal lines at {@code t} and {@code 1 - t} of the frame. */
	private static void grid(GuiGraphicsExtractor g, Rect f, double t, int c) {
		for (double p : new double[]{t, 1 - t}) {
			double x = f.x() + f.w() * p, y = f.y() + f.h() * p;
			Draw.line(g, x, f.y(), x, f.bottom(), c);
			Draw.line(g, f.x(), y, f.right(), y, c);
		}
	}
}
