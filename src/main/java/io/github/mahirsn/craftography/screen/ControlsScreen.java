package io.github.mahirsn.craftography.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mahirsn.craftography.Autofocus;
import io.github.mahirsn.craftography.CameraMode;
import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.CraftographyClient;
import io.github.mahirsn.craftography.Tripod;
import io.github.mahirsn.craftography.capture.Shutter;
import io.github.mahirsn.craftography.hud.ViewfinderHud;
import io.github.mahirsn.craftography.render.LensEffects;
import io.github.mahirsn.craftography.render.Meter;
import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.ExposureMath;
import io.github.mahirsn.craftography.util.LensMath;
import io.github.mahirsn.craftography.util.Rect;
import io.github.mahirsn.craftography.util.TimeMath;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/** A tabbed settings dock. Every label and slider reads the live camera setting, without a widget value cache. */
public class ControlsScreen extends Screen {
	public enum Tab {
		EXPOSURE("exposure"), COLOUR("colour"), FOCUS("focus"), FRAME("frame"), WORLD("world"), DRIVE("drive");
		final String id;
		Tab(String id) { this.id = id; }
	}

	private record Setting(String label, Supplier<String> value, DoubleSupplier position,
		DoubleConsumer setPosition, DoubleConsumer step, Runnable reset, BooleanSupplier enabled) { }

	private static final int INK = 0xFF2B2622;
	private static final int AMBER = 0xFFFFC85A;
	private final boolean held;
	private final List<Setting> settings = new ArrayList<>();
	private Tab tab = Tab.EXPOSURE;
	private DockLayout layout;
	private int scroll, dragging = -1, focused = -1, lastPressed = -1;

	public ControlsScreen(boolean held) {
		super(Component.translatable(held ? "craftography.panel" : "craftography.interactive"));
		this.held = held;
	}

	public static boolean isOpen() {
		return Minecraft.getInstance().gui.screen() instanceof ControlsScreen;
	}

	@Override
	protected void init() {
		layout = DockLayout.of(width, height);
		populate();
		scroll = Math.clamp(scroll, 0, maxScroll());
		dragging = lastPressed = -1;
	}

	private static String tr(String key) { return Component.translatable(key).getString(); }
	private static String number(double v) { return String.format(Locale.ROOT, "%.1f", v); }
	private static final BooleanSupplier ALWAYS = () -> true;

	private void slider(String label, Supplier<String> value, DoubleSupplier get, DoubleConsumer set,
		double min, double max, double step, double defaultValue, BooleanSupplier enabled) {
		settings.add(new Setting(label, value, () -> (get.getAsDouble() - min) / (max - min),
			p -> set.accept(Math.clamp(min + Math.round(p * (max - min) / step) * step, min, max)),
			n -> set.accept(Math.clamp(get.getAsDouble() + n * step, min, max)),
			() -> set.accept(defaultValue), enabled));
	}

	private void choice(String label, Supplier<String> value, DoubleConsumer step, Runnable reset) {
		settings.add(new Setting(label, value, null, null, step, reset, ALWAYS));
	}

	private void toggle(String label, BooleanSupplier get, Runnable action) {
		choice(label, () -> tr(get.getAsBoolean() ? "craftography.ui.on" : "craftography.ui.off"), n -> action.run(), action);
	}

