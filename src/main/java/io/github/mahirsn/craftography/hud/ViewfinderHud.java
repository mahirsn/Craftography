package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.render.Meter;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import io.github.mahirsn.craftography.util.TimeMath;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Draws the whole viewfinder in order. Reads settings only. */
public final class ViewfinderHud {
	private ViewfinderHud() {
	}

	/** Frame rectangle in GUI units, including the open/close animation. */
	public static Rect currentFrame(double width, double height) {
		Rect full = new Rect(0, 0, width, height);
		return full.lerp(AspectMask.frame(width, height, CameraSettings.get().aspect), CameraMode.progress());
	}

	/** Time of day shown in the picture: the override, or the server time. */
	public static int visibleDayTicks(CameraSettings s) {
		return s.timeOfDay >= 0 ? s.timeOfDay : TimeMath.dayTicks(CameraMode.serverClockTicks);
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		CameraSettings s = CameraSettings.get();
		float p = CameraMode.progress();
		Rect f = currentFrame(g.guiWidth(), g.guiHeight());
		AspectMask.draw(g, f, s.aspect != CameraSettings.Aspect.FREE, s.maskOpacity * p, 0.9f * p, p);
		GuideOverlay.draw(g, f, s.guide, s.guideOpacity * p);
		if (s.histogram) histogram(g, f, p);
		double mm = CameraMode.focalLength();
		int time = visibleDayTicks(s);
		TimeBar.draw(g, time, s.timeOfDay >= 0, p);
		CameraBar.draw(g, s, mm, time, p);
		Shutter.draw(g, f);
	}

	/** Live-view histogram in the picture's bottom-left corner, dark to bright; only while it is turned on. */
	private static void histogram(GuiGraphicsExtractor g, Rect f, float alpha) {
		float[] bins = Meter.histogram();
		double x = f.x() + 14, bottom = f.bottom() - 6, h = 20;
		Draw.rect(g, x - 2, bottom - h - 2, x + bins.length + 2, bottom + 2, Draw.argb(0.45f * alpha, 0x000000));
		int bar = Draw.argb(0.8f * alpha, 0xFFFFFF);
		for (int i = 0; i < bins.length; i++) Draw.rect(g, x + i, bottom - bins[i] * h, x + i + 1, bottom, bar);
	}
}
