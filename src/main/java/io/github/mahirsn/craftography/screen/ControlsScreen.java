package io.github.mahirsn.craftography.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.CameraSettings.Aspect;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.Tripod;
import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.hud.CameraBar;
import io.github.mahirsn.craftography.hud.DialBank;
import io.github.mahirsn.craftography.hud.TimeBar;
import io.github.mahirsn.craftography.hud.ViewfinderHud;
import io.github.mahirsn.craftography.screen.widget.BarButton;
import io.github.mahirsn.craftography.screen.widget.Control;
import io.github.mahirsn.craftography.screen.widget.DialControl;
import io.github.mahirsn.craftography.screen.widget.Slider;
import io.github.mahirsn.craftography.util.LensMath;
import io.github.mahirsn.craftography.util.Rect;
import io.github.mahirsn.craftography.util.TimeMath;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Camera controls with a free cursor, on top of the viewfinder HUD. The camera bar, its top dials and the time bar of
 * the HUD become draggable and clickable; nothing is drawn over the picture. Two ways to open it:
 * <ul>
 * <li>interactive mode (toggle key): stays open until the toggle key or Esc;</li>
 * <li>held panel (panel key): open while the key is held.</li>
 * </ul>
 * While it is open the player does not move or look around, because vanilla sends no game input to a screen.
 */
public class ControlsScreen extends Screen {
	private final boolean held;
	private final List<Control> controls = new ArrayList<>();
	private Control dragging, lastPressed;
	private double pressX;

	public ControlsScreen(boolean held) {
		super(Component.translatable(held ? "craftography.panel" : "craftography.interactive"));
		this.held = held;
	}

	@Override
	protected void init() {
		CameraSettings s = CameraSettings.get();
		controls.clear();
		addBar(s);
		for (int i = 0; i < DialBank.SLOTS; i++) controls.add(new DialControl(CameraBar.dial(i, width, height), i));

		// The time bar, when the screen is wide enough to show it.
		Rect bar = TimeBar.bar(width, height);
		if (bar != null) {
			controls.add(new Slider(new Rect(bar.x(), bar.y() - 6, bar.w(), 12), 0, TimeMath.DAY,
				() -> ViewfinderHud.visibleDayTicks(s), v -> s.timeOfDay = TimeMath.dayTicks(Math.round(v / 10) * 10), v -> v,
				(v, amount, fine) -> {
					s.timeOfDay = TimeMath.dayTicks(Math.round(v) + Math.round(amount * (fine ? 10 : 100)));
					return s.timeOfDay;
				}, () -> s.timeOfDay = -1));
		}
	}

	private void addBar(CameraSettings s) {
		for (CameraBar.Item item : CameraBar.Item.values()) {
			Rect area = CameraBar.area(item, width, height);
			BarButton.Painter paint = g -> CameraBar.drawItem(g, item, s, CameraMode.focalLength(), ViewfinderHud.visibleDayTicks(s), true);
			controls.add(switch (item) {
				case ZOOM -> BarButton.plate(area, paint, () -> tooltip("craftography.button.zoom", null, Component.translatable("craftography.button.zoom.hint")),
					steps -> CraftographyClient.change(c -> c.focalLength = LensMath.zoom(c.focalLength, CraftographyClient.shiftDown() ? steps / 4 : steps)),
					() -> CraftographyClient.change(c -> c.focalLength = LensMath.DEFAULT_MM));
				case TIME -> BarButton.plate(area, paint, () -> tooltip("craftography.button.time", null, Component.translatable("craftography.button.time.hint")),
					steps -> CraftographyClient.change(c -> c.timeOfDay = TimeMath.dayTicks(ViewfinderHud.visibleDayTicks(c) + Math.round(steps * (CraftographyClient.shiftDown() ? 10 : 100)))),
					() -> CraftographyClient.change(c -> c.timeOfDay = -1));
				case ROLL -> BarButton.plate(area, paint, () -> tooltip("craftography.button.roll", CraftographyClient.resetRollKey,
						Component.literal(String.format(Locale.ROOT, "%+.1f°", s.roll)), Component.translatable("craftography.button.roll.hint")),
					steps -> CraftographyClient.change(c -> c.addRoll(steps * (CraftographyClient.shiftDown() ? 0.1 : 1))),
					() -> CraftographyClient.change(c -> c.roll = 0));
				case GUIDE -> BarButton.button(area, paint, () -> tooltip("craftography.panel.guide", CraftographyClient.guideKey,
					Component.translatable(s.guide.translationKey())), () -> CraftographyClient.change(CameraSettings::cycleGuide));
				case ASPECT -> BarButton.button(area, paint, () -> tooltip("craftography.panel.aspect", CraftographyClient.aspectKey,
					s.aspect == Aspect.FREE ? Component.translatable("craftography.aspect.free") : Component.literal(s.aspect.label)),
					() -> CraftographyClient.change(CameraSettings::cycleAspect));
				case WEATHER -> BarButton.button(area, paint, () -> tooltip("craftography.panel.weather", CraftographyClient.weatherKey,
					Component.translatable(s.weather.translationKey())), () -> CraftographyClient.change(CameraSettings::cycleWeather));
				case SERVER_TIME -> BarButton.button(area, paint, () -> tooltip("key.craftography.server_time", CraftographyClient.serverTimeKey),
					() -> CraftographyClient.change(c -> c.timeOfDay = -1));
				case TIMER -> BarButton.button(area, paint, () -> tooltip("craftography.button.timer", CraftographyClient.timerKey,
					s.selfTimer == 0 ? Component.translatable("options.off") : Component.translatable("craftography.button.timer.seconds", s.selfTimer)),
					() -> CraftographyClient.change(CameraSettings::cycleTimer));
				case TRIPOD -> BarButton.button(area, paint, () -> tooltip("key.craftography.tripod", CraftographyClient.tripodKey,
					Component.translatable(Tripod.isActive() ? "options.on" : "options.off")), Tripod::toggle);
				case MENU -> BarButton.button(area, paint, () -> tooltip("craftography.button.menu", null,
						Component.translatable("craftography.button.menu.page", DialBank.page().title(), DialBank.page().ordinal() + 1, DialBank.Page.values().length),
						Component.translatable("craftography.button.menu.hint")),
					DialBank::nextPage);
				case GALLERY -> BarButton.button(area, paint, () -> tooltip("craftography.gallery", CraftographyClient.galleryKey),
					() -> minecraft.gui.setScreen(new GalleryScreen(this)));
				case SHUTTER -> BarButton.button(area, paint, () -> Shutter.timerRunning()
					? tooltip("key.craftography.shutter", CraftographyClient.shutterKey, Component.translatable("craftography.button.shutter.cancel"))
					: tooltip("key.craftography.shutter", CraftographyClient.shutterKey), Shutter::press);
			});
		}
	}

