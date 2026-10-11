package io.github.mahirsn.craftography.screen.widget;

import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

/**
 * Horizontal hit area over a HUD element that already shows the value (the time bar): click or drag to set, scroll to
 * step, double-click to reset. It draws only a hover outline.
 */
public class Slider extends Control {
	public interface Step {
		double apply(double value, double amount, boolean fine);
	}

	private final double min, max;
	private final DoubleSupplier get;
	private final DoubleConsumer set;
	private final DoubleUnaryOperator snap;
	private final Step step;
	private final Runnable reset;

	public Slider(Rect area, double min, double max, DoubleSupplier get, DoubleConsumer set, DoubleUnaryOperator snap, Step step, Runnable reset) {
		super(area);
		this.min = min;
		this.max = max;
		this.get = get;
		this.set = set;
		this.snap = snap;
		this.step = step;
		this.reset = reset;
	}

	@Override
	public void drag(double x, double y) {
		double t = Math.max(0, Math.min(1, (x - area.x()) / area.w()));
		set.accept(snap.applyAsDouble(min + t * (max - min)));
	}

	@Override
	public void scroll(double amount, boolean fine) {
		set.accept(step.apply(get.getAsDouble(), amount, fine));
	}

	@Override
	public void reset() {
		reset.run();
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean hovered) {
		if (!hovered) return;
		int color = Draw.argb(0.6f, 0xFFB000);
		Draw.line(g, area.x(), area.y(), area.right(), area.y(), color);
		Draw.line(g, area.x(), area.bottom(), area.right(), area.bottom(), color);
		Draw.line(g, area.x(), area.y(), area.x(), area.bottom(), color);
		Draw.line(g, area.right(), area.y(), area.right(), area.bottom(), color);
	}
}
