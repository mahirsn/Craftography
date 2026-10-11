package io.github.mahirsn.craftography.screen.widget;

import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

/**
 * Linear or logarithmic slider. With {@code label == null} it draws nothing but a hover outline: it is then a hit
 * area over a HUD element that already shows the value (the zoom ruler and the time bar).
 */
public class Slider extends Control {
	public interface Step {
		double apply(double value, double amount, boolean fine);
	}

	private final boolean vertical, log;
	private final double min, max;
	private final DoubleSupplier get;
	private final DoubleConsumer set;
	private final DoubleUnaryOperator snap;
	private final Step step;
	private final Runnable reset;
	private final Component label;
	/** Shown at the right of the label instead of a percentage, e.g. "ISO 400" or "1/125". */
	private java.util.function.Supplier<String> valueText;

	public Slider withValue(java.util.function.Supplier<String> text) {
		valueText = text;
		return this;
	}

	public Slider(Rect area, boolean vertical, boolean log, double min, double max, DoubleSupplier get, DoubleConsumer set,
				  DoubleUnaryOperator snap, Step step, Runnable reset, Component label) {
		super(area);
		this.vertical = vertical;
		this.log = log;
		this.min = min;
		this.max = max;
		this.get = get;
		this.set = set;
		this.snap = snap;
		this.step = step;
		this.reset = reset;
		this.label = label;
	}

	private double toValue(double t) {
		t = Math.max(0, Math.min(1, t));
		return log ? Math.exp(Math.log(min) + t * (Math.log(max) - Math.log(min))) : min + t * (max - min);
	}

	private double toT(double v) {
		double t = log ? (Math.log(v) - Math.log(min)) / (Math.log(max) - Math.log(min)) : (v - min) / (max - min);
		return Math.max(0, Math.min(1, t));
	}

	@Override
	public void drag(double x, double y) {
		double t = vertical ? 1 - (y - area.y()) / area.h() : (x - area.x()) / area.w();
		set.accept(snap.applyAsDouble(toValue(t)));
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
		int accent = Draw.argb(1f, 0xFFB000), white = Draw.argb(0.9f, 0xFFFFFF), dim = Draw.argb(0.4f, 0xFFFFFF);
		if (label == null) {
			if (hovered) outline(g, Draw.argb(0.6f, 0xFFB000));
			return;
		}
		Font font = Minecraft.getInstance().font;
		double t = toT(get.getAsDouble());
		Draw.plate(g, area, hovered);
		Draw.engraved(g, label.getString(), area.x(), area.y());
		String value = valueText != null ? valueText.get() : Math.round(t * 100) + "%";
		Draw.engraved(g, value, area.right() - font.width(value), area.y());
		// An engraved groove with a raised knob, like the plates' own relief.
		double cy = area.bottom() - 4, hx = area.x() + 1 + t * (area.w() - 3);
		Draw.line(g, area.x(), cy, area.right(), cy, Draw.argb(1f, Draw.PRINT));
		Draw.line(g, area.x(), cy + 1, area.right(), cy + 1, Draw.argb(1f, Draw.PRINT_LIGHT));
		Draw.rect(g, hx - 2, cy - 3, hx + 2, cy + 4, Draw.argb(1f, Draw.PRINT));
		Draw.rect(g, hx - 1, cy - 2, hx + 1, cy + 3, hovered ? 0xFFE6E0D6 : 0xFFB8B2AA);
	}

	private void outline(GuiGraphicsExtractor g, int color) {
		Draw.line(g, area.x(), area.y(), area.right(), area.y(), color);
		Draw.line(g, area.x(), area.bottom(), area.right(), area.bottom(), color);
		Draw.line(g, area.x(), area.y(), area.x(), area.bottom(), color);
		Draw.line(g, area.right(), area.y(), area.right(), area.bottom(), color);
	}
}
