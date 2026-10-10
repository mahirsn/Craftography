package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.Autofocus;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.hud.BodyControls.Control;
import io.github.mahirsn.craftography.hud.BodyLayout.Part;
import io.github.mahirsn.craftography.render.LensEffects;
import io.github.mahirsn.craftography.render.Meter;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.ExposureMath;
import io.github.mahirsn.craftography.util.Rect;
import io.github.mahirsn.craftography.util.TimeMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.Locale;

/**
 * The camera held in the hands, drawn in pixel art around the live view: lens barrel with zoom, focus and
 * aperture rings, silver top plate with dials, shutter release and status LCD, black leatherette body with a grip,
 * and the rear buttons and wheels. Every value shown comes from {@link BodyControls}.
 */
public final class CameraBody {
	/** Engraved text on metal, as on Exposure's camera controls. */
	static final int ENGRAVED = 0x2B2622, ENGRAVED_LIGHT = 0x7A736C;
	static final int PRINT = 0xE4E0D8, AMBER = 0xFFB000, LCD_TEXT = 0x1E231C;
	private static final double[] ZOOM_MARKS = {14, 24, 35, 50, 85, 135, 200};
	private static final double[] FOCUS_MARKS = {0.3, 0.5, 1, 2, 5, 10, ExposureMath.FOCUS_INFINITY};
	private static final double[] APERTURE_MARKS = {1.4, 2, 2.8, 4, 5.6, 8, 11, 16, 22};

	private CameraBody() {
	}

	/** Darkens the world around the camera, so the camera reads as an object in front of it. */
	public static void drawSurroundings(GuiGraphicsExtractor g, Rect lcd, float alpha) {
		int c = Draw.argb(0.82f * alpha, 0x000000);
		int w = g.guiWidth(), h = g.guiHeight();
		Draw.rect(g, 0, 0, w, lcd.y(), c);
		Draw.rect(g, 0, lcd.bottom(), w, h, c);
		Draw.rect(g, 0, lcd.y(), lcd.x(), lcd.bottom(), c);
		Draw.rect(g, lcd.right(), lcd.y(), w, lcd.bottom(), c);
	}

	public static void draw(GuiGraphicsExtractor g, BodyLayout l, Part hovered) {
		Font font = Minecraft.getInstance().font;
		CameraSettings s = CameraSettings.get();

		// Lens barrel (furthest away first).
		Draw.camera(g, "body/lens_front", l.lens.x(), l.lens.y(), (int) l.lens.w(), (int) l.lens.h(), -1);
		ring(g, l, Part.ZOOM, "ring_rubber", hovered, font);
		ring(g, l, Part.FOCUS, "ring_rubber", hovered, font);
		ring(g, l, Part.APERTURE, "ring_metal", hovered, font);
		Draw.camera(g, "body/lens_mount", l.mount.x(), l.mount.y(), (int) l.mount.w(), (int) l.mount.h(), -1);

		// Body, grip, top plate.
		// The rear plate is drawn in four clipped strips around the LCD, so it never covers the live view.
		Rect hole = l.lcd;
		int x0 = (int) l.body.x(), y0 = (int) l.body.y(), x1 = (int) l.body.right(), y1 = (int) l.body.bottom();
		int[][] strips = {{x0, y0, x1, (int) hole.y()}, {x0, (int) hole.bottom(), x1, y1}, {x0, (int) hole.y(), (int) hole.x(), (int) hole.bottom()},
			{(int) hole.right(), (int) hole.y(), x1, (int) hole.bottom()}};
		for (int[] st : strips) {
			g.enableScissor(st[0], st[1], st[2], st[3]);
			Draw.camera(g, "body/body", x0, y0, x1 - x0, y1 - y0, -1);
			g.disableScissor();
		}
		Draw.camera(g, "body/grip", l.grip.x(), l.grip.y(), (int) l.grip.w(), (int) l.grip.h(), -1);
		Draw.camera(g, "body/topplate", l.topPlate.x(), l.topPlate.y(), (int) l.topPlate.w(), (int) l.topPlate.h(), -1);
		engraved(g, font, "CRAFTOGRAPHY", (int) l.topPlate.x() + 8, (int) l.topPlate.y() + 12);
		Draw.camera(g, "body/hump", l.hump.x(), l.hump.y(), (int) l.hump.w(), (int) l.hump.h(), -1);
		dial(g, l, Part.TIME, "dial_time", TimeMath.format(ViewfinderHud.visibleDayTicks(s)), hovered, font);
		dial(g, l, Part.ISO, "dial", String.valueOf(s.iso), hovered, font);
		String speed = ExposureMath.formatShutter(s.shutter);
		dial(g, l, Part.SHUTTER_SPEED, "dial", speed.startsWith("1/") ? speed.substring(2) : speed, hovered, font);
		Rect release = l.get(Part.SHUTTER);
		Draw.camera(g, "body/shutter_button" + (hovered == Part.SHUTTER ? "_hl" : ""), release.x(), release.y(), 20, 14, -1);
		topLcd(g, l, s, font);
		selfTimerLamp(g, l);

		// Rear LCD surround and controls.
		Rect lcd = l.lcd;
		int b = BodyLayout.BEZEL;
		Draw.camera(g, "body/bezel", lcd.x() - b, lcd.y() - b, (int) lcd.w() + 2 * b, (int) lcd.h() + 2 * b, -1);
		for (Control c : BodyControls.all()) {
			switch (c.kind()) {
				case BUTTON -> button(g, l.get(c.part()), c, hovered == c.part());
				case WHEEL -> wheel(g, l.get(c.part()), c, hovered == c.part());
				case SWITCH -> focusSwitch(g, l.get(c.part()), s.autofocus, hovered == c.part(), font);
				default -> {
				}
			}
		}
		rightArm(g, l);
	}

