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

	/** The live view in GUI units, including the open/close animation (it grows from the whole screen). */
	public static Rect liveView(double width, double height) {
		Rect full = new Rect(0, 0, width, height);
		return full.lerp(BodyLayout.of(width, height).lcd, CameraMode.progress());
	}

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
		Rect lv = liveView(g.guiWidth(), g.guiHeight());
		Rect f = AspectMask.frame(lv, s.aspect);

		AspectMask.draw(g, lv, f, s.maskOpacity * p, 0.9f * p);
		GuideOverlay.draw(g, f, s.guide, s.guideOpacity * p);
		focusPoint(g, f, s, p);
		LevelGauge.draw(g, lv, s.roll, p);
		if (s.histogram) histogram(g, lv, p);
		if (ControlsScreen.isOpen()) TimeBar.draw(g, lv, visibleDayTicks(s), s.timeOfDay >= 0, p);
		Shutter.draw(g, f, lv);

		CameraBody.drawSurroundings(g, lv, p);
		g.pose().pushMatrix();
		// The camera is raised into view while camera mode opens.
		g.pose().translate(0, Math.round((1 - p) * g.guiHeight()));
		CameraBody.draw(g, l, ControlsScreen.hoveredPart());
		g.pose().popMatrix();
	}

	/** Autofocus point in the frame centre with the distance the lens is focused at. */
	private static void focusPoint(GuiGraphicsExtractor g, Rect f, CameraSettings s, float alpha) {
		Font font = Minecraft.getInstance().font;
		double cx = f.x() + f.w() / 2, cy = f.y() + f.h() / 2;
		boolean dof = LensEffects.depthOfFieldAvailable();
		int color = Draw.argb(0.85f * alpha, s.autofocus && dof ? 0x7CFC64 : 0xFFFFFF);
		if (s.autofocus) Draw.sprite(g, "af", cx - 6.5, cy - 4.5, 13, 9, color);
		String text = dof ? ExposureMath.formatFocus(Autofocus.distance()) : Component.translatable("craftography.dof.off_short").getString();
		g.text(font, text, (int) Math.round(cx - font.width(text) / 2.0), (int) (cy + 7), color, true);
	}

	/** Luminance histogram in the top left of the live view. */
	private static void histogram(GuiGraphicsExtractor g, Rect lv, float alpha) {
		float[] bins = Meter.histogram();
		int w = Meter.BINS, h = 28;
		int x = (int) lv.x() + 6, y = (int) lv.y() + 6;
		Draw.rect(g, x - 1, y - 1, x + w + 1, y + h + 1, Draw.argb(0.5f * alpha, 0x000000));
		int bar = Draw.argb(0.85f * alpha, 0xFFFFFF);
		for (int i = 0; i < w; i++) {
			int bh = Math.round(bins[i] * h);
			if (bh > 0) g.fill(x + i, y + h - bh, x + i + 1, y + h, bar);
		}
	}
}
