package io.github.mahirsn.craftography.screen.widget;

import io.github.mahirsn.craftography.hud.DialBank;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A top dial in a slot of the {@link DialBank}; it works on whatever dial the current page puts there. Drag sideways
 * or scroll to turn it. A click without a drag steps a choice or a switch once; double-click resets a value. The HUD
 * draws the dial; this draws only its highlighted look on hover.
 */
public class DialControl extends Control {
	/** GUI units of horizontal drag per step, as on the bar's lens and time plates. */
	private static final double DRAG_STEP = 6;

	private final int slot;
	private double lastX;
	private boolean dragged;

	public DialControl(Rect area, int slot) {
		super(area);
		this.slot = slot;
	}

	public DialBank.Dial dial() {
		return DialBank.get(slot);
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
		dial().step().accept(steps);
	}

	@Override
	public void release() {
		if (!dragged && dial().clickSteps()) dial().step().accept(1);
	}

	@Override
	public void scroll(double amount, boolean fine) {
		dial().step().accept(amount);
	}

	@Override
	public void reset() {
		if (dial().reset() != null) {
			dial().reset().run();
		} else {
			dial().step().accept(1);
		}
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean hovered) {
		if (hovered) DialBank.draw(g, slot, area, true);
	}
}