	/**
	 * The player's own right arm holding the grip, drawn from the player's skin like a first-person arm: the back of
	 * the arm (and sleeve layer) at 4x scale, with the inner side in shade.
	 */
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

	private static void engraved(GuiGraphicsExtractor g, Font font, String text, int x, int y) {
		g.text(font, text, x, y + 1, Draw.argb(1f, ENGRAVED_LIGHT), false);
		g.text(font, text, x, y, Draw.argb(1f, ENGRAVED), false);
	}

	private static void dial(GuiGraphicsExtractor g, BodyLayout l, Part part, String sprite, String text, Part hovered, Font font) {
		Rect r = l.get(part);
		Draw.camera(g, "body/" + sprite + (hovered == part ? "_hl" : ""), r.x(), r.y(), (int) r.w(), (int) r.h(), -1);
		int x = (int) (r.x() + (r.w() - font.width(text)) / 2);
		engraved(g, font, text, x, (int) r.y() + 2);
	}

	/** A lens ring: the printed scale turns with the setting under a fixed index mark in the middle. */
	private static void ring(GuiGraphicsExtractor g, BodyLayout l, Part part, String sprite, Part hovered, Font font) {
		Rect r = l.get(part);
		Draw.camera(g, "body/" + sprite + (hovered == part ? "_hl" : ""), r.x(), r.y(), (int) r.w(), (int) r.h(), -1);
		CameraSettings s = CameraSettings.get();
		double[] marks;
		double current;
		java.util.function.DoubleUnaryOperator pos;
		double span;
		switch (part) {
			case ZOOM -> {
				marks = ZOOM_MARKS;
				pos = Math::log;
				current = s.focalLength;
				span = Math.log(200 / 10.0);
			}
			case FOCUS -> {
				marks = FOCUS_MARKS;
				// Log distance scale; infinity sits a little beyond 10 m, as on a real lens.
				pos = d -> Math.log(Math.min(d, 40));
				current = Autofocus.distance();
				span = Math.log(40 / ExposureMath.MIN_FOCUS);
			}
			default -> {
				marks = APERTURE_MARKS;
				pos = n -> Math.log(n * n) / Math.log(2);
				current = s.aperture;
				span = 8;
			}
		}
		// The ring shows a little more than half of its scale at a time.
		double cx = r.x() + r.w() / 2, scale = (r.w() - 10) / (span * 0.6);
		int textY = (int) r.y() + 2;
		int color = Draw.argb(0.95f, part == Part.APERTURE ? 0xF0E8D0 : PRINT);
		g.enableScissor((int) r.x() + 3, (int) r.y() + 1, (int) r.right() - 3, (int) r.bottom() - 1);
		for (double m : marks) {
			double x = cx + (pos.applyAsDouble(m) - pos.applyAsDouble(current)) * scale;
			String label = part == Part.FOCUS ? (m >= ExposureMath.FOCUS_INFINITY ? "∞" : m < 1 ? String.valueOf(m).substring(1) : String.valueOf((int) m))
				: part == Part.APERTURE ? (m == Math.floor(m) ? String.valueOf((int) m) : String.valueOf(m)) : String.valueOf((int) m);
			g.text(font, label, (int) Math.round(x - font.width(label) / 2.0), textY, color, false);
		}
		g.disableScissor();
		// Index mark: amber line on the ring's near edge.
		g.fill((int) cx, (int) r.bottom() - 2, (int) cx + 1, (int) r.bottom(), Draw.argb(1f, AMBER));
		if (part == Part.FOCUS && s.autofocus) g.text(font, "AF", (int) r.right() - 4 - font.width("AF"), textY, Draw.argb(1f, AMBER), false);
	}

