package io.github.mahirsn.craftography.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.Util;

import java.util.function.Supplier;

/**
 * Light meter and histogram. A few times a second the picture (after exposure, before live-view aids) is shrunk on
 * the GPU and read back. The meter is centre-weighted: the exposure level is how far the mean light of the picture
 * is from middle grey (18 %), in stops, like the exposure scale of a real camera.
 */
public final class Meter {
	public static final int WIDTH = 96, HEIGHT = 64, BINS = 64;
	private static final long INTERVAL_MS = 200;
	private static final double MIDDLE_GREY = 0.18;

	private static volatile float[] histogram = new float[BINS];
	private static volatile double level = Double.NaN;
	private static long lastRequest;
	private static boolean reading;

	private Meter() {
	}

	/** Normalised luminance histogram (highest bin = 1), dark to bright. */
	public static float[] histogram() {
		return histogram;
	}

	/** Exposure against middle grey in stops (0 = correct, + = bright), or NaN before the first reading. */
	public static double level() {
		return level;
	}

	static void request(Supplier<TextureTarget> render) {
		long now = Util.getMillis();
		if (reading || now - lastRequest < INTERVAL_MS) return;
		lastRequest = now;
		TextureTarget target = render.get();
		var texture = target.getColorTexture();
		GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Craftography meter", 9, (long) WIDTH * HEIGHT * 4);
		reading = true;
		RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(texture, buffer, 0, () -> {
			try (GpuBufferSlice.MappedView read = buffer.map(true, false)) {
				read(read);
			} finally {
				buffer.close();
				reading = false;
			}
		}, 0);
	}

	private static void read(GpuBufferSlice.MappedView view) {
		float[] bins = new float[BINS];
		double weighted = 0, weights = 0;
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				int abgr = view.data().getInt((x + y * WIDTH) * 4);
				double r = linear(abgr & 255), g = linear(abgr >> 8 & 255), b = linear(abgr >> 16 & 255);
				double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
				bins[Math.min(BINS - 1, (int) (srgb(luminance) * BINS))]++;
				// Centre-weighted: weight falls off towards the edges.
				double dx = (x + 0.5) / WIDTH - 0.5, dy = (y + 0.5) / HEIGHT - 0.5;
				double w = Math.exp(-(dx * dx + dy * dy) * 8);
				weighted += luminance * w;
				weights += w;
			}
		}
		float max = 0;
		for (float v : bins) max = Math.max(max, v);
		for (int i = 0; i < BINS; i++) bins[i] /= Math.max(1, max);
		histogram = bins;
		level = Math.log(Math.max(1e-4, weighted / weights) / MIDDLE_GREY) / Math.log(2);
	}

	private static double linear(int c) {
		double v = c / 255.0;
		return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
	}

	private static double srgb(double v) {
		return Math.min(0.9999, v <= 0.0031308 ? v * 12.92 : 1.055 * Math.pow(v, 1 / 2.4) - 0.055);
	}
}
