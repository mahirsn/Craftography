package io.github.mahirsn.craftography.screen.widget;

import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

/**
 * One side-panel setting drawn like a camera bar item: a raised plate with the name engraved on the left and the value
 * (text or icon) on the right, plus the bar's amber lamp when the setting is not at its default. Drag sideways or
 * scroll to step; a click without a drag steps once (cycles a choice, flips a switch); double-click resets.
 */
public class SettingRow extends Control {
	/** GUI units of horizontal drag per step, as on the bar's lens and time plates. */
	private static final double DRAG_STEP = 6;
	private static final int LED = 0xFFB000;

	public interface Icon {
		/** Draws an engraved 11x9 icon with its top-left corner at (x, y). */
		void draw(GuiGraphicsExtractor g, int x, int y);
	}

	private final Component name;
	private final Supplier<String> value;
	private final Icon icon;
	private final DoubleConsumer step;
	private final boolean clickSteps;
	private final Runnable reset;
	private final BooleanSupplier changed;
	private final Supplier<Component> tooltip;
	private double lastX;
	private boolean dragged;

	/**
	 * @param value      value text, or null when {@code icon} shows the value
	 * @param step       steps (positive = next / more), from drag, scroll and click
	 * @param clickSteps whether a plain click steps once; off for continuous values, where a click would jump
	 * @param tooltip    current value in words, under the name in the tooltip
	 */
	public SettingRow(Rect area, Component name, Supplier<String> value, Icon icon, DoubleConsumer step, boolean clickSteps,
					  Runnable reset, BooleanSupplier changed, Supplier<Component> tooltip) {
		super(area);
		this.name = name;
		this.value = value;
		this.icon = icon;
		this.step = step;
		this.clickSteps = clickSteps;
		this.reset = reset;
		this.changed = changed;
		this.tooltip = tooltip;
	}

	public Component name() {
		return name;
	}

	public Component tooltip() {
		return tooltip == null ? null : tooltip.get();
	}

	@Override
	public void press(double x, double y) {
		lastX = x;
		dragged = false;
	}

	@Override
	public void drag(double x, double y) {
		double steps = (x - lastX) / DRAG_STEP;
		if (Math.abs(steps) < 1) return;
		steps = (int) steps;
		lastX += steps * DRAG_STEP;
		dragged = true;
		step.accept(steps);
	}

	@Override
	public void release() {
		if (!dragged && clickSteps) step.accept(1);
	}

	@Override
	public void scroll(double amount, boolean fine) {
		step.accept(amount);
	}

	@Override
	public void reset() {
		reset.run();
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean hovered) {
		Font font = Minecraft.getInstance().font;
		int x = (int) area.x(), y = (int) area.y(), w = (int) area.w(), h = (int) area.h();
		Draw.camera(g, hovered ? "plate_highlighted" : "plate", x, y, w, h, -1);
		if (changed.getAsBoolean()) g.fill(x + 3, y + 2, x + 6, y + 3, Draw.argb(1f, LED));
		// Same baseline as the bar's engraved print: 7 px down on an 18 px plate, under its raised top edge.
		int ty = y + Math.max(2, h / 2 - 2);
		int right = x + w - 5, valueWidth;
		if (icon != null) {
			valueWidth = 11;
			icon.draw(g, right - 11, ty - 2);
		} else {
			String v = value.get();
			valueWidth = font.width(v);
			Draw.engraved(g, v, right - valueWidth, ty);
		}
		String label = name.getString();
		while (!label.isEmpty() && font.width(label) > w - 14 - valueWidth) label = label.substring(0, label.length() - 1);
		Draw.engraved(g, label, x + 5, ty);
	}
}
