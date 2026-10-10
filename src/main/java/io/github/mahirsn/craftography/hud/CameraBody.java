package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.hud.BodyControls.Control;
import io.github.mahirsn.craftography.hud.BodyControls.Page;
import io.github.mahirsn.craftography.hud.BodyLayout.Part;
import io.github.mahirsn.craftography.render.LensEffects;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.ExposureMath;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/** First-person rear camera housing. Hardware keys and wheels are drawn where they are actually hit-tested. */
public final class CameraBody {
	static final int AMBER = 0xFFB000;
	private static final int PRINT = 0xD6DAD8;
	private CameraBody() { }

	private static void text(GuiGraphicsExtractor g, BodyLayout l, String text, double x, double y, int color) {
		g.text(Minecraft.getInstance().font, text, (int) (l.x + x * l.scale), (int) (l.y + y * l.scale), Draw.argb(1f, color), false);
	}
	public static void draw(GuiGraphicsExtractor g, BodyLayout l, Part hovered) {
		Font font = Minecraft.getInstance().font;
		CameraSettings s = CameraSettings.get();
		Draw.camera(g, "body/camcorder", l.x, l.y, (int) Math.round(480 * l.scale), (int) Math.round(270 * l.scale), -1);
		Rect zoom = l.get(Part.ZOOM), release = l.get(Part.SHUTTER);
		g.pose().pushMatrix();
		g.pose().translate((float) (zoom.x() + zoom.w() / 2), (float) (zoom.y() + zoom.h() / 2));
		g.pose().rotate((float) (Math.log(s.focalLength / 35.0) * 0.7));
		Draw.camera(g, "body/zoom_rocker" + (hovered == Part.ZOOM ? "_hl" : ""), -zoom.w() / 2, -zoom.h() / 2, (int) zoom.w(), (int) zoom.h(), -1);
		g.pose().popMatrix();
		Draw.camera(g, "body/capture_cap" + (hovered == Part.SHUTTER ? "_hl" : ""), release.x(), release.y(), (int) release.w(), (int) release.h(), -1);
		Draw.camera(g, "body/icon_capture", release.x() + 5, release.y() + 4, 10, 8, Draw.argb(1f, PRINT));
		// Six small physical mode keys sit beside the LCD.
		for (Page page : Page.values()) {
			Rect r = l.pageButton(page.ordinal());
			Draw.camera(g, "body/button" + (page == BodyControls.page() ? "_on" : ""), r.x(), r.y(), (int) r.w(), (int) r.h(), -1);
			String icon = switch (page) {
				case EXPOSURE -> "exposure"; case COLOUR -> "kelvin"; case FOCUS -> "focus";
				case FRAME -> "frame"; case WORLD -> "clock"; case DRIVE -> "drive";
			};
			Draw.camera(g, "body/icon_" + icon, r.x() + (r.w() - 11) / 2, r.y() + (r.h() - 9) / 2, 11, 9, Draw.argb(1f, page == BodyControls.page() ? AMBER : PRINT));
		}
		Rect lcd = l.lcd;
		// The DSLR has its own deep black LCD surround in the housing sprite.
		for (Part part : BodyControls.page().parts) hardware(g, l, BodyControls.get(part), part == hovered);
		// Readouts are on the camera's LCD, while the picture remains inside the display.
		String info = Math.round(s.focalLength) + "mm  " + ExposureMath.formatAperture(s.aperture) + "  " + ExposureMath.formatShutter(s.shutter) + "  ISO " + s.iso;
		boolean clockBar = io.github.mahirsn.craftography.screen.ControlsScreen.isOpen() && BodyControls.page() == Page.WORLD;
		int infoX = clockBar ? (int) l.lcd.right() - font.width(info) - 5 : (int) l.lcd.x() + 5;
		int infoY = clockBar ? (int) l.lcd.y() + 18 : (int) l.lcd.bottom() - 11;
		g.text(font, info, infoX, infoY, Draw.argb(0.9f, PRINT), true);
		rightArm(g, l);
		String state = Shutter.timerRunning() ? "TIMER " + Shutter.timerSeconds() + "s" : "STBY";
		g.text(font, state, (int) l.lcd.x() + 5, (int) l.lcd.y() + 5, Draw.argb(1f, Shutter.timerLampLit() ? 0xFF6557 : PRINT), true);
		if (!LensEffects.depthOfFieldAvailable()) g.text(font, "DOF OFF", (int) l.lcd.right() - font.width("DOF OFF") - 5, (int) l.lcd.y() + 5, Draw.argb(1f, AMBER), true);
	}
	private static void rightArm(GuiGraphicsExtractor g, BodyLayout l) {
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		PlayerSkin skin = player.getSkin();
		Identifier texture = skin.body().texturePath();
		int armW = skin.model() == PlayerModelType.SLIM ? 3 : 4;
		// As wide as the grip. The texture is flipped so the hand (the bottom of the arm texture) holds the grip and
		// the arm leaves the screen at the bottom.
		int scale = 5, w = armW * scale, h = 12 * scale;
		int x = (int) l.grip.right() - w + 2, y = (int) l.grip.y() + 16;
		int side = 6;
		// Skin layout of the right arm (row v 20..32, sleeve layer v 36..48): outer side, front, inner side (4 wide), back.
		int inner = 44 + armW, back = 48 + armW;
		g.blit(RenderPipelines.GUI_TEXTURED, texture, x - side, y, inner, 32, side, h, 4, -12, 64, 64, Draw.argb(1f, 0x8A8A8A));
		g.blit(RenderPipelines.GUI_TEXTURED, texture, x - side, y, inner, 48, side, h, 4, -12, 64, 64, Draw.argb(1f, 0x8A8A8A));
		g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, back, 32, w, h, armW, -12, 64, 64, -1);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, back, 48, w, h, armW, -12, 64, 64, -1);
		// The arm goes down out of the light.
		for (int i = 0; i < 12; i++) {
			g.fill(x - side, y + h / 2 + i * 2, x + w, y + h / 2 + i * 2 + 2, Draw.argb(i * 0.035f, 0x000000));
		}
	}

	private static String label(Part p) {
		return switch (p) {
			case ISO -> "ISO"; case APERTURE -> "IRIS"; case SHUTTER_SPEED -> "SPEED";
			case NOISE -> "GRAIN"; case HISTOGRAM -> "HIST"; case KELVIN -> "WB"; case TINT -> "TINT";
			case FOCUS -> "FOCUS"; case FOCUS_MODE -> "AF / MF"; case PEAKING -> "PEAK";
			case ASPECT -> "ASPECT"; case GUIDE -> "GUIDE"; case LEVEL -> "LEVEL"; case MASK -> "MASK"; case GRID -> "GRID";
			case TIME -> "CLOCK"; case WEATHER -> "WEATHER"; case MOON -> "MOON"; case SERVER_TIME -> "RESET"; case LOCK -> "LOCK";
			case TIMER -> "TIMER"; case TRIPOD -> "TRIPOD"; case PREVIEW -> "PLAY";
			default -> "";
		};
	}
	private static void hardware(GuiGraphicsExtractor g, BodyLayout l, Control c, boolean hover) {
		Rect r = l.get(c.part());
		Font font = Minecraft.getInstance().font;
		if (c.step() != null || r.h() / l.scale >= 28) {
			Draw.camera(g, "body/control_dial" + (hover ? "_hl" : ""), r.x(), r.y(), (int) r.w(), (int) r.h(), -1);
		} else {
			Draw.camera(g, "body/button" + (c.lit().getAsBoolean() ? "_on" : "") + (hover ? "_hl" : ""), r.x(), r.y(), (int) r.w(), (int) r.h(), -1);
		}
		String icon = c.icon() == null ? switch (c.part()) {
			case ISO -> "iso"; case APERTURE -> "iris"; case SHUTTER_SPEED -> "speed";
			case KELVIN -> "kelvin"; case TINT -> "tint"; case FOCUS, FOCUS_MODE -> "focus";
			default -> "frame";
		} : c.icon().get();
		Draw.camera(g, "body/icon_" + icon, r.x() + r.w() / 2 - 5, r.y() + r.h() / 2 - 4, 11, 9, Draw.argb(1f, hover ? AMBER : PRINT));
		if (hover && c.value() != null) {
			String readout = label(c.part()) + "  " + c.value().get().getString();
			readout = font.plainSubstrByWidth(readout, (int) l.lcd.w() - 12);
			int xx = (int) l.lcd.x() + 5, yy = (int) l.lcd.y() + 17;
			Draw.rect(g, xx - 2, yy - 2, xx + font.width(readout) + 2, yy + 10, 0xCC101318);
			g.text(font, readout, xx, yy, Draw.argb(1f, PRINT), false);
		}
	}
}
