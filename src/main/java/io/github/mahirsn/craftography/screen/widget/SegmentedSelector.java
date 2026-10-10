package io.github.mahirsn.craftography.screen.widget;

import io.github.mahirsn.craftography.util.Draw;
import io.github.mahirsn.craftography.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Row of options; text segments are as wide as their labels, icon segments share the width evenly. */
public class SegmentedSelector extends Control {
	public interface Icon {
		void draw(GuiGraphicsExtractor g, int index, Rect box, int color);
	}

	private final List<Component> labels;
	private final IntSupplier get;
	private final IntConsumer set;
	private final Icon icon;
	private final Component title;
	private final double[] edges;

	public SegmentedSelector(Rect area, Component title, List<Component> labels, IntSupplier get, IntConsumer set, Icon icon) {
		super(area);
		this.title = title;
		this.labels = labels;
		this.get = get;
		this.set = set;
		this.icon = icon;
		Font font = Minecraft.getInstance().font;
		double[] widths = new double[labels.size()];
		double total = 0;
		for (int i = 0; i < widths.length; i++) {
			widths[i] = icon != null ? 1 : font.width(labels.get(i)) + 6;
			total += widths[i];
		}
		edges = new double[widths.length + 1];
		edges[0] = area.x();
		for (int i = 0; i < widths.length; i++) edges[i + 1] = edges[i] + widths[i] / total * area.w();
	}

	private Rect box(int i) {
		double top = area.y() + 10;
		return new Rect(edges[i], top, edges[i + 1] - edges[i], area.bottom() - top);
	}

	@Override
	public void press(double x, double y) {
		for (int i = 0; i < labels.size(); i++) {
			if (x >= edges[i] && x < edges[i + 1]) set.accept(i);
		}
	}

	/** Label of the segment under the mouse, for a tooltip on icon segments. */
	public Component hoveredLabel(double x) {
		if (icon == null) return null;
		for (int i = 0; i < labels.size(); i++) {
			if (x >= edges[i] && x < edges[i + 1]) return labels.get(i);
		}
		return null;
	}

	@Override
	public void scroll(double amount, boolean fine) {
		int n = labels.size();
		set.accept(Math.floorMod(get.getAsInt() + (amount > 0 ? -1 : 1), n));
	}

	@Override
	public void reset() {
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean hovered) {
		Font font = Minecraft.getInstance().font;
		int white = Draw.argb(0.9f, 0xFFFFFF), amber = Draw.argb(1f, 0xFFB000), dim = Draw.argb(0.35f, 0xFFFFFF);
		g.text(font, title, (int) area.x(), (int) area.y(), hovered ? amber : white, true);
		int selected = get.getAsInt();
		for (int i = 0; i < labels.size(); i++) {
			Rect b = box(i);
			boolean on = i == selected;
			int color = on ? amber : white;
			if (icon != null) {
				icon.draw(g, i, new Rect(b.x() + 1, b.y() + 1, b.w() - 2, b.h() - 3), color);
			} else {
				Component label = labels.get(i);
				g.text(font, label, (int) Math.round(b.x() + (b.w() - font.width(label)) / 2), (int) (b.y() + 2), color, true);
			}
			Draw.line(g, b.x() + 1, b.bottom(), b.right() - 1, b.bottom(), on ? amber : dim);
		}
	}
}
