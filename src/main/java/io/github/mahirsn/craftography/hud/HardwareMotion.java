package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.Autofocus;
import io.github.mahirsn.craftography.hud.BodyLayout.Part;
import net.minecraft.util.Util;
import java.util.EnumMap;

/** Presentation state only. Settings remain the sole source of camera values. */
public final class HardwareMotion {
	private static final class Motion {
		double value = Double.NaN, from, target;
		long at;
		double position() {
			double t = Math.clamp((Util.getMillis() - at) / 160.0, 0, 1);
			return from + (target - from) * (1 - Math.pow(1 - t, 3));
		}
		double follow(double next) {
			if (Double.isNaN(value)) { value = next; from = target = next; }
			if (next != value) { from = position(); target = next; at = Util.getMillis(); value = next; }
			return position();
		}
	}
	private static final EnumMap<Part, Motion> wheels = new EnumMap<>(Part.class);
	private static final EnumMap<Part, Long> presses = new EnumMap<>(Part.class);
	private record Release(long at, double depth) { }
	private static final EnumMap<Part, Release> releases = new EnumMap<>(Part.class);
	private static Part held;
	private static long focusAt;
	private static final Motion selector = new Motion();
	private static final Motion meter = new Motion();
	public static double exposurePosition(double stops) { return meter.follow(Math.clamp(stops,-3,3)*4); }
	public static double pagePosition() { return selector.follow(BodyControls.page().ordinal()); }
	public static void press(Part p) { held = p; releases.remove(p); presses.put(p, Util.getMillis()); if (p == Part.SHUTTER) acquire(); }
	public static void release() {
		if (held != null) releases.put(held, new Release(Util.getMillis(), inset(held)));
		held = null;
	}
	public static boolean halfPress() { return held == Part.SHUTTER; }
	public static void acquire() { focusAt = Util.getMillis(); }
	public static double acquisition() { return Math.clamp((Util.getMillis() - focusAt) / 180.0, 0, 1); }
	public static double inset(Part p) {
		Long at = presses.get(p);
		if (at == null) return 0;
		double elapsed = Util.getMillis() - at;
		if (p == held) return Math.min(1, elapsed / 80);
		Release release = releases.get(p);
		return release == null ? 0 : release.depth() * Math.max(0, 1 - (Util.getMillis() - release.at()) / 80.0);
	}
	public static double position(Part p) {
		CameraSettings s = CameraSettings.get();
		double value = switch (p) {
			case ZOOM -> Math.log(s.focalLength / 35) * 4;
			case ISO -> Math.log(s.iso / 100.0) / Math.log(2) * 3;
			case APERTURE -> Math.log(s.aperture / 5.6) / Math.log(2) * 6;
			case SHUTTER_SPEED -> Math.log(s.shutter * 125) / Math.log(2) * 3;
			case TIME -> ViewfinderHud.visibleDayTicks(s) / 100.0;
			case KELVIN -> s.kelvin / 100.0;
			case TINT -> s.tint / 5.0;
			case FOCUS -> Math.log(Math.max(.1, s.autofocus ? Autofocus.distance() : s.focusDistance)) / Math.log(1.2);
			case MASK -> s.maskOpacity * 20;
			case GRID -> s.guideOpacity * 20;
			default -> 0;
		};
		return wheels.computeIfAbsent(p, k -> new Motion()).follow(value);
	}
	private HardwareMotion() { }
}
