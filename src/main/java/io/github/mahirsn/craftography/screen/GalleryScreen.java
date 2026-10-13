package io.github.mahirsn.craftography.screen;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.util.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The photos taken with the camera ({@code screenshots/craftography}), newest first: a grid of thumbnails, and one
 * photo large with its details. Thumbnails are scaled down off the render thread and only the visible page is kept in
 * GPU memory.
 */
public class GalleryScreen extends Screen {
	private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
	private static final int COLS = 4, ROWS = 3, PER_PAGE = COLS * ROWS, THUMB = 192, GAP = 6;
	/** Craftography's file name: date_time_<mm>mm_<aspect>.png (see Shutter). */
	private static final Pattern NAME = Pattern.compile("^(.*)_(\\d+)mm_(.+)\\.png$");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT).withZone(ZoneId.systemDefault());

	private record Shot(Path file, Identifier id) {
	}

	private final Screen parent;
	private final Path dir;
	private final List<Shot> shots = new ArrayList<>();
	/** Loaded textures, with their pixel size; only the shots on screen are loaded. */
	private final Map<Identifier, int[]> loaded = new HashMap<>();
	private final Map<Identifier, Boolean> loading = new HashMap<>();
	private int page;
	/** The photo shown large, or -1 for the grid. */
	private int open = -1;
	private Identifier full;
	private int[] fullSize;

	public GalleryScreen(Screen parent) {
		super(Component.translatable("craftography.gallery"));
		this.parent = parent;
		Minecraft mc = Minecraft.getInstance();
		this.dir = mc.gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR).resolve("craftography");
		scan();
	}

	private void scan() {
		shots.clear();
		if (!Files.isDirectory(dir)) return;
		try (Stream<Path> files = Files.list(dir)) {
			List<Path> list = files.filter(p -> p.getFileName().toString().endsWith(".png"))
				.sorted(Comparator.comparingLong(GalleryScreen::modified).reversed()).toList();
			for (int i = 0; i < list.size(); i++) {
				shots.add(new Shot(list.get(i), Identifier.fromNamespaceAndPath(CraftographyClient.MOD_ID, "gallery/thumb_" + i)));
			}
		} catch (IOException e) {
			LOG.warn("Could not list {}: {}", dir, e.toString());
		}
	}

	private static long modified(Path p) {
		try {
			return Files.getLastModifiedTime(p).toMillis();
		} catch (IOException e) {
			return 0;
		}
	}

	private int pages() {
		return Math.max(1, (shots.size() + PER_PAGE - 1) / PER_PAGE);
	}

	/** Reads and scales a photo off the render thread, then uploads it on the render thread. */
	private void load(Path file, Identifier id, int maxWidth, boolean isFull) {
		if (loaded.containsKey(id) || loading.containsKey(id)) return;
		loading.put(id, true);
		Util.ioPool().execute(() -> {
			try (InputStream in = Files.newInputStream(file); NativeImage image = NativeImage.read(in)) {
				int w = Math.min(maxWidth, image.getWidth());
				int h = Math.max(1, Math.round((float) image.getHeight() * w / image.getWidth()));
				NativeImage scaled = new NativeImage(w, h, false);
				image.resizeSubRectTo(0, 0, image.getWidth(), image.getHeight(), scaled);
				int iw = image.getWidth(), ih = image.getHeight();
				minecraft.execute(() -> {
					loading.remove(id);
					if (minecraft.gui.screen() != this) {
						scaled.close();
						return;
					}
					minecraft.getTextureManager().register(id, new DynamicTexture(() -> "Craftography gallery", scaled));
					loaded.put(id, new int[]{w, h});
					if (isFull) fullSize = new int[]{iw, ih};
				});
			} catch (IOException e) {
				LOG.warn("Could not read {}: {}", file, e.toString());
				minecraft.execute(() -> loading.remove(id));
			}
		});
	}

	private void releaseAll() {
		for (Identifier id : loaded.keySet()) minecraft.getTextureManager().release(id);
		loaded.clear();
	}

	private void showPage(int p) {
		page = Math.clamp(p, 0, pages() - 1);
		releaseAll();
	}

	private void openShot(int i) {
		if (i < 0 || i >= shots.size()) return;
		if (full != null) {
			minecraft.getTextureManager().release(full);
			loaded.remove(full);
		}
		open = i;
		full = Identifier.fromNamespaceAndPath(CraftographyClient.MOD_ID, "gallery/full_" + i);
		fullSize = null;
		load(shots.get(i).file(), full, 1920, true);
	}

	// ---- layout -------------------------------------------------------------------------------------------------

	private int cell() {
		int byW = (width - 40 - (COLS - 1) * GAP) / COLS, byH = (height - 70 - (ROWS - 1) * GAP) / ROWS;
		return Math.max(24, Math.min(byW, byH * 16 / 9));
	}

	private int cellH() {
		return cell() * 9 / 16;
	}

	private int gridX() {
		return (width - (COLS * cell() + (COLS - 1) * GAP)) / 2;
	}

	private int gridY() {
		return 30;
	}

	private int at(double mx, double my) {
		int c = cell(), h = cellH();
		for (int k = 0; k < PER_PAGE; k++) {
			int i = page * PER_PAGE + k;
			if (i >= shots.size()) break;
			int x = gridX() + (k % COLS) * (c + GAP), y = gridY() + (k / COLS) * (h + GAP);
			if (mx >= x && mx < x + c && my >= y && my < y + h) return i;
		}
		return -1;
	}

	// ---- drawing ------------------------------------------------------------------------------------------------

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		g.fill(0, 0, width, height, 0xE0101113);
		if (open >= 0) {
			drawOpen(g);
			return;
		}
		String title = Component.translatable("craftography.gallery").getString() + "  " + (shots.isEmpty() ? "" : (page + 1) + "/" + pages());
		g.text(font, title, 20, 10, 0xFFFFB000, true);
		String hint = Component.translatable("craftography.gallery.hint").getString();
		g.text(font, hint, width - 20 - font.width(hint), 10, 0xFF9A9A9A, true);
		if (shots.isEmpty()) {
			Component empty = Component.translatable("craftography.gallery.empty");
			g.text(font, empty, (width - font.width(empty)) / 2, height / 2, 0xFFB0B0B0, true);
			return;
		}
		int c = cell(), h = cellH(), hovered = at(mouseX, mouseY);
		for (int k = 0; k < PER_PAGE; k++) {
			int i = page * PER_PAGE + k;
			if (i >= shots.size()) break;
			int x = gridX() + (k % COLS) * (c + GAP), y = gridY() + (k / COLS) * (h + GAP);
			Shot s = shots.get(i);
			g.fill(x - 1, y - 1, x + c + 1, y + h + 1, i == hovered ? 0xFFFFB000 : 0xFF2B2C2E);
			g.fill(x, y, x + c, y + h, 0xFF000000);
			int[] size = loaded.get(s.id());
			if (size == null) {
				load(s.file(), s.id(), THUMB, false);
				continue;
			}
			fit(g, s.id(), size, x, y, c, h);
		}
	}

	/** Draws a texture scaled to fit a box, centred, keeping its aspect ratio. */
	private static void fit(GuiGraphicsExtractor g, Identifier id, int[] size, int x, int y, int w, int h) {
		double k = Math.min(w / (double) size[0], h / (double) size[1]);
		int dw = Math.max(1, (int) (size[0] * k)), dh = Math.max(1, (int) (size[1] * k));
		g.blit(RenderPipelines.GUI_TEXTURED, id, x + (w - dw) / 2, y + (h - dh) / 2, 0, 0, dw, dh, size[0], size[1], size[0], size[1]);
	}

	private void drawOpen(GuiGraphicsExtractor g) {
		Shot s = shots.get(open);
		int infoH = 44, x = 20, y = 24, w = width - 40, h = height - y - infoH - 8;
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2B2C2E);
		int[] size = loaded.get(full);
		if (size != null) fit(g, full, size, x, y, w, h);
		String name = s.file().getFileName().toString();
		g.text(font, (open + 1) + "/" + shots.size() + "   " + name, 20, 10, 0xFFFFB000, true);
		List<String> lines = new ArrayList<>();
		lines.add(Component.translatable("craftography.gallery.taken", DATE.format(Instant.ofEpochMilli(modified(s.file())))).getString());
		Matcher m = NAME.matcher(name);
		String lens = m.matches() ? m.group(2) + " mm" : "?";
		String aspect = m.matches() ? m.group(3).replace('x', ':').replace('_', ':') : "?";
		String res = fullSize != null ? fullSize[0] + " × " + fullSize[1] : "…";
		long bytes;
		try {
			bytes = Files.size(s.file());
		} catch (IOException e) {
			bytes = 0;
		}
		lines.add(Component.translatable("craftography.gallery.details", lens, aspect, res,
			String.format(Locale.ROOT, "%.1f MB", bytes / 1048576.0)).getString());
		lines.add(Component.translatable("craftography.gallery.open_hint").getString());
		int ly = height - infoH;
		for (String line : lines) {
			g.text(font, line, 20, ly, 0xFFD8D8D2, true);
			ly += 12;
		}
	}

	// ---- input --------------------------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
		if (open >= 0) return true;
		int i = at(event.x(), event.y());
		if (i >= 0) openShot(i);
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		double amount = scrollY != 0 ? scrollY : -scrollX;
		if (open >= 0) openShot(Math.clamp(open - (int) Math.signum(amount), 0, shots.size() - 1));
		else showPage(page - (int) Math.signum(amount));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == GLFW.GLFW_KEY_ESCAPE && open >= 0) {
			minecraft.getTextureManager().release(full);
			loaded.remove(full);
			open = -1;
			return true;
		}
		if (key == GLFW.GLFW_KEY_O) {
			Util.getPlatform().openPath(dir);
			return true;
		}
		if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_LEFT) {
			int d = key == GLFW.GLFW_KEY_RIGHT ? 1 : -1;
			if (open >= 0) openShot(Math.clamp(open + d, 0, shots.size() - 1));
			else showPage(page + d);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		releaseAll();
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
