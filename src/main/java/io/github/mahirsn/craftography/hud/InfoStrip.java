package io.github.mahirsn.craftography.hud;

import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Text around the frame: title and lens at the top, settings and key hint at the bottom. */
public final class InfoStrip {
	static final int WHITE = 0xFFFFFF, AMBER = 0xFFB000;

	private InfoStrip() {
	}

	public static void draw(GuiGraphicsExtractor g, Rect f, CameraSettings s, double mm, float alpha) {
		Font font = Minecraft.getInstance().font;
		int white = Draw.argb(alpha, WHITE), amber = Draw.argb(alpha, AMBER);
		int top = (int) Math.max(2, f.y() - font.lineHeight - 2);
		int left = (int) Math.max(4, f.x());
		int right = (int) Math.min(g.guiWidth() - 4, f.right());

		g.text(font, "CRAFTOGRAPHY", left, top, white, false);
		String aspect = s.aspect == CameraSettings.Aspect.FREE ? Component.translatable("craftography.aspect.free").getString() : s.aspect.label;
		String lens = aspect + "   " + Math.round(mm) + "mm";
		g.text(font, lens, right - font.width(lens), top, amber, false);

		int bottom = g.guiHeight() - font.lineHeight - 3;
		Component guide = Component.translatable(s.guide.translationKey());
		g.text(font, guide, left, bottom, white, false);
	}
}