	private static void icon(GuiGraphicsExtractor g, String name, double x, double y, int color) {
		Draw.camera(g, "body/icon_" + name, x, y, 11, 9, color);
	}

	private static void button(GuiGraphicsExtractor g, Rect r, Control c, boolean hovered) {
		String sprite = "body/button" + (c.lit().getAsBoolean() ? "_on" : "") + (hovered ? "_hl" : "");
		Draw.camera(g, sprite, r.x(), r.y(), (int) r.w(), (int) r.h(), -1);
		icon(g, c.icon().get(), r.x() + 5, r.y() + 4, Draw.argb(1f, PRINT));
	}

	private static void wheel(GuiGraphicsExtractor g, Rect r, Control c, boolean hovered) {
		icon(g, c.icon().get(), r.x(), r.y() + 0.5, Draw.argb(0.9f, PRINT));
		Draw.camera(g, "body/wheel" + (hovered ? "_hl" : ""), r.x() + 14, r.y(), 28, 10, -1);
	}

	private static void focusSwitch(GuiGraphicsExtractor g, Rect r, boolean auto, boolean hovered, Font font) {
		Draw.camera(g, "body/switch_" + (auto ? "af" : "mf") + (hovered ? "_hl" : ""), r.x(), r.y(), 40, 12, -1);
		int x = (int) r.x() + (auto ? 2 : 22) + 3;
		g.text(font, auto ? "AF" : "MF", x, (int) r.y() + 2, Draw.argb(1f, ENGRAVED), false);
	}

	/** Status panel: exposure scale with the meter needle, aperture, white balance and focus. */
	private static void topLcd(GuiGraphicsExtractor g, BodyLayout l, CameraSettings s, Font font) {
		Rect r = l.get(Part.TOP_LCD);
		if (r.w() < 40) return;
		Draw.camera(g, "body/toplcd", r.x(), r.y(), (int) r.w(), (int) r.h(), -1);
		int ink = Draw.argb(1f, LCD_TEXT);
		int x = (int) r.x() + 3, y = (int) r.y() + 2;
		g.enableScissor((int) r.x() + 2, (int) r.y() + 2, (int) r.right() - 2, (int) r.bottom() - 2);
		// Exposure scale -3 .. +3 stops; the needle shows the light meter.
		int scaleW = (int) Math.min(48, r.w() - 6), mid = x + scaleW / 2;
		for (int i = -3; i <= 3; i++) {
			int tx = mid + i * (scaleW / 2) / 3;
			g.fill(tx, y + 1, tx + 1, y + (i == 0 ? 6 : 4), ink);
		}
		g.fill(x, y + 3, x + scaleW + 1, y + 4, ink);
		double level = Meter.level();
		if (!Double.isNaN(level)) {
			int nx = mid + (int) Math.round(Math.clamp(level, -3.3, 3.3) * (scaleW / 2) / 3.0);
			g.fill(nx - 1, y + 5, nx + 2, y + 7, ink);
		}
		String stops = Double.isNaN(level) ? "" : String.format(Locale.ROOT, "%+.1f", level);
		g.text(font, stops, x + scaleW + 4, y, ink, false);
		String focus = LensEffects.depthOfFieldAvailable() ? (s.autofocus ? "AF " : "MF ") + ExposureMath.formatFocus(Autofocus.distance())
			: Component.translatable("craftography.dof.off_short").getString();
		g.text(font, ExposureMath.formatAperture(s.aperture) + " " + s.kelvin + "K " + focus, x, y + 9, ink, false);
		g.disableScissor();
	}

	/** Self-timer lamp beside the brand name: blinks while the timer counts down. */
	private static void selfTimerLamp(GuiGraphicsExtractor g, BodyLayout l) {
		boolean lit = Shutter.timerLampLit();
		int x = (int) l.topPlate.x() + 8 + Minecraft.getInstance().font.width("CRAFTOGRAPHY") + 5;
		Draw.camera(g, "body/icon_lamp", x, l.topPlate.y() + 13, 5, 5, Draw.argb(1f, lit ? 0xFF3A2A : 0x5A1A14));
	}
}