	private void populate() {
		settings.clear();
		CameraSettings s = CameraSettings.get();
		switch (tab) {
			case EXPOSURE -> {
				slider("craftography.ui.iso", () -> String.valueOf(s.iso), () -> ExposureMath.nearest(ExposureMath.ISO, s.iso),
					v -> CraftographyClient.change(c -> c.iso = ExposureMath.ISO[(int) v]), 0, ExposureMath.ISO.length - 1, 1, 0, ALWAYS);
				slider("craftography.ui.aperture", () -> ExposureMath.formatAperture(s.aperture), () -> ExposureMath.nearest(ExposureMath.APERTURE, s.aperture),
					v -> CraftographyClient.change(c -> c.aperture = ExposureMath.APERTURE[(int) v]), 0, ExposureMath.APERTURE.length - 1, 1,
					ExposureMath.nearest(ExposureMath.APERTURE, ExposureMath.DEFAULT_APERTURE), ALWAYS);
				slider("craftography.ui.shutter", () -> ExposureMath.formatShutter(s.shutter), () -> ExposureMath.nearest(ExposureMath.SHUTTER, s.shutter),
					v -> CraftographyClient.change(c -> c.shutter = ExposureMath.SHUTTER[(int) v]), 0, ExposureMath.SHUTTER.length - 1, 1,
					ExposureMath.nearest(ExposureMath.SHUTTER, ExposureMath.DEFAULT_SHUTTER), ALWAYS);
				toggle("craftography.control.noise", () -> s.noise, () -> CraftographyClient.change(c -> c.noise = !c.noise));
				toggle("craftography.control.histogram", () -> s.histogram, () -> CraftographyClient.change(c -> c.histogram = !c.histogram));
			}
			case COLOUR -> {
				slider("craftography.ui.kelvin", () -> s.kelvin + " K", () -> s.kelvin, v -> CraftographyClient.change(c -> c.kelvin = (int) v),
					ExposureMath.MIN_KELVIN, ExposureMath.MAX_KELVIN, 100, ExposureMath.DEFAULT_KELVIN, ALWAYS);
				slider("craftography.ui.tint", () -> (s.tint > 0 ? "+" : "") + s.tint, () -> s.tint, v -> CraftographyClient.change(c -> c.tint = (int) v),
					-ExposureMath.MAX_TINT, ExposureMath.MAX_TINT, 1, 0, ALWAYS);
			}
			case FOCUS -> {
				slider("craftography.ui.lens", () -> Math.round(s.focalLength) + " mm", () -> s.focalLength,
					v -> CraftographyClient.change(c -> c.focalLength = LensMath.clamp(v)), LensMath.MIN_MM, LensMath.MAX_MM, 1, LensMath.DEFAULT_MM, ALWAYS);
				choice("craftography.control.focus_mode", () -> tr(s.autofocus ? "craftography.focus.auto" : "craftography.focus.manual"),
					n -> CraftographyClient.change(c -> c.autofocus = !c.autofocus), () -> CraftographyClient.change(c -> c.autofocus = true));
				slider("craftography.ui.distance", () -> ExposureMath.formatFocus(s.autofocus ? Autofocus.distance() : s.focusDistance),
					() -> Math.log(s.focusDistance / ExposureMath.MIN_FOCUS),
					v -> CraftographyClient.change(c -> c.focusDistance = Math.min(ExposureMath.FOCUS_INFINITY, ExposureMath.MIN_FOCUS * Math.exp(v))),
					0, Math.log(ExposureMath.FOCUS_INFINITY / ExposureMath.MIN_FOCUS), 0.01,
					Math.log(ExposureMath.FOCUS_INFINITY / ExposureMath.MIN_FOCUS), () -> !s.autofocus && LensEffects.depthOfFieldAvailable());
				toggle("craftography.control.peaking", () -> s.peaking, () -> CraftographyClient.change(c -> c.peaking = !c.peaking));
			}
			case FRAME -> {
				choice("craftography.panel.aspect", () -> s.aspect == CameraSettings.Aspect.FREE ? tr("craftography.aspect.free") : s.aspect.label,
					n -> CraftographyClient.change(c -> c.aspect = CameraSettings.Aspect.values()[Math.floorMod(c.aspect.ordinal() + (int) n, CameraSettings.Aspect.values().length)]),
					() -> CraftographyClient.change(c -> c.aspect = CameraSettings.Aspect.R3_2));
				choice("craftography.panel.guide", () -> tr(s.guide.translationKey()),
					n -> CraftographyClient.change(c -> c.guide = CameraSettings.Guide.values()[Math.floorMod(c.guide.ordinal() + (int) n, CameraSettings.Guide.values().length)]),
					() -> CraftographyClient.change(c -> c.guide = CameraSettings.Guide.THIRDS));
				slider("craftography.ui.roll", () -> number(s.roll) + "°", () -> s.roll,
					v -> CraftographyClient.change(c -> c.roll = v), -90, 90, 0.1, 0, ALWAYS);
				slider("craftography.panel.mask_opacity", () -> Math.round(s.maskOpacity * 100) + "%", () -> s.maskOpacity,
					v -> CraftographyClient.change(c -> c.maskOpacity = (float) v), 0, 1, 0.01, 0.75, ALWAYS);
				slider("craftography.panel.guide_opacity", () -> Math.round(s.guideOpacity * 100) + "%", () -> s.guideOpacity,
					v -> CraftographyClient.change(c -> c.guideOpacity = (float) v), 0, 1, 0.01, 0.45, ALWAYS);
			}
			case WORLD -> {
				slider("craftography.ui.time", () -> TimeMath.format(ViewfinderHud.visibleDayTicks(s)), () -> ViewfinderHud.visibleDayTicks(s),
					v -> CraftographyClient.change(c -> c.timeOfDay = TimeMath.dayTicks(Math.round(v))), 0, TimeMath.DAY - 1, 1, 0, ALWAYS);
				choice("key.craftography.server_time", () -> tr(s.timeOfDay < 0 ? "craftography.ui.on" : "craftography.ui.off"),
					n -> CraftographyClient.change(c -> c.timeOfDay = -1), () -> CraftographyClient.change(c -> c.timeOfDay = -1));
				choice("craftography.panel.weather", () -> tr(s.weather.translationKey()),
					n -> CraftographyClient.change(c -> c.weather = CameraSettings.Weather.values()[Math.floorMod(c.weather.ordinal() + (int) n, CameraSettings.Weather.values().length)]),
					() -> CraftographyClient.change(c -> c.weather = CameraSettings.Weather.SERVER));
				choice("craftography.panel.moon", () -> tr(s.moonPhase < 0 ? "craftography.moon.server" : "craftography.moon." + s.moonPhase),
					n -> CraftographyClient.change(c -> c.moonPhase = Math.floorMod(c.moonPhase + 1 + (int) n, 9) - 1), () -> CraftographyClient.change(c -> c.moonPhase = -1));
				toggle("craftography.panel.lock", () -> s.lockTimeWeather, () -> CraftographyClient.change(c -> c.lockTimeWeather = !c.lockTimeWeather));
			}
			case DRIVE -> {
				choice("craftography.button.timer", () -> s.selfTimer == 0 ? tr("craftography.ui.off") : s.selfTimer + " s",
					n -> CraftographyClient.change(c -> { int[] delays = {0, 3, 10}; int i = c.selfTimer == 0 ? 0 : c.selfTimer == 3 ? 1 : 2; c.selfTimer = delays[Math.floorMod(i + (int) n, 3)]; }), () -> CraftographyClient.change(c -> c.selfTimer = 0));
				toggle("key.craftography.tripod", Tripod::isActive, Tripod::toggle);
				toggle("craftography.panel.preview", () -> s.showPreview, () -> CraftographyClient.change(c -> c.showPreview = !c.showPreview));
			}
		}
	}

