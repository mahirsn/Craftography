package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.Autofocus;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.render.LensEffects;
import io.github.mahirsn.craftography.render.Meter;
import io.github.mahirsn.craftography.screen.ControlsScreen;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.ExposureMath;
import io.github.mahirsn.craftography.util.Rect;
import io.github.mahirsn.craftography.util.TimeMath;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Minimal camcorder viewfinder. The scene and saved photo share the full-screen aspect frame. */
public final class ViewfinderHud {
	private ViewfinderHud() { }

	public static Rect liveView(double width, double height) {
		return new Rect(0, 0, width, height);
	}

	/** Frame rectangle in GUI units. */
	public static Rect currentFrame(double width, double height) {
		return AspectMask.frame(liveView(width, height), CameraSettings.get().aspect);
	}

	/** Time of day shown in the picture: the override, or the server time. */
	public static int visibleDayTicks(CameraSettings s) {
		return s.timeOfDay >= 0 ? s.timeOfDay : TimeMath.dayTicks(CameraMode.serverClockTicks);
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		CameraSettings s = CameraSettings.get();
		float p = CameraMode.progress();
		Rect lv = liveView(g.guiWidth(), g.guiHeight());
		Rect f = AspectMask.frame(lv, s.aspect);
		AspectMask.draw(g, lv, f, s.maskOpacity * p, 0.8f * p);
		GuideOverlay.draw(g, f, s.guide, s.guideOpacity * p);
		focusPoint(g, f, s, p);
		if (!ControlsScreen.isOpen()) LevelGauge.draw(g, lv, s.roll, p);
		if (s.histogram) histogram(g, lv, p);
		readouts(g, s, p);
		Shutter.draw(g, f, lv);
	}

	private static void readouts(GuiGraphicsExtractor g, CameraSettings s, float alpha) {
		Font font = Minecraft.getInstance().font;
		int white = Draw.argb(alpha, 0xF2EFE6);
		int amber = Draw.argb(alpha, 0xFFC85A);
		String status = Shutter.timerRunning() ? "● " + Component.translatable("craftography.status.timer").getString() + " " + Shutter.timerSeconds() + "s"
			: Component.translatable("craftography.status.ready").getString();
		g.text(font, status, 12, 12, Shutter.timerLampLit() ? Draw.argb(alpha, 0xFF6655) : white, true);
		if (!ControlsScreen.isOpen()) {
			String clock = TimeMath.format(visibleDayTicks(s));
			g.text(font, clock, g.guiWidth() - font.width(clock) - 12, 12, white, true);
			if (io.github.mahirsn.craftography.Tripod.isActive()) {
				Draw.camera(g, "ui/icon_tripod", g.guiWidth() - 25, 28, 11, 11, amber);
			}
		}
		String lens = Math.round(s.focalLength) + "mm";
		String exposure = lens + "   ISO " + s.iso + "   " + ExposureMath.formatAperture(s.aperture) + "   " + ExposureMath.formatShutter(s.shutter);
		int available = ControlsScreen.isOpen() ? (int) io.github.mahirsn.craftography.screen.DockLayout.of(g.guiWidth(), g.guiHeight()).panel().x() - 20 : g.guiWidth() - 24;
		if (font.width(exposure) > available) exposure = lens + "   ISO " + s.iso;
		g.text(font, exposure, 12, g.guiHeight() - 25, white, true);
		String info = s.kelvin + "K   " + (s.autofocus ? "AF " : "MF ") + ExposureMath.formatFocus(Autofocus.distance());
		if (s.selfTimer > 0) info += "   " + s.selfTimer + "s";
		if (font.width(info) <= available) g.text(font, info, 12, g.guiHeight() - 13, white, true);
	}

	/** Autofocus point in the frame centre. */
	private static void focusPoint(GuiGraphicsExtractor g, Rect f, CameraSettings s, float alpha) {
		double cx = f.x() + f.w() / 2, cy = f.y() + f.h() / 2;
		boolean dof = LensEffects.depthOfFieldAvailable();
		int color = Draw.argb(0.85f * alpha, s.autofocus && dof ? 0x7CFC64 : 0xFFFFFF);
		if (s.autofocus) Draw.sprite(g, "af", cx - 6.5, cy - 4.5, 13, 9, color);
	}

	/** Luminance histogram in the top left of the live view. */
	private static void histogram(GuiGraphicsExtractor g, Rect lv, float alpha) {
		float[] bins = Meter.histogram();
		int w = Meter.BINS, h = 28;
		int x = (int) lv.x() + 12, y = (int) lv.y() + 30;
		Draw.rect(g, x - 1, y - 1, x + w + 1, y + h + 1, Draw.argb(0.5f * alpha, 0x000000));
		int bar = Draw.argb(0.85f * alpha, 0xFFFFFF);
		for (int i = 0; i < w; i++) {
			int bh = Math.round(bins[i] * h);
			if (bh > 0) g.fill(x + i, y + h - bh, x + i + 1, y + h, bar);
		}
	}
}
