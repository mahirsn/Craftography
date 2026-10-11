package io.github.mahirsn.craftography.screen;

import io.github.mahirsn.craftography.CameraSettings;
import io.github.mahirsn.craftography.CameraSettings.Aspect;
import io.github.mahirsn.craftography.CameraSettings.Guide;
import io.github.mahirsn.craftography.CameraSettings.Weather;
import io.github.mahirsn.craftography.Lean;
import io.github.mahirsn.craftography.util.ExposureMath;
import io.github.mahirsn.craftography.util.TimeMath;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Craftography's settings, one tab per kind: the held camera, the image (exposure, colour, focus), the viewfinder and
 * the scene. Every option is also on the camera itself; this screen is for setting things up in one place. Opens from
 * Mod Menu, the settings key, or right-clicking the dial page button in interactive mode.
 */
public class ConfigScreen extends Screen {
	private static final int WIDTH = 170;

	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private final TabManager tabs = new TabManager(this::addRenderableWidget, this::removeWidget);
	private MenuTabBar bar;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("craftography.config"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		bar = MenuTabBar.builder(tabs, width).addTabs(new CameraTab(), new ImageTab(), new ViewfinderTab(), new SceneTab()).build();
		addRenderableWidget(bar);
		layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(200).build());
		layout.visitWidgets(this::addRenderableWidget);
		bar.selectTab(0, false);
		repositionElements();
	}

	@Override
	public void repositionElements() {
		if (bar == null) return;
		bar.arrangeElements(width);
		int top = bar.getRectangle().bottom();
		tabs.setTabArea(new ScreenRectangle(0, top, width, height - layout.getFooterHeight() - top));
		layout.setHeaderHeight(top);
		layout.arrangeElements();
	}

	@Override
	public void onClose() {
		CameraSettings.save();
		minecraft.gui.setScreen(parent);
	}

	private static CameraSettings s() {
		return CameraSettings.get();
	}

	/** A tab with a two-column grid of options. */
	private abstract static class OptionsTab extends GridLayoutTab {
		private final GridLayout.RowHelper rows;

		OptionsTab(String key) {
			super(Component.translatable("craftography.config.tab." + key));
			layout.columnSpacing(10).rowSpacing(6);
			rows = layout.createRowHelper(2);
		}

		void add(AbstractWidget widget, String tooltipKey) {
			if (tooltipKey != null) widget.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
			rows.addChild(widget);
		}
	}

	private static CycleButton<Boolean> toggle(String key, Supplier<Boolean> get, Consumer<Boolean> set) {
		CycleButton<Boolean> button = CycleButton.booleanBuilder(CommonComponents.OPTION_ON, CommonComponents.OPTION_OFF, get.get())
			.create(Component.translatable(key), (b, v) -> set.accept(v));
		button.setWidth(WIDTH);
		return button;
	}

	@SafeVarargs
	private static <T> CycleButton<T> choice(String key, Function<T, Component> name, Supplier<T> get, Consumer<T> set, T... values) {
		CycleButton<T> button = new CycleButton.Builder<>(name, get).withValues(values).create(Component.translatable(key), (b, v) -> set.accept(v));
		button.setWidth(WIDTH);
		return button;
	}

	/** A slider over a fixed list of positions, showing the setting's own text for the chosen one. */
	private static class StepSlider extends AbstractSliderButton {
		private final Component name;
		private final int count;
		private final IntConsumer set;
		private final IntFunction<String> text;

		StepSlider(String key, int count, IntSupplier get, IntConsumer set, IntFunction<String> text) {
			super(0, 0, WIDTH, 20, Component.empty(), get.getAsInt() / (double) (count - 1));
			this.name = Component.translatable(key);
			this.count = count;
			this.set = set;
			this.text = text;
			updateMessage();
		}

		private int index() {
			return (int) Math.round(value * (count - 1));
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.empty().append(name).append(": ").append(text.apply(index())));
		}

		@Override
		protected void applyValue() {
			set.accept(index());
		}
	}

	private static StepSlider list(String key, double[] values, Supplier<Double> get, Consumer<Double> set, Function<Double, String> text) {
		return new StepSlider(key, values.length, () -> ExposureMath.nearest(values, get.get()), i -> set.accept(values[i]), i -> text.apply(values[i]));
	}

	private static StepSlider percent(String key, Supplier<Float> get, Consumer<Float> set) {
		return new StepSlider(key, 21, () -> Math.round(get.get() * 20), i -> set.accept(i / 20f), i -> i * 5 + "%");
	}

	private static class CameraTab extends OptionsTab {
		CameraTab() {
			super("camera");
			add(toggle("craftography.config.held_camera", () -> s().heldCamera, v -> s().heldCamera = v), "craftography.config.held_camera.tip");
			add(choice("craftography.config.lean_mode", m -> Component.translatable(m.translationKey()), () -> s().leanMode, v -> s().leanMode = v, Lean.Mode.values()),
				"craftography.config.lean_mode.tip");
			add(choice("craftography.button.timer", t -> t == 0 ? CommonComponents.OPTION_OFF : Component.translatable("craftography.button.timer.seconds", t),
				() -> s().selfTimer, v -> s().selfTimer = v, 0, 3, 10), null);
			add(toggle("craftography.config.preview", () -> s().showPreview, v -> s().showPreview = v), "craftography.dial.preview.name");
		}
	}

	private static class ImageTab extends OptionsTab {
		ImageTab() {
			super("image");
			double[] iso = java.util.Arrays.stream(ExposureMath.ISO).asDoubleStream().toArray();
			add(list("craftography.dial.iso.name", iso, () -> (double) s().iso, v -> s().iso = (int) Math.round(v), v -> String.valueOf(Math.round(v))), null);
			add(list("craftography.dial.shutter.name", ExposureMath.SHUTTER, () -> s().shutter, v -> s().shutter = v, ExposureMath::formatShutter), null);
			add(list("craftography.dial.aperture.name", ExposureMath.APERTURE, () -> s().aperture, v -> s().aperture = v, ExposureMath::formatAperture), null);
			int kelvinSteps = (ExposureMath.MAX_KELVIN - ExposureMath.MIN_KELVIN) / 100 + 1;
			add(new StepSlider("craftography.dial.white_balance.name", kelvinSteps, () -> (s().kelvin - ExposureMath.MIN_KELVIN) / 100,
				i -> s().kelvin = ExposureMath.MIN_KELVIN + i * 100, i -> ExposureMath.MIN_KELVIN + i * 100 + "K"), null);
			add(new StepSlider("craftography.dial.tint.name", 2 * ExposureMath.MAX_TINT + 1, () -> s().tint + ExposureMath.MAX_TINT,
				i -> s().tint = i - ExposureMath.MAX_TINT, i -> {
					int t = i - ExposureMath.MAX_TINT;
					return (t > 0 ? "M" : t < 0 ? "G" : "") + Math.abs(t);
				}), null);
			add(toggle("craftography.dial.grain.name", () -> s().noise, v -> s().noise = v), null);
			add(toggle("craftography.dial.autofocus.name", () -> s().autofocus, v -> s().autofocus = v), null);
			add(toggle("craftography.dial.peaking.name", () -> s().peaking, v -> s().peaking = v), null);
		}
	}

	private static class ViewfinderTab extends OptionsTab {
		ViewfinderTab() {
			super("viewfinder");
			add(choice("craftography.panel.aspect", a -> a == Aspect.FREE ? Component.translatable("craftography.aspect.free") : Component.literal(a.label),
				() -> s().aspect, v -> s().aspect = v, Aspect.values()), null);
			add(choice("craftography.panel.guide", g -> Component.translatable(g.translationKey()), () -> s().guide, v -> s().guide = v, Guide.values()), null);
			add(percent("craftography.dial.mask.name", () -> s().maskOpacity, v -> s().maskOpacity = v), null);
			add(percent("craftography.dial.lines.name", () -> s().guideOpacity, v -> s().guideOpacity = v), null);
			add(toggle("craftography.dial.histogram.name", () -> s().histogram, v -> s().histogram = v), null);
		}
	}

	private static class SceneTab extends OptionsTab {
		SceneTab() {
			super("scene");
			add(choice("craftography.panel.weather", w -> Component.translatable(w.translationKey()), () -> s().weather, v -> s().weather = v, Weather.values()), null);
			Integer[] phases = new Integer[TimeMath.MOON_PHASES + 1];
			for (int i = 0; i < phases.length; i++) phases[i] = i - 1;
			add(choice("craftography.dial.moon.name", p -> Component.translatable(p < 0 ? "craftography.moon.server" : "craftography.moon." + p),
				() -> s().moonPhase, v -> s().moonPhase = v, phases), null);
			add(toggle("craftography.config.keep", () -> s().lockTimeWeather, v -> s().lockTimeWeather = v), "craftography.dial.keep.name");
		}
	}
}
