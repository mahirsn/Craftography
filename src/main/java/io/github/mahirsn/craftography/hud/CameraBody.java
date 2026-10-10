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
import java.util.Locale;

/** Perspective camcorder housing. Hardware keys and wheels are drawn where they are actually hit-tested. */
public final class CameraBody {
	static final int AMBER = 0xFFB000;
	private static final int PRINT = 0xD6DAD8;
	private CameraBody() { }

	public static void drawSurroundings(GuiGraphicsExtractor g, Rect lcd, float alpha) {
		int c = Draw.argb(0.65f * alpha, 0x000000);
		Draw.rect(g, 0, 0, g.guiWidth(), lcd.y(), c);
		Draw.rect(g, 0, lcd.bottom(), g.guiWidth(), g.guiHeight(), c);
		Draw.rect(g, 0, lcd.y(), lcd.x(), lcd.bottom(), c);
		Draw.rect(g, lcd.right(), lcd.y(), g.guiWidth(), lcd.bottom(), c);
	}
	private static void text(GuiGraphicsExtractor g, BodyLayout l, String text, double x, double y, int color) {
		g.text(Minecraft.getInstance().font, text, (int) (l.x + x * l.scale), (int) (l.y + y * l.scale), Draw.argb(1f, color), false);
	}
	public static void draw(GuiGraphicsExtractor g, BodyLayout l, Part hovered) {
		Font font = Minecraft.getInstance().font;
		CameraSettings s = CameraSettings.get();
		Draw.camera(g, "body/camcorder", l.x, l.y, (int) Math.round(480 * l.scale), (int) Math.round(270 * l.scale), -1);
		Rect zoom = l.get(Part.ZOOM), release = l.get(Part.SHUTTER);
		Draw.camera(g, "body/zoom_rocker" + (hovered == Part.ZOOM ? "_hl" : ""), zoom.x(), zoom.y(), (int) zoom.w(), (int) zoom.h(), -1);
		text(g, l, "W", 385, 33, PRINT); text(g, l, "T", 420, 33, PRINT);
		Draw.camera(g, "body/capture_cap" + (hovered == Part.SHUTTER ? "_hl" : ""), release.x(), release.y(), (int) release.w(), (int) release.h(), -1);
		text(g, l, "SNAP", 436, 24, PRINT);
		text(g, l, "CRAFTOGRAPHY", 242, 37, 0xB5BDC1);
		text(g, l, "HD / DIGITAL", 173, 52, 0x959FA5);
		text(g, l, pageLabel(), 403, 55, 0x99C593);
		// The six small physical mode keys live in the LCD's lower bezel, not in a floating settings panel.
		for (Page page : Page.values()) {
			Rect r = l.pageButton(page.ordinal());
			Draw.camera(g, "body/button" + (page == BodyControls.page() ? "_on" : ""), r.x(), r.y(), (int) r.w(), (int) r.h(), -1);
			String label = page.label;
			g.text(font, label, (int) (r.x() + (r.w() - font.width(label)) / 2), (int) r.y() + 3, Draw.argb(1f, page == BodyControls.page() ? AMBER : PRINT), false);
		}
		Rect lcd = l.lcd;
		Draw.camera(g, "body/bezel", lcd.x() - 8, lcd.y() - 8, (int) lcd.w() + 16, (int) lcd.h() + 16, -1);
		for (Part part : BodyControls.page().parts) hardware(g, l, BodyControls.get(part), part == hovered);
		// Readouts are on the camera's LCD, while the picture remains inside the display.
		String info = Math.round(s.focalLength) + "mm  " + ExposureMath.formatAperture(s.aperture) + "  " + ExposureMath.formatShutter(s.shutter);
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
		int x = (int) l.grip.right() - w + 2, y = (int) l.body.bottom() - h + 8;
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

	private static String pageLabel() { return BodyControls.page().label; }
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
		g.text(font, label(c.part()), (int) r.x(), (int) r.y(), Draw.argb(1f, 0xA7B0B5), false);
		if (c.step() != null) {
			Draw.camera(g, "body/wheel" + (hover ? "_hl" : ""), r.x(), r.y() + 21, (int) r.w(), 6, -1);
			String value = c.value().get().getString().replace("ISO ", "").replace(" (server)", "");
			value = font.plainSubstrByWidth(value, (int) r.w());
			Draw.rect(g, r.x() - 1, r.y() + 9, r.right() + 1, r.y() + 19, 0xFF111916);
			g.text(font, value, (int) r.x(), (int) r.y() + 10, Draw.argb(1f, hover ? AMBER : 0xA9C8A5), false);
		} else if (c.kind() == BodyControls.Kind.SWITCH) {
			Draw.camera(g, "body/switch_" + (CameraSettings.get().autofocus ? "af" : "mf") + (hover ? "_hl" : ""), r.x(), r.y() + 11, (int) r.w(), (int) (12 * l.scale), -1);
		} else {
			Draw.camera(g, "body/button" + (c.lit().getAsBoolean() ? "_on" : "") + (hover ? "_hl" : ""), r.x() + r.w() / 2 - 10, r.y() + 11, 20, 16, -1);
			if (c.icon() != null) Draw.camera(g, "body/icon_" + c.icon().get(), r.x() + r.w() / 2 - 5, r.y() + 15, 11, 9, Draw.argb(1f, PRINT));
		}
	}
}