	private int maxScroll() { return Math.max(0, settings.size() * DockLayout.ROW_HEIGHT - (int) layout.content().h()); }
	private int rowAt(double x, double y) {
		if (!DockLayout.contains(layout.content(), x, y)) return -1;
		int i = (int) (y - layout.content().y() + scroll) / DockLayout.ROW_HEIGHT;
		return i < settings.size() ? i : -1;
	}
	private void switchTab(Tab next) {
		tab = next;
		scroll = 0;
		dragging = focused = lastPressed = -1;
		populate();
	}
	private void setSlider(int i, double x) {
		Setting s = settings.get(i);
		Rect r = layout.row(i, scroll);
		if (s.enabled.getAsBoolean() && s.setPosition != null)
			s.setPosition.accept(Math.clamp((x - r.x() - 3) / (r.w() - 6), 0, 1));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		Rect p = layout.panel();
		Draw.camera(g, "ui/panel", p.x(), p.y(), (int) p.w(), (int) p.h(), 0xFFFFFFFF);
		g.text(font, tr("craftography.tab." + tab.id), (int) p.x() + 10, (int) p.y() + 9, INK, false);
		int overTab = -1;
		for (Tab t : Tab.values()) {
			Rect r = layout.tab(t.ordinal());
			boolean over = DockLayout.contains(r, mouseX, mouseY);
			if (over) overTab = t.ordinal();
			Draw.camera(g, "ui/tab" + (t == tab ? "_selected" : over ? "_hl" : ""), r.x(), r.y(), (int) r.w(), (int) r.h(), 0xFFFFFFFF);
			Draw.camera(g, "ui/icon_tab_" + t.id, r.x() + (r.w() - 11) / 2, r.y() + 5, 11, 11, t == tab ? AMBER : 0xFFFFFFFF);
			if (t == tab) Draw.rect(g, r.x() + 2, r.bottom() - 2, r.right() - 2, r.bottom(), AMBER);
		}
		Rect content = layout.content();
		g.enableScissor((int) content.x(), (int) content.y(), (int) content.right(), (int) content.bottom());
		int over = rowAt(mouseX, mouseY);
		for (int i = 0; i < settings.size(); i++) {
			Rect r = layout.row(i, scroll);
			if (r.bottom() <= content.y() || r.y() + font.lineHeight > content.bottom()) continue;
			Setting s = settings.get(i);
			boolean enabled = s.enabled.getAsBoolean();
			int color = enabled ? INK : 0xFF666666;
			if (i == focused) Draw.rect(g, r.x() - 2, r.y(), r.right() + 2, r.bottom() - 2, 0x33805B2F);
			g.text(font, font.plainSubstrByWidth(tr(s.label), (int) r.w()), (int) r.x(), (int) r.y(), color, false);
			String value = font.plainSubstrByWidth(s.value.get(), (int) r.w() - (s.position == null ? 28 : 0));
			g.text(font, value, (int) (r.x() + (r.w() - font.width(value)) / 2), (int) r.y() + 12, color, false);
			if (s.position != null) {
				Draw.camera(g, "ui/slot", r.x(), r.y() + 25, (int) r.w(), 4, 0xFFFFFFFF);
				double pos = Math.clamp(s.position.getAsDouble(), 0, 1);
				Draw.camera(g, "ui/knob" + (enabled && (i == over || i == dragging) ? "_hl" : ""), r.x() + pos * (r.w() - 6), r.y() + 21, 6, 12, enabled ? 0xFFFFFFFF : 0xFF999999);
			} else {
				Draw.camera(g, "ui/arrow_prev" + (i == over ? "_hl" : ""), r.x(), r.y() + 11, 9, 11, 0xFFFFFFFF);
				Draw.camera(g, "ui/arrow_next" + (i == over ? "_hl" : ""), r.right() - 9, r.y() + 11, 9, 11, 0xFFFFFFFF);
			}
		}
		g.disableScissor();
		if (maxScroll() > 0) {
			int h = Math.max(10, (int) (content.h() * content.h() / (settings.size() * DockLayout.ROW_HEIGHT)));
			Draw.rect(g, p.right() - 6, content.y(), p.right() - 4, content.bottom(), 0xFF8B8B8B);
			double y = content.y() + (content.h() - h) * scroll / maxScroll();
			Draw.rect(g, p.right() - 6, y, p.right() - 4, y + h, 0xFFFFFFFF);
		}
		Rect button = layout.shutter();
		Draw.camera(g, "ui/slot", button.x(), button.y(), (int) button.w(), (int) button.h(), 0xFFFFFFFF);
		String caption = tr(Shutter.timerRunning() ? "craftography.ui.cancel" : "key.craftography.shutter");
		g.text(font, caption, (int) (button.x() + (button.w() - font.width(caption)) / 2), (int) button.y() + 4, DockLayout.contains(button, mouseX, mouseY) ? AMBER : 0xFFFFFFFF, true);
		if (overTab >= 0) g.setComponentTooltipForNextFrame(font, List.of(Component.translatable("craftography.tab." + Tab.values()[overTab].id)), mouseX, mouseY);
		else if (over >= 0 && dragging < 0) {
			Setting s = settings.get(over);
			List<Component> tip = new ArrayList<>();
			tip.add(Component.translatable(s.label));
			tip.add(Component.literal(s.value.get()));
			if (!s.enabled.getAsBoolean()) tip.add(Component.translatable(LensEffects.depthOfFieldAvailable() ? "craftography.ui.manual_hint" : "craftography.dof.off"));
			else tip.add(Component.translatable(s.position != null ? "craftography.ui.slider_hint" : "craftography.ui.choice_hint"));
			g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
		}
		// Availability and metering are information, not pretend controls.
		if (tab == Tab.FOCUS && !LensEffects.depthOfFieldAvailable())
			g.text(font, tr("craftography.dof.off_short"), (int) p.right() - font.width(tr("craftography.dof.off_short")) - 10, (int) p.y() + 9, 0xFF805B2F, false);
		if (tab == Tab.EXPOSURE && p.w() >= 160) {
			String meter = Double.isNaN(Meter.level()) ? "EV --" : String.format(Locale.ROOT, "EV %+.1f", Meter.level());
			g.text(font, meter, (int) p.right() - font.width(meter) - 10, (int) p.y() + 9, INK, false);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) { }
	@Override
	public boolean isPauseScreen() { return false; }

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) { CraftographyClient.onMiddleClick(); return true; }
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
		for (Tab t : Tab.values()) if (DockLayout.contains(layout.tab(t.ordinal()), event.x(), event.y())) { switchTab(t); return true; }
		if (DockLayout.contains(layout.shutter(), event.x(), event.y())) { Shutter.press(); return true; }
		int i = rowAt(event.x(), event.y());
		if (i < 0) return false;
		Setting s = settings.get(i);
		focused = i;
		if (!s.enabled.getAsBoolean()) return true;
		if (doubleClick && lastPressed == i) { s.reset.run(); lastPressed = -1; }
		else if (s.position != null) { setSlider(i, event.x()); dragging = i; lastPressed = i; }
		else { s.step.accept(event.x() < layout.row(i, scroll).x() + 12 ? -1 : 1); lastPressed = -1; }
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging < 0) return false;
		setSlider(dragging, event.x());
		return true;
	}
	@Override
	public boolean mouseReleased(MouseButtonEvent event) { dragging = -1; return true; }

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		double amount = scrollY != 0 ? scrollY : -scrollX;
		if (amount == 0) return false;
		int i = rowAt(x, y);
		if (i >= 0 && y >= layout.row(i, scroll).y() + 21 && settings.get(i).enabled.getAsBoolean() && settings.get(i).position != null) {
			settings.get(i).step.accept(amount * (tab == Tab.WORLD && i == 0 ? (CraftographyClient.shiftDown() ? 10 : 100) : 1));
		} else if (DockLayout.contains(layout.panel(), x, y)) {
			scroll = Math.clamp(scroll - (int) (amount * DockLayout.ROW_HEIGHT), 0, maxScroll());
		} else CraftographyClient.onScroll(amount);
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
		if (CraftographyClient.runCameraKey(event)) return true;
		if (event.key() == GLFW.GLFW_KEY_TAB) {
			focused = Math.floorMod(focused + (CraftographyClient.shiftDown() ? -1 : 1), settings.size());
			scroll = Math.clamp(focused * DockLayout.ROW_HEIGHT - (int) layout.content().h() / 2, 0, maxScroll());
			return true;
		}
		if (focused >= 0 && (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT || event.key() == GLFW.GLFW_KEY_ENTER)) {
			Setting s = settings.get(focused);
			if (s.enabled.getAsBoolean()) s.step.accept(event.key() == GLFW.GLFW_KEY_LEFT ? -1 : 1);
			return true;
		}
		return super.keyPressed(event);
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
