package io.github.mahirsn.craftography.screen.widget;

import io.github.mahirsn.craftography.hud.BodyControls;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Mouse handling for one control on the camera body. It keeps no value of its own: every action goes through the
 * control's functions, which change {@code CameraSettings}. The HUD draws the control, highlighted on hover.
 */
public class BodyWidget extends Control {
	/** GUI units of sideways drag per step on a ring, dial or wheel. */
	private static final double DRAG_STEP = 6;

	private final BodyControls.Control control;
	private double lastX;

	public BodyWidget(Rect area, BodyControls.Control control) {
		super(area);
		this.control = control;
	}

	public BodyControls.Control control() {
		return control;
	}

	@Override
	public void press(double x, double y) {
		lastX = x;
		if (control.click() != null) control.click().run();
		if (control.part() == io.github.mahirsn.craftography.hud.BodyLayout.Part.ZOOM)
			control.step().accept(x < area.x() + area.w() / 2 ? -1 : 1);
	}

	@Override
	public void drag(double x, double y) {
		if (control.step() == null) return;
		int steps = (int) ((x - lastX) / DRAG_STEP);
		if (steps == 0) return;
		lastX += steps * DRAG_STEP;
		control.step().accept(steps);
	}

	@Override
	public void scroll(double amount, boolean fine) {
		if (control.step() != null) {
			control.step().accept(amount);
		} else if (control.click() != null) {
			control.click().run();
		}
	}

	@Override
	public void reset() {
		if (control.reset() != null) control.reset().run();
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean hovered) {
	}
}
