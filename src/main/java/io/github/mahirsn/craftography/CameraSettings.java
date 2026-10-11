package io.github.mahirsn.craftography;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.mahirsn.craftography.util.ExposureMath;
import io.github.mahirsn.craftography.util.LensMath;
import io.github.mahirsn.craftography.util.TimeMath;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Client settings of the camera. HUD code only reads these; input handlers change them. */
public final class CameraSettings {
	public enum Aspect {
		R16_9("16:9", 16 / 9.0), R3_2("3:2", 3 / 2.0), R4_3("4:3", 4 / 3.0), R1_1("1:1", 1.0),
		R4_5("4:5", 4 / 5.0), R9_16("9:16", 9 / 16.0), R239_1("2.39:1", 2.39), FREE("free", 0);

		public final String label;
		/** Width / height, or 0 for no mask. */
		public final double ratio;

		Aspect(String label, double ratio) {
			this.label = label;
			this.ratio = ratio;
		}

		/** Label usable in a file name, for example 16x9. */
		public String fileLabel() {
			return label.replace(':', 'x');
		}
	}

	public enum Guide {
		NONE, THIRDS, PHI, CENTER, DIAGONALS, QUARTERS;

		public String translationKey() {
			return "craftography.guide." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	public static final double MAX_ROLL = 90;

	public enum Weather {
		SERVER, CLEAR, RAIN, THUNDER;

		public String translationKey() {
			return "craftography.weather." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("craftography.json");
	private static CameraSettings instance = new CameraSettings();

	public Aspect aspect = Aspect.R3_2;
	public Guide guide = Guide.THIRDS;
	public float maskOpacity = 0.75f;
	public float guideOpacity = 0.45f;
	public double focalLength = LensMath.DEFAULT_MM;
	/** Camera roll in degrees, positive = clockwise. */
	public double roll = 0;
	/** Time of day the client sees, in day ticks, or -1 for the server time. */
	public int timeOfDay = -1;
	public Weather weather = Weather.SERVER;
	/** Moon phase the client sees (0 = full moon ... 7), or -1 for the server's. */
	public int moonPhase = -1;
	/** Keep the time and weather override after camera mode closes. */
	public boolean lockTimeWeather = false;
	/** Show a small preview of the photo for two seconds after the shot. */
	public boolean showPreview = true;
	/** Self-timer delay in seconds: 0 (off), 3 or 10. */
	public int selfTimer = 0;

	private static final int[] TIMERS = {0, 3, 10};

	// Exposure: the defaults (ISO 100, f/5.6, 1/125 s, 6500 K) leave the game's picture unchanged.
	public int iso = ExposureMath.DEFAULT_ISO;
	public double aperture = ExposureMath.DEFAULT_APERTURE;
	/** Shutter speed in seconds. */
	public double shutter = ExposureMath.DEFAULT_SHUTTER;
	/** White balance colour temperature in kelvin. */
	public int kelvin = ExposureMath.DEFAULT_KELVIN;
	/** White balance tint, positive = magenta, negative = green. */
	public int tint = 0;
	/** Autofocus on the frame centre; off = manual focus at {@link #focusDistance}. */
	public boolean autofocus = true;
	/** Manual focus distance in metres; {@link ExposureMath#FOCUS_INFINITY} = infinity. */
	public double focusDistance = ExposureMath.FOCUS_INFINITY;
	/** Sensor noise that grows with ISO. */
	public boolean noise = true;
	/** Histogram in the live view. */
	public boolean histogram = false;
	/** Focus peaking in the live view (never in the photo). */
	public boolean peaking = false;

	public static CameraSettings get() {
		return instance;
	}

	public static void load() {
		if (!Files.exists(FILE)) return;
		try (Reader reader = Files.newBufferedReader(FILE)) {
			CameraSettings loaded = GSON.fromJson(reader, CameraSettings.class);
			if (loaded != null) instance = loaded.sanitized();
		} catch (IOException | RuntimeException e) {
			CraftographyClient.LOG.warn("Could not read {}, using defaults", FILE, e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			CraftographyClient.LOG.warn("Could not write {}", FILE, e);
		}
	}

	private CameraSettings sanitized() {
		if (aspect == null) aspect = Aspect.R3_2;
		if (guide == null) guide = Guide.THIRDS;
		maskOpacity = clamp(maskOpacity, 0f, 1f);
		guideOpacity = clamp(guideOpacity, 0f, 1f);
		if (weather == null) weather = Weather.SERVER;
		if (moonPhase < -1 || moonPhase > 7) moonPhase = -1;
		if (java.util.Arrays.stream(TIMERS).noneMatch(t -> t == selfTimer)) selfTimer = 0;
		iso = ExposureMath.ISO[ExposureMath.nearest(ExposureMath.ISO, iso)];
		aperture = ExposureMath.APERTURE[ExposureMath.nearest(ExposureMath.APERTURE, aperture > 0 ? aperture : ExposureMath.DEFAULT_APERTURE)];
		shutter = ExposureMath.SHUTTER[ExposureMath.nearest(ExposureMath.SHUTTER, shutter > 0 ? shutter : ExposureMath.DEFAULT_SHUTTER)];
		kelvin = Math.clamp(kelvin, ExposureMath.MIN_KELVIN, ExposureMath.MAX_KELVIN);
		tint = Math.clamp(tint, -ExposureMath.MAX_TINT, ExposureMath.MAX_TINT);
		focusDistance = Double.isNaN(focusDistance) ? ExposureMath.FOCUS_INFINITY : Math.clamp(focusDistance, ExposureMath.MIN_FOCUS, ExposureMath.FOCUS_INFINITY);
		if (timeOfDay < -1 || timeOfDay >= TimeMath.DAY) timeOfDay = -1;
		roll = Double.isNaN(roll) ? 0 : Math.max(-MAX_ROLL, Math.min(MAX_ROLL, roll));
		focalLength = Double.isNaN(focalLength) ? LensMath.DEFAULT_MM : LensMath.clamp(focalLength);
		return this;
	}

	private static float clamp(float v, float min, float max) {
		return Float.isNaN(v) ? min : Math.max(min, Math.min(max, v));
	}

	/** Changes the roll; crossing or landing near 0 stops at exactly 0 (level). */
	public void addRoll(double degrees) {
		double next = Math.max(-MAX_ROLL, Math.min(MAX_ROLL, roll + degrees));
		if (roll != 0 && (Math.signum(next) != Math.signum(roll) || Math.abs(next) < 0.05)) next = 0;
		roll = next;
	}

	/** Whether the time and weather settings apply now. */
	public boolean overridesApply() {
		return CameraMode.isActive() || lockTimeWeather;
	}

	public void cycleWeather() {
		Weather[] values = Weather.values();
		weather = values[(weather.ordinal() + 1) % values.length];
	}

	public void cycleAspect() {
		Aspect[] values = Aspect.values();
		aspect = values[(aspect.ordinal() + 1) % values.length];
	}

	public void cycleTimer() {
		int i = 0;
		while (TIMERS[i] != selfTimer) i++;
		selfTimer = TIMERS[(i + 1) % TIMERS.length];
	}

	public void cycleGuide() {
		Guide[] values = Guide.values();
		guide = values[(guide.ordinal() + 1) % values.length];
	}
}
