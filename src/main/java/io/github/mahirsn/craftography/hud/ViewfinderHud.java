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

/**
 * Draws camera mode: the live view on the rear LCD (frame mask, guides, focus point, level, histogram, time bar)
 * and the camera body around it. Reads settings only.
 */
public final class ViewfinderHud {
	private ViewfinderHud() {
	}

	/** The live view in GUI units, including the camera’s rise from the bottom. */
	public static Rect liveView(double width, double height) {
		Rect lcd = BodyLayout.of(width, height).lcd;
		return lcd;
	}

	/** Fit LCD typography and instruments as a unit, including at large GUI scales. */
	public static double lcdScale(Rect lcd) { return Math.min(lcd.w()/244, lcd.h()/137); }

	/** Frame rectangle in GUI units, including the open/close animation. */
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
		BodyLayout l = BodyLayout.of(g.guiWidth(), g.guiHeight());
		Rect projected = liveView(g.guiWidth(), g.guiHeight());
		double uiScale = lcdScale(projected);
		g.pose().pushMatrix();
		g.pose().translate((float)projected.x(), (float)projected.y());
		g.pose().scale((float)uiScale, (float)uiScale);
		Rect lv = new Rect(0, 0, projected.w()/uiScale, projected.h()/uiScale);
		Rect f = AspectMask.frame(lv, s.aspect);

		AspectMask.draw(g, lv, f, s.maskOpacity * p, 0.9f * p);
		GuideOverlay.draw(g, f, s.guide, s.guideOpacity * p);
		focusPoint(g, f, s, p);
		LevelGauge.draw(g, lv, s.roll, p);
		if (s.histogram && ControlsScreen.hoveredPart() == null) histogram(g, lv, p);
		if (ControlsScreen.isOpen() && BodyControls.page() == BodyControls.Page.WORLD) TimeBar.draw(g, lv, visibleDayTicks(s), s.timeOfDay >= 0, p);
		Shutter.draw(g, f, lv);

		g.pose().popMatrix();
		CameraBody.draw(g, l, ControlsScreen.hoveredPart());
	}

	/** Autofocus point in the frame centre with the distance the lens is focused at. */
	private static void focusPoint(GuiGraphicsExtractor g, Rect f, CameraSettings s, float alpha) {
		Font font = Minecraft.getInstance().font;
		double cx = f.x() + f.w() / 2, cy = f.y() + f.h() / 2;
		boolean dof = LensEffects.depthOfFieldAvailable();
		int color = Draw.argb(0.85f * alpha, s.autofocus && dof ? 0xBCD6C4 : 0xFFFFFF);
		double acquire = HardwareMotion.acquisition();
		double radius = 1 + (1 - acquire) * .65;
		if (s.autofocus || HardwareMotion.halfPress()) {
			double rx = 6.5 * radius, ry = 4.5 * radius;
			for (int dx : new int[]{-1, 1}) for (int dy : new int[]{-1, 1}) {
				double xx = cx + dx * rx, yy = cy + dy * ry;
				Draw.line(g, xx, yy, xx - dx * 3, yy, color);
				Draw.line(g, xx, yy, xx, yy - dy * 2.5, color);
			}
		}
		String text = dof ? ExposureMath.formatFocus(Autofocus.distance()) : Component.translatable("craftography.dof.off_short").getString();
		g.text(font, text, (int) Math.round(cx - font.width(text) / 2.0), (int) (cy + 7), color, true);
	}

	/** Luminance histogram in the top left of the live view. */
	private static void histogram(GuiGraphicsExtractor g, Rect lv, float alpha) {
		float[] bins = Meter.histogram();
		int w = Meter.BINS, h = 20;
		int x = (int) lv.x() + 8, y = (int) lv.y() + 38;
		Draw.rect(g, x - 1, y - 1, x + w + 1, y + h + 1, Draw.argb(0.25f * alpha, 0x000000));
		int bar = Draw.argb(0.5f * alpha, 0xFFFFFF);
		for (int i = 0; i < w; i++) {
			int bh = Math.round(bins[i] * h);
			if (bh > 0) g.fill(x + i, y + h - bh, x + i + 1, y + h, bar);
		}
	}
}
