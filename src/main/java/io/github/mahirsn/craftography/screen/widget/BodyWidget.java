package io.github.mahirsn.craftography.screen.widget;

import net.minecraft.client.Minecraft;
import io.github.mahirsn.craftography.hud.BodyLayout;
import io.github.mahirsn.craftography.hud.HardwareMotion;

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

	/** Hit targets follow the projected hardware even during the opening animation. */
	@Override
	public Rect area() {
		var window = Minecraft.getInstance().getWindow();
		return BodyLayout.of(window.getGuiScaledWidth(), window.getGuiScaledHeight()).get(control.part());
	}

	@Override
	public boolean contains(double x, double y) {
		Rect r = area();
		return x >= r.x() && x < r.right() && y >= r.y() && y < r.bottom();
	}

	public BodyControls.Control control() {
		return control;
	}

	@Override
	public void press(double x, double y) {
		lastX = x; lastY = y;
		HardwareMotion.press(control.part());
		if (control.part() == BodyLayout.Part.SHUTTER) return;
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
		if (control.part() == BodyLayout.Part.SHUTTER) control.click().run();
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean hovered) {
	}
}
