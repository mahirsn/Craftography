package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
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
		AspectMask.draw(g, f, s.maskOpacity * p, 0.9f * p);
		GuideOverlay.draw(g, f, s.guide, s.guideOpacity * p);
		double mm = CameraMode.focalLength();
		ZoomScale.draw(g, f, mm, p);
		LevelGauge.draw(g, f, s.roll, p);
		int time = visibleDayTicks(s);
		TimeBar.draw(g, f, time, s.timeOfDay >= 0, p);
		InfoStrip.draw(g, f, s, mm, time, p);
	}
}
