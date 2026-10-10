package io.github.mahirsn.craftography.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.hud.BodyControls;
import io.github.mahirsn.craftography.hud.BodyLayout;
import io.github.mahirsn.craftography.hud.BodyLayout.Part;
import io.github.mahirsn.craftography.hud.LevelGauge;
import io.github.mahirsn.craftography.hud.TimeBar;
import io.github.mahirsn.craftography.hud.ViewfinderHud;
import io.github.mahirsn.craftography.screen.widget.BodyWidget;
import io.github.mahirsn.craftography.screen.widget.Control;
import io.github.mahirsn.craftography.screen.widget.Dial;
import io.github.mahirsn.craftography.screen.widget.Slider;
import io.github.mahirsn.craftography.util.Rect;
import io.github.mahirsn.craftography.util.TimeMath;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Hands on the camera: the cursor is free and every control on the camera body can be turned, pressed or flipped
 * with the mouse. In the live view the time bar and the roll dial can be dragged. Two ways to open it:
 * interactive mode (toggle key, stays open until the key or Esc) and the held panel (open while its key is held).
 * The player does not move or look around meanwhile, because vanilla sends no game input to a screen.
 */
public class ControlsScreen extends Screen {
	private static Part hovered;

	private final boolean held;
	private final List<Control> controls = new ArrayList<>();
	private Control dragging, lastPressed;

	public ControlsScreen(boolean held) {
		super(Component.translatable(held ? "craftography.panel" : "craftography.interactive"));
		this.held = held;
	}

	public static boolean isOpen() {
		return Minecraft.getInstance().gui.screen() instanceof ControlsScreen;
	}

	/** The body control under the cursor, for the HUD's highlight; null when none or no screen is open. */
	public static Part hoveredPart() {
		return isOpen() ? hovered : null;
	}

	@Override
	protected void init() {
		controls.clear();
		BodyLayout l = BodyLayout.of(width, height);
		for (BodyControls.Control c : BodyControls.all()) controls.add(new BodyWidget(l.get(c.part()), c));
		CameraSettings s = CameraSettings.get();
		Rect lcd = l.lcd;
		controls.add(new Dial(LevelGauge.centerX(lcd), LevelGauge.centerY(lcd), 12, () -> s.roll, v -> CraftographyClient.change(c -> c.roll = v),
			(v, amount, fine) -> {
				s.addRoll(amount * (fine ? 0.1 : 1));
				CameraSettings.save();
				return s.roll;
			}));
		if (BodyControls.page() == BodyControls.Page.WORLD) {
			Rect bar = TimeBar.bar(lcd);
			controls.add(new Slider(new Rect(bar.x() - 3, bar.y() - 6, bar.w() + 6, 12), false, false, 0, TimeMath.DAY,
				() -> ViewfinderHud.visibleDayTicks(s), v -> CraftographyClient.change(c -> c.timeOfDay = TimeMath.dayTicks(Math.round(v / 10) * 10)), v -> v,
				(v, amount, fine) -> {
					CraftographyClient.change(c -> c.timeOfDay = TimeMath.dayTicks(Math.round(v) + Math.round(amount * (fine ? 10 : 100))));
					return s.timeOfDay;
				}, () -> CraftographyClient.change(c -> c.timeOfDay = -1), null));
		}
	}

	private static boolean inside(Rect r, double x, double y) {
		return x >= r.x() && x < r.right() && y >= r.y() && y < r.bottom();
	}

	private Control at(double x, double y) {
		for (Control c : controls) {
			if (c.contains(x, y)) return c;
		}
		return null;
	}

	/** Tooltip: the name with its key, the current value, and how to use the control. */
	private static List<Component> tooltip(BodyControls.Control c) {
		MutableComponent title = Component.translatable(c.name());
		KeyMapping key = c.key() == null ? null : c.key().get();
		if (key != null && !key.isUnbound()) title.append(Component.literal(" [").append(key.getTranslatedKeyMessage()).append("]").withStyle(ChatFormatting.GRAY));
		List<Component> lines = new ArrayList<>();
		lines.add(title);
		Component value = c.value() == null ? null : c.value().get();
		if (value != null) lines.add(value.copy().withStyle(ChatFormatting.YELLOW));
		String hint = switch (c.kind()) {
			case RING, DIAL, WHEEL -> "craftography.hint.turn";
			case BUTTON -> "craftography.hint.press";
			case SWITCH -> "craftography.hint.switch";
			default -> null;
		};
		if (hint != null) lines.add(Component.translatable(hint).withStyle(ChatFormatting.GRAY));
		return lines;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		Control over = dragging != null ? dragging : at(mouseX, mouseY);
		hovered = over instanceof BodyWidget w ? w.control().part() : null;
		for (Control c : controls) c.draw(g, c == over);
		if (dragging == null && over instanceof BodyWidget w) g.setComponentTooltipForNextFrame(font, tooltip(w.control()), mouseX, mouseY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		// No background: the camera and the live view stay visible.
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
		BodyLayout layout = BodyLayout.of(width, height);
		for (BodyControls.Page page : BodyControls.Page.values()) {
			if (inside(layout.pageButton(page.ordinal()), event.x(), event.y())) {
				BodyControls.setPage(page);
				dragging = lastPressed = null;
				init();
				return true;
			}
		}
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
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) return false;
		dragging.drag(event.x(), event.y());
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
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
		hovered = null;
		CameraSettings.save();
	}
}
