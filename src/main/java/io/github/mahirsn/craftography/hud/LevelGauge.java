package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

/** Horizon level on the right of the frame: a line that shows where the true horizon lies, plus the roll angle. */
public final class LevelGauge {
	/** Below this roll (degrees) the camera counts as level and the gauge turns amber. */
	static final double LEVEL = 0.05;

	public static final double RADIUS = 12;

	private LevelGauge() {
	}

	/** Inside the live view, at its right edge, like a camera's electronic level. */
	public static double centerX(Rect liveView) {
		return liveView.right() - RADIUS - 14;
	}

	public static double centerY(Rect liveView) {
		return liveView.y() + liveView.h() / 2;
	}

	public static void draw(GuiGraphicsExtractor g, Rect f, double roll, float alpha) {
		Font font = Minecraft.getInstance().font;
		boolean level = Math.abs(roll) < LEVEL;
		int color = Draw.argb(alpha, level ? CameraBody.AMBER : 0xFFFFFF);
		int dim = Draw.argb(alpha * 0.45f, 0xFFFFFF);
		double r = RADIUS;
		double cx = centerX(f), cy = centerY(f);

		// Fixed marks: the camera's own horizontal axis.
		Draw.line(g, cx - r - 4, cy, cx - r, cy, dim);
		Draw.line(g, cx + r, cy, cx + r + 4, cy, dim);
		Draw.line(g, cx, cy - r, cx, cy - r + 3, dim);
		// The world horizon as seen in the picture: rotated against the camera roll.
		double a = Math.toRadians(-roll), dx = Math.cos(a) * r, dy = Math.sin(a) * r;
		Draw.line(g, cx - dx, cy - dy, cx + dx, cy + dy, color);

		String text = String.format(Locale.ROOT, "%+.1f°", level ? 0.0 : roll);
		g.text(font, text, (int) Math.round(cx - font.width(text) / 2.0), (int) (cy + r + 3), color, false);
	}
}
