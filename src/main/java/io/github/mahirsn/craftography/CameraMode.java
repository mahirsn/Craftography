package io.github.mahirsn.craftography;

import io.github.mahirsn.craftography.util.Easing;
import net.minecraft.util.Util;

/** Whether camera mode is on, plus the open/close transition. */
public final class CameraMode {
	private static final float DURATION_MS = 350f;

	private static boolean open;
	private static long changedAt;
	private static float fromProgress;

	// Smoothed values that follow the settings.
	private static double focalLength = Double.NaN;
	private static long lastSmoothNanos;

	private CameraMode() {
	}

	private static float rawProgress() {
		float t = (Util.getMillis() - changedAt) / DURATION_MS;
		return open ? Math.min(1f, fromProgress + t) : Math.max(0f, fromProgress - t);
	}

	/** True while camera mode is open or still animating closed. Every mixin checks this first. */
	public static boolean isActive() {
		return open || rawProgress() > 0f;
	}

	public static boolean isOpen() {
		return open;
	}

	/** Eased transition value, 0 = closed, 1 = fully open. */
	public static float progress() {
		return Easing.inOutCubic(rawProgress());
	}

	public static void toggle() {
		set(!open);
	}

	public static void set(boolean value) {
		if (value == open) return;
		fromProgress = rawProgress();
		open = value;
		changedAt = Util.getMillis();
	}

	/** Lens focal length eased toward the setting, in mm. Smoothed in log space so zoom feels the same at every length. */
	public static double focalLength() {
		double target = CameraSettings.get().focalLength;
		long now = System.nanoTime();
		double seconds = Math.min(0.25, (now - lastSmoothNanos) / 1e9);
		lastSmoothNanos = now;
		if (Double.isNaN(focalLength)) {
			focalLength = target;
		} else {
			focalLength = Math.exp(Easing.approach(Math.log(focalLength), Math.log(target), 12, seconds));
		}
		return focalLength;
	}

	/** Closes immediately, without animation (for example when leaving the world). */
	public static void reset() {
		open = false;
		fromProgress = 0f;
	}
}
