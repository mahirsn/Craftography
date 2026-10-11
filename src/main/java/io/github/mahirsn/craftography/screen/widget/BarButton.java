package io.github.mahirsn.craftography.screen.widget;

import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

/**
 * One item of the camera bar. A button runs its action on click and on scroll. A plate (zoom, time) changes its
 * value when dragged sideways or scrolled, and double-click resets it. The HUD draws the bar; this draws only the
 * highlighted look on hover.
 */
public class BarButton extends Control {
	/** GUI units of horizontal drag per scroll step on a plate. */
	private static final double DRAG_STEP = 6;

	public interface Painter {
		void draw(GuiGraphicsExtractor g);
	}

	private final Painter highlighted;
	private final Supplier<List<Component>> tooltip;
	private final Runnable click, reset;
	private final DoubleConsumer step;
	private double lastX;

	/** A button: {@code click} runs on click, double-click and scroll. */
	public static BarButton button(Rect area, Painter highlighted, Supplier<List<Component>> tooltip, Runnable click) {
		return new BarButton(area, highlighted, tooltip, click, null, click);
	}

	/** A plate: {@code step} gets scroll steps (positive = up/right), {@code reset} runs on double-click. */
	public static BarButton plate(Rect area, Painter highlighted, Supplier<List<Component>> tooltip, DoubleConsumer step, Runnable reset) {
		return new BarButton(area, highlighted, tooltip, null, step, reset);
	}

	private BarButton(Rect area, Painter highlighted, Supplier<List<Component>> tooltip, Runnable click, DoubleConsumer step, Runnable reset) {
		super(area);
		this.highlighted = highlighted;
		this.tooltip = tooltip;
		this.click = click;
		this.step = step;
		this.reset = reset;
	}

	public List<Component> tooltip() {
		return tooltip.get();
	}

	@Override
	public void press(double x, double y) {
		lastX = x;
		if (click != null) click.run();
	}

	@Override
	public void drag(double x, double y) {
		if (step == null) return;
		double steps = (x - lastX) / DRAG_STEP;
		if (Math.abs(steps) < 1) return;
		steps = (int) steps;
		lastX += steps * DRAG_STEP;
		step.accept(steps);
	}

	@Override
	public void scroll(double amount, boolean fine) {
		if (step != null) {
			step.accept(amount);
		} else {
			click.run();
		}
	}

	@Override
	public void reset() {
		reset.run();
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean hovered) {
		if (hovered) highlighted.draw(g);
	}
}
