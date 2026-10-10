package io.github.mahirsn.craftography.util;

import java.util.Locale;

/** Day ticks (0..23999) to clock time. Tick 0 = 06:00, 6000 = noon, 12000 = 18:00, 18000 = midnight. */
public final class TimeMath {
	public static final int DAY = 24000;

	private TimeMath() {
	}

	public static int dayTicks(long totalTicks) {
		return (int) Math.floorMod(totalTicks, (long) DAY);
	}

	/** Hours since midnight, 0 (inclusive) to 24 (exclusive). */
	public static double hours(int dayTicks) {
		return ((dayTicks / 1000.0) + 6) % 24;
	}

	public static int ticksFromHours(double hours) {
		return dayTicks(Math.round((hours - 6) * 1000));
	}

	public static String format(int dayTicks) {
		int minutes = (int) Math.floor(hours(dayTicks) * 60 + 1e-9);
		return String.format(Locale.ROOT, "%02d:%02d", minutes / 60 % 24, minutes % 60);
	}

	public static final int MOON_PHASES = 8;

	/**
	 * Visible clock value: the real total ticks with the time of day and/or the moon phase replaced (-1 keeps the
	 * real one). The moon phase is the day number modulo 8; the night after day d shows the phase of day d.
	 */
	public static long visible(long totalTicks, int dayTicks, int moonPhase) {
		long day = Math.floorDiv(totalTicks, (long) DAY);
		if (moonPhase >= 0) day = day - Math.floorMod(day, MOON_PHASES) + moonPhase;
		return day * DAY + (dayTicks >= 0 ? dayTicks : dayTicks(totalTicks));
	}

	/** Same day as {@code totalTicks}, with the time of day replaced. */
	public static long withDayTicks(long totalTicks, int dayTicks) {
		return Math.floorDiv(totalTicks, (long) DAY) * DAY + dayTicks;
	}
}