	/** Tooltip lines: the name with its key, if bound, then the current value and hints in gray. */
	private static List<Component> tooltip(String name, KeyMapping key, Component... values) {
		MutableComponent title = Component.translatable(name);
		if (key != null && !key.isUnbound()) title.append(Component.literal(" [").append(key.getTranslatedKeyMessage()).append("]").withStyle(ChatFormatting.GRAY));
		List<Component> lines = new ArrayList<>();
		lines.add(title);
		for (Component value : values) lines.add(value.copy().withStyle(ChatFormatting.GRAY));
		return lines;
	}

	private Control at(double x, double y) {
		for (Control c : controls) {
			if (c.contains(x, y)) return c;
		}
		return null;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		Control hovered = dragging != null ? dragging : at(mouseX, mouseY);
		for (Control c : controls) c.draw(g, c == hovered);
		if (dragging != null) return;
		if (hovered instanceof DialControl control) {
			DialBank.Dial dial = control.dial();
			g.setComponentTooltipForNextFrame(font, List.of(dial.name(), dial.words().get().copy().withStyle(ChatFormatting.GRAY),
				Component.translatable(dial.reset() != null ? "craftography.dial.hint.value" : "craftography.dial.hint.choice")
					.withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
		}
		if (hovered instanceof BarButton button) g.setComponentTooltipForNextFrame(font, button.tooltip(), mouseX, mouseY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		// No background: the picture stays visible while adjusting.
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
			CraftographyClient.onMiddleClick();
			return true;
		}
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
		Control c = at(event.x(), event.y());
		if (c == null) return false;
		// A double click counts only on the control that got the first click.
		if (doubleClick && c == lastPressed) {
			c.reset();
			lastPressed = null;
		} else {
			c.press(event.x(), event.y());
			dragging = c;
			lastPressed = c;
			pressX = event.x();
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) return false;
		// A press that turned something is a drag: the next quick press starts a new drag instead of a double click.
		if (Math.abs(event.x() - pressX) >= 6) lastPressed = null;
		dragging.drag(event.x(), event.y());
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) dragging.release();
		dragging = null;
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		double amount = scrollY != 0 ? scrollY : -scrollX;
		if (amount == 0) return false;
		Control c = at(x, y);
		if (c != null) {
			c.scroll(amount, CraftographyClient.shiftDown());
		} else {
			CraftographyClient.onScroll(amount);
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (CraftographyClient.toggleKey.matches(event)) {
			CameraMode.set(false);
			onClose();
			return true;
		}
		if (!held && CraftographyClient.interactiveKey.matches(event)) {
			onClose();
			return true;
		}
		// Camera keys still work here: vanilla sends no key clicks while a screen is open.
		return CraftographyClient.runCameraKey(event) || super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (held && CraftographyClient.panelKey.matches(event)) {
			onClose();
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public void tick() {
		if (!CameraMode.isOpen() || held && !panelKeyHeld()) onClose();
	}

	private boolean panelKeyHeld() {
		InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(CraftographyClient.panelKey);
		if (key.getType() == InputConstants.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
		}
		return key.getType() != InputConstants.Type.KEYSYM || CraftographyClient.isDown(key.getValue());
	}

	@Override
	public void removed() {
		CameraSettings.save();
	}
}
