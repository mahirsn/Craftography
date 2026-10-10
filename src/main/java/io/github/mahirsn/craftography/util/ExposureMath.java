package io.github.mahirsn.craftography.util;

import java.util.Locale;

/**
 * Camera exposure, white balance and depth of field math. Pure functions; the lens shader gets their results as
 * uniforms. Stops are the usual 1/3-stop series of real cameras.
 */
public final class ExposureMath {
	public static final int[] ISO = {100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250, 1600, 2000, 2500, 3200, 4000, 5000, 6400, 8000, 10000, 12800};
	public static final double[] APERTURE = {1.4, 1.6, 1.8, 2, 2.2, 2.5, 2.8, 3.2, 3.5, 4, 4.5, 5, 5.6, 6.3, 7.1, 8, 9, 10, 11, 13, 14, 16, 18, 20, 22};
	/** Shutter speeds in seconds, fastest first. */
	public static final double[] SHUTTER = {1 / 4000.0, 1 / 3200.0, 1 / 2500.0, 1 / 2000.0, 1 / 1600.0, 1 / 1250.0, 1 / 1000.0, 1 / 800.0,
		1 / 640.0, 1 / 500.0, 1 / 400.0, 1 / 320.0, 1 / 250.0, 1 / 200.0, 1 / 160.0, 1 / 125.0, 1 / 100.0, 1 / 80.0, 1 / 60.0, 1 / 50.0,
		1 / 40.0, 1 / 30.0, 1 / 25.0, 1 / 20.0, 1 / 15.0, 1 / 13.0, 1 / 10.0, 1 / 8.0, 1 / 6.0, 1 / 5.0, 1 / 4.0, 0.3, 0.4, 0.5, 0.6, 0.8, 1};

	public static final int DEFAULT_ISO = 100;
	public static final double DEFAULT_APERTURE = 5.6, DEFAULT_SHUTTER = 1 / 125.0;
	public static final int DEFAULT_KELVIN = 6500, MIN_KELVIN = 2500, MAX_KELVIN = 10000;
	public static final int MAX_TINT = 50;
	/** Focus distances in metres; FOCUS_INFINITY and above means infinity. */
	public static final double MIN_FOCUS = 0.3, FOCUS_INFINITY = 1000;

	/**
	 * The game's own picture counts as correctly exposed at the default settings (ISO 100, f/5.6, 1/125 s), so
	 * the defaults change nothing. Every setting moves the exposure from there in real stops.
	 */
	private static final double REFERENCE_EV = ev100(DEFAULT_APERTURE, DEFAULT_SHUTTER, DEFAULT_ISO);

	private ExposureMath() {
	}

	/** Exposure value at ISO 100 that the settings are right for: log2(N^2 / t) - log2(ISO / 100). */
	public static double ev100(double aperture, double shutter, int iso) {
		return Math.log(aperture * aperture / shutter) / Math.log(2) - Math.log(iso / 100.0) / Math.log(2);
	}

	/** Linear brightness factor of the settings against the reference: one stop = factor 2. */
	public static double gain(double aperture, double shutter, int iso) {
		return Math.pow(2, REFERENCE_EV - ev100(aperture, shutter, iso));
	}

	/** Exposure difference to the reference in stops, positive = brighter. */
	public static double stops(double aperture, double shutter, int iso) {
		return REFERENCE_EV - ev100(aperture, shutter, iso);
	}

	public static int nearest(int[] values, int value) {
		int best = 0;
		for (int i = 1; i < values.length; i++) if (Math.abs(values[i] - value) < Math.abs(values[best] - value)) best = i;
		return best;
	}

	public static int nearest(double[] values, double value) {
		int best = 0;
		for (int i = 1; i < values.length; i++) if (Math.abs(Math.log(values[i] / value)) < Math.abs(Math.log(values[best] / value))) best = i;
		return best;
	}

	public static int step(int[] values, int value, int steps) {
		return values[Math.clamp(nearest(values, value) + steps, 0, values.length - 1)];
	}

	public static double step(double[] values, double value, int steps) {
		return values[Math.clamp(nearest(values, value) + steps, 0, values.length - 1)];
	}

	public static String formatShutter(double seconds) {
		if (seconds >= 0.3 - 1e-9) return (seconds >= 1 ? String.valueOf((int) Math.round(seconds)) : String.format(Locale.ROOT, "%.1f", seconds)) + "\"";
		return "1/" + Math.round(1 / seconds);
	}

	public static String formatAperture(double n) {
		return "f/" + (n < 10 && n != Math.floor(n) ? String.format(Locale.ROOT, "%.1f", n) : String.valueOf((int) Math.round(n)));
	}

	public static String formatFocus(double metres) {
		if (metres >= FOCUS_INFINITY) return "∞m";
		return (metres < 10 ? String.format(Locale.ROOT, "%.1f", metres) : String.valueOf(Math.round(metres))) + "m";
	}

	/** sRGB 0..1 to linear light. */
	static double linear(double c) {
		return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	}

	/** Colour of a black body at {@code kelvin} as linear RGB (Tanner Helland's fit of the CIE data). */
	static double[] blackBody(double kelvin) {
		double t = kelvin / 100;
		double r = t <= 66 ? 255 : 329.698727446 * Math.pow(t - 60, -0.1332047592);
		double g = t <= 66 ? 99.4708025861 * Math.log(t) - 161.1195681661 : 288.1221695283 * Math.pow(t - 60, -0.0755148492);
		double b = t >= 66 ? 255 : t <= 19 ? 0 : 138.5177312231 * Math.log(t - 10) - 305.0447927307;
		return new double[]{linear(Math.clamp(r, 0, 255) / 255), linear(Math.clamp(g, 0, 255) / 255), linear(Math.clamp(b, 0, 255) / 255)};
	}

	/**
	 * Channel factors of a white balance setting, for linear RGB. The camera assumes the light has the set
	 * temperature and neutralises it: a low setting (tungsten) makes the picture bluer, a high one warmer.
	 * Positive tint = magenta, negative = green. Normalised so the brightness of grey does not change.
	 */
	public static double[] whiteBalance(int kelvin, int tint) {
		double[] ref = blackBody(DEFAULT_KELVIN), set = blackBody(kelvin);
		double[] m = {ref[0] / set[0], ref[1] / set[1], ref[2] / set[2]};
		m[1] *= Math.pow(2, -tint / 100.0);
		double luminance = 0.2126 * m[0] + 0.7152 * m[1] + 0.0722 * m[2];
		return new double[]{m[0] / luminance, m[1] / luminance, m[2] / luminance};
	}

	/**
	 * Depth of field: circle of confusion diameter (thin lens) as a fraction of the sensor height, for a point at
	 * {@code distance} metres. Focal length in mm, 24 mm tall full-frame sensor.
	 * c = f^2 / N * |1/S - 1/d| * S / (S - f); with S at infinity, c = f^2 / (N d).
	 */
	public static double circleOfConfusion(double focalMm, double aperture, double focusM, double distanceM) {
		return cocScale(focalMm, aperture, focusM) * Math.abs(inverseFocus(focusM) - 1 / distanceM);
	}

	/** Factor of the circle of confusion per |1/S - 1/d| (1/m), as a fraction of the sensor height. */
	public static double cocScale(double focalMm, double aperture, double focusM) {
		double f = focalMm / 1000;
		double lens = focusM >= FOCUS_INFINITY ? 1 : focusM / (focusM - f);
		return f * f / aperture * lens / 0.024;
	}

	public static double inverseFocus(double focusM) {
		return focusM >= FOCUS_INFINITY ? 0 : 1 / focusM;
	}
}
