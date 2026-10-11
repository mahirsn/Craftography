package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Text above the frame: title on the left; shutter key hint, aspect ratio and lens on the right. */
public final class InfoStrip {
	static final int WHITE = 0xFFFFFF, AMBER = 0xFFB000;

	private InfoStrip() {
	}

	public static void draw(GuiGraphicsExtractor g, Rect f, CameraSettings s, double mm, float alpha) {
		Font font = Minecraft.getInstance().font;
		int white = Draw.argb(alpha, WHITE), amber = Draw.argb(alpha, AMBER);
		boolean bezel = s.aspect != CameraSettings.Aspect.FREE;
		int top = (int) Math.max(2, f.y() - (bezel ? AspectMask.BEZEL : 0) - font.lineHeight - 2);
		int left = (int) Math.max(4, f.x());
		int right = (int) Math.min(g.guiWidth() - 4, f.right());
		String aspect = s.aspect == CameraSettings.Aspect.FREE ? Component.translatable("craftography.aspect.free").getString() : s.aspect.label;
		String lens = aspect + "   " + Math.round(mm) + "mm";
		// A narrow frame (9:16, 4:5) has no room for both texts above it: use the screen width instead.
		if (right - left < font.width("CRAFTOGRAPHY") + 12 + font.width(lens)) {
			left = 4;
			right = g.guiWidth() - 4;
		}

		g.text(font, "CRAFTOGRAPHY", left, top, white, false);
		int lensX = right - font.width(lens);
		g.text(font, lens, lensX, top, amber, false);
		if (CraftographyClient.shutterKey.isUnbound()) return;
		Component hint = Component.translatable("craftography.hint.shoot", CraftographyClient.shutterKey.getTranslatedKeyMessage());
		int hintX = lensX - 12 - font.width(hint);
		if (hintX > left + font.width("CRAFTOGRAPHY") + 12) g.text(font, hint, hintX, top, white, false);
	}
}
