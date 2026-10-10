package io.github.mahirsn.craftography.util;

/** Focal length (mm, full-frame sensor, 24 mm tall) to vertical field of view (degrees) and back. */
public final class LensMath {
	public static final double MIN_MM = 10, MAX_MM = 200, DEFAULT_MM = 35;
	public static final double[] PRESETS = {14, 24, 35, 50, 85, 135, 200};
	/** Zoom factor per scroll notch. */
	public static final double STEP = 1.08;
	private static final double HALF_SENSOR = 12;

	private LensMath() {
	}

	public static double vfov(double mm) {
		return Math.toDegrees(2 * Math.atan(HALF_SENSOR / mm));
	}

	public static double focalLength(double vfovDegrees) {
		return HALF_SENSOR / Math.tan(Math.toRadians(vfovDegrees) / 2);
	}

	public static double clamp(double mm) {
		return Math.max(MIN_MM, Math.min(MAX_MM, mm));
	}

	/**
	 * Zooms by {@code notches} scroll steps on a log scale. A preset between the old and the new value
	 * stops the zoom there, so presets are never skipped (soft snap).
	 */
	public static double zoom(double mm, double notches) {
		double next = clamp(mm * Math.pow(STEP, notches));
		for (int i = 0; i < PRESETS.length; i++) {
			double p = PRESETS[notches > 0 ? i : PRESETS.length - 1 - i];
			boolean crossed = notches > 0 ? p > mm * 1.0001 && p < next : p < mm / 1.0001 && p > next;
			if (crossed) return p;
		}
		return next;
	}

	/** Position of a focal length on a log scale, 0 at MIN_MM and 1 at MAX_MM. */
	public static double scalePosition(double mm) {
		return Math.log(mm / MIN_MM) / Math.log(MAX_MM / MIN_MM);
	}
}
