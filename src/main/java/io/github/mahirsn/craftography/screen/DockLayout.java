package io.github.mahirsn.craftography.screen;

import io.github.mahirsn.craftography.util.Rect;

/** The settings dock stays on the right, leaving the centre of the picture clear. */
public record DockLayout(Rect panel) {
	public static final int ROW_HEIGHT = 34;

	public static DockLayout of(int width, int height) {
		int w = Math.min(184, Math.max(96, width / 2 - 12));
		return new DockLayout(new Rect(width - w - 8, 8, w, Math.min(268, height - 16)));
	}

	public Rect tab(int index) {
		double w = (panel.w() - 16) / 6;
		return new Rect(panel.x() + 8 + index * w, panel.y() + 23, w - 2, 22);
	}

	public Rect content() {
		return new Rect(panel.x() + 10, panel.y() + 53, panel.w() - 20, panel.h() - 85);
	}

	public Rect row(int index, int scroll) {
		Rect c = content();
		return new Rect(c.x(), c.y() + index * ROW_HEIGHT - scroll, c.w(), ROW_HEIGHT);
	}

	public Rect shutter() {
		return new Rect(panel.x() + 10, panel.bottom() - 25, panel.w() - 20, 17);
	}

	public static boolean contains(Rect r, double x, double y) {
		return x >= r.x() && x < r.right() && y >= r.y() && y < r.bottom();
	}
}
