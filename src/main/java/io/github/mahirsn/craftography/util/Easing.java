package io.github.mahirsn.craftography.util;

public final class Easing {
	private Easing() {
	}

	public static float outCubic(float t) {
		float u = 1f - clamp01(t);
		return 1f - u * u * u;
	}

	public static float inOutCubic(float t) {
		t = clamp01(t);
		return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
	}

	public static float clamp01(float t) {
		return t < 0f ? 0f : Math.min(t, 1f);
	}

	/** Moves {@code current} toward {@code target} by a frame-rate independent fraction. */
	public static double approach(double current, double target, double speed, double seconds) {
		return target + (current - target) * Math.exp(-speed * seconds);
	}
}
