package io.github.mahirsn.craftography.screen.widget;

import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A panel control: drag or click to set, scroll to step, double-click to reset. */
public abstract class Control {
	protected final Rect area;

	protected Control(Rect area) {
		this.area = area;
	}

	public Rect area() {
		return area;
	}

	public boolean contains(double x, double y) {
		return x >= area.x() && x < area.right() && y >= area.y() && y < area.bottom();
	}

	public abstract void draw(GuiGraphicsExtractor g, boolean hovered);

	public void press(double x, double y) {
		drag(x, y);
	}

	public void drag(double x, double y) {
	}

	/** Mouse button released after {@link #press}. */
	public void release() {
	}

	public abstract void scroll(double amount, boolean fine);

	public abstract void reset();
}
