package io.github.mahirsn.craftography.capture;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.hud.AspectMask;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Takes the picture. The capture runs after the level is drawn and before any GUI is drawn in the same frame, so the
 * image never contains the HUD. The image is cropped to the frame and saved as PNG in screenshots/craftography.
 */
public final class Shutter {
	private static final Identifier PREVIEW = Identifier.fromNamespaceAndPath(CraftographyClient.MOD_ID, "shutter_preview");
	private static final long CURTAIN_MS = 220, PREVIEW_MS = 2000;
	private static final int PREVIEW_WIDTH = 120;

	private static boolean pending;
	private static long shotAt = -1;
	private static DynamicTexture preview;
	private static long previewAt = -1;

	private Shutter() {
	}

	public static void press() {
		if (pending) return;
		pending = true;
		shotAt = Util.getMillis();
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.9f, 0.6f));
	}

	public static boolean isPending() {
		return pending;
	}

	/** Called by GameRendererMixin right before the GUI is drawn. */
	public static void capture() {
		pending = false;
		Minecraft mc = Minecraft.getInstance();
		CameraSettings s = CameraSettings.get();
		CameraSettings.Aspect aspect = s.aspect;
		long mm = Math.round(CameraMode.focalLength());
		Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
			Rect r = AspectMask.frame(image.getWidth(), image.getHeight(), aspect);
			int x = (int) Math.round(r.x()), y = (int) Math.round(r.y());
			int w = Math.min((int) Math.round(r.w()), image.getWidth() - x), h = Math.min((int) Math.round(r.h()), image.getHeight() - y);
			NativeImage photo = new NativeImage(w, h, false);
			image.copyRect(photo, x, y, 0, 0, w, h, false, false);
			image.close();
			if (s.showPreview) showPreview(mc, photo);
			Path dir = mc.gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR).resolve("craftography");
			String name = Util.getFilenameFormattedDateTime() + "_" + mm + "mm_" + (aspect == CameraSettings.Aspect.FREE ? "free" : aspect.fileLabel());
			Util.ioPool().execute(() -> save(mc, dir, name, photo));
		});
	}

	private static void save(Minecraft mc, Path dir, String name, NativeImage photo) {
		try (photo) {
			Files.createDirectories(dir);
			Path file = dir.resolve(name + ".png");
			for (int i = 2; Files.exists(file); i++) file = dir.resolve(name + "_" + i + ".png");
			photo.writeToFile(file);
			CraftographyClient.LOG.info("Saved {}", file);
		} catch (Exception e) {
			CraftographyClient.LOG.warn("Could not save photo", e);
			mc.execute(() -> mc.gui.hud.getChat().addClientSystemMessage(Component.translatable("craftography.shutter.failed", e.getMessage())));
		}
	}

	private static void showPreview(Minecraft mc, NativeImage photo) {
		int pw = Math.min(PREVIEW_WIDTH * 4, photo.getWidth());
		int ph = Math.max(1, Math.round((float) photo.getHeight() * pw / photo.getWidth()));
		NativeImage small = new NativeImage(pw, ph, false);
		photo.resizeSubRectTo(0, 0, photo.getWidth(), photo.getHeight(), small);
		mc.execute(() -> {
			if (preview != null) mc.getTextureManager().release(PREVIEW);
			preview = new DynamicTexture(() -> "Craftography preview", small);
			mc.getTextureManager().register(PREVIEW, preview);
			previewAt = Util.getMillis();
		});
	}

	/** Curtain over the frame and the preview in the corner. Drawn by the viewfinder HUD. */
	public static void draw(GuiGraphicsExtractor g, Rect f) {
		long now = Util.getMillis();
		if (shotAt >= 0 && now - shotAt < CURTAIN_MS) {
			// Closes in the first half, opens in the second half.
			double t = (now - shotAt) / (double) CURTAIN_MS;
			double cover = (t < 0.5 ? t * 2 : (1 - t) * 2) * f.h() / 2;
			int black = Draw.argb(1f, 0x000000);
			Draw.rect(g, f.x(), f.y(), f.right(), f.y() + cover, black);
			Draw.rect(g, f.x(), f.bottom() - cover, f.right(), f.bottom(), black);
		}
		if (preview != null && previewAt >= 0 && now - previewAt < PREVIEW_MS) {
			NativeImage px = preview.getPixels();
			if (px == null) return;
			int w = PREVIEW_WIDTH, h = Math.round((float) px.getHeight() * w / px.getWidth());
			int x = g.guiWidth() - w - 6, y = 6;
			float fade = Math.min(1f, (PREVIEW_MS - (now - previewAt)) / 300f);
			g.blit(RenderPipelines.GUI_TEXTURED, PREVIEW, x, y, 0, 0, w, h, px.getWidth(), px.getHeight(), px.getWidth(), px.getHeight(), Draw.argb(fade, 0xFFFFFF));
			int line = Draw.argb(fade, 0xFFFFFF);
			Draw.line(g, x, y, x + w, y, line);
			Draw.line(g, x, y + h, x + w, y + h, line);
			Draw.line(g, x, y, x, y + h, line);
			Draw.line(g, x + w, y, x + w, y + h, line);
		}
	}
}
