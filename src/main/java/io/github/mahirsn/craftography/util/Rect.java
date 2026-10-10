package io.github.mahirsn.craftography.util;

public record Rect(double x, double y, double w, double h) {
	public double right() {
		return x + w;
	}

	public double bottom() {
		return y + h;
	}

	public Rect lerp(Rect to, double t) {
		return new Rect(x + (to.x - x) * t, y + (to.y - y) * t, w + (to.w - w) * t, h + (to.h - h) * t);
	}

	public Rect scale(double s) {
		return new Rect(x * s, y * s, w * s, h * s);
	}
}
