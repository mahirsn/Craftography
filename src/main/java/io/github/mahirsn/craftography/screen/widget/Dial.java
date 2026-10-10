package io.github.mahirsn.craftography.screen.widget;

import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Half-circle dial for an angle from -90 to +90 degrees; 0 points up. Snaps to 0 within one degree. */
public class Dial extends Control {
	private final double cx, cy, r;
	private final DoubleSupplier get;
	private final DoubleConsumer set;
	private final Slider.Step step;

	public Dial(double cx, double cy, double r, DoubleSupplier get, DoubleConsumer set, Slider.Step step) {
		super(new Rect(cx - r - 4, cy - r - 4, 2 * r + 8, r + 8));
		this.cx = cx;
		this.cy = cy;
		this.r = r;
		this.get = get;
		this.set = set;
		this.step = step;
	}

	@Override
	public void drag(double x, double y) {
		double angle = Math.toDegrees(Math.atan2(x - cx, cy - y));
		angle = Math.max(-90, Math.min(90, angle));
		set.accept(Math.abs(angle) < 1 ? 0 : Math.round(angle * 10) / 10.0);
	}

	@Override
	public void scroll(double amount, boolean fine) {
		set.accept(step.apply(get.getAsDouble(), amount, fine));
	}

	@Override
	public void reset() {
		set.accept(0);
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean hovered) {
		// The sprite is a 47x25 half circle of radius 22 with its center at (23, 23).
		int tint = Draw.argb(hovered ? 0.9f : 0.6f, hovered ? 0xFFB000 : 0xFFFFFF);
		Draw.sprite(g, "dial", cx - 23 * r / 22, cy - 23 * r / 22, (int) Math.round(47 * r / 22), (int) Math.round(25 * r / 22), tint);
		double rad = Math.toRadians(get.getAsDouble());
		Draw.line(g, cx + Math.sin(rad) * (r - 5), cy - Math.cos(rad) * (r - 5), cx + Math.sin(rad) * (r + 3), cy - Math.cos(rad) * (r + 3), Draw.argb(1f, 0xFFB000));
	}
}
