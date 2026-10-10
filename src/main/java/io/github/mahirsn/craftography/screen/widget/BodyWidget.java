package io.github.mahirsn.craftography.screen.widget;

import io.github.mahirsn.craftography.hud.BodyControls;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Mouse handling for one control on the camera body. It keeps no value of its own: every action goes through the
 * control's functions, which change {@code CameraSettings}. The HUD draws the control, highlighted on hover.
 */
public class BodyWidget extends Control {
	/** GUI units of vertical or sideways drag per step on a ring, dial or wheel. */
	private static final double DRAG_STEP = 6;

	private final BodyControls.Control control;
	private double lastX, lastY;

	public BodyWidget(Rect area, BodyControls.Control control) {
		super(area);
		this.control = control;
	}

	public BodyControls.Control control() {
		return control;
	}

	@Override
	public void press(double x, double y) {
		lastX = x; lastY = y;
		io.github.mahirsn.craftography.hud.HardwareMotion.press(control.part());
		if (control.part() == io.github.mahirsn.craftography.hud.BodyLayout.Part.SHUTTER) return;
		if (control.click() != null) control.click().run();
	}

	@Override
	public void drag(double x, double y) {
		if (control.step() == null) return;
		double delta = Math.abs(y - lastY) > Math.abs(x - lastX) ? lastY - y : x - lastX;
		int steps = (int) (delta / DRAG_STEP);
		if (steps == 0) return;
		if (Math.abs(y - lastY) > Math.abs(x - lastX)) lastY -= steps * DRAG_STEP;
		else lastX += steps * DRAG_STEP;
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

	public void release() {
		if (control.part() == io.github.mahirsn.craftography.hud.BodyLayout.Part.SHUTTER) control.click().run();
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean hovered) {
	}
}
