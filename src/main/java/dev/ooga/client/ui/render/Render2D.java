package dev.ooga.client.ui.render;

import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Shape primitives for the Ooga UI.
 *
 * <p>Vanilla GUI drawing snaps to GUI pixels (2–4 screen pixels each), which makes rounded
 * corners look blocky. Every primitive here temporarily scales the pose down by the GUI scale
 * and rasterises in physical screen pixels instead, with coverage-based anti-aliasing on
 * curved edges. Inputs are still GUI coordinates, as floats, so animated positions stay smooth.
 *
 * <p>A global opacity stack lets whole screens fade in and out without every component
 * having to thread an alpha value through.
 */
public final class Render2D {
	private static final Deque<Float> ALPHA_STACK = new ArrayDeque<>();
	private static float alpha = 1f;

	private Render2D() {
	}

	// ------------------------------------------------------------------ opacity

	public static void pushAlpha(float factor) {
		ALPHA_STACK.push(alpha);
		alpha *= Math.max(0f, Math.min(1f, factor));
	}

	public static void popAlpha() {
		alpha = ALPHA_STACK.isEmpty() ? 1f : ALPHA_STACK.pop();
	}

	public static float alpha() {
		return alpha;
	}

	/** Applies the current global opacity to a color. */
	public static int apply(int color) {
		return alpha >= 1f ? color : ColorUtil.fade(color, alpha);
	}

	// ------------------------------------------------------------------ pixel space

	public static int guiScale() {
		return Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
	}

	private static int px(float gui, int scale) {
		return Math.round(gui * scale);
	}

	private static void begin(GuiGraphics g, int scale) {
		g.pose().pushMatrix();
		g.pose().scale(1f / scale, 1f / scale);
	}

	private static void end(GuiGraphics g) {
		g.pose().popMatrix();
	}

	// ------------------------------------------------------------------ primitives

	public static void rect(GuiGraphics g, float x, float y, float w, float h, int color) {
		if (w <= 0 || h <= 0) return;
		int c = apply(color);
		if (ColorUtil.alpha(c) == 0) return;
		int s = guiScale();
		begin(g, s);
		g.fill(px(x, s), px(y, s), px(x + w, s), px(y + h, s), c);
		end(g);
	}

	public static void verticalGradient(GuiGraphics g, float x, float y, float w, float h, int top, int bottom) {
		if (w <= 0 || h <= 0) return;
		int s = guiScale();
		begin(g, s);
		g.fillGradient(px(x, s), px(y, s), px(x + w, s), px(y + h, s), apply(top), apply(bottom));
		end(g);
	}

	/**
	 * Left-to-right gradient: {@code left} fading out rightwards laid over {@code right} fading
	 * out leftwards. Pass 0 for either side to get a one-colour fade. Square corners.
	 */
	public static void horizontalGradient(GuiGraphics g, float x, float y, float w, float h, int left, int right) {
		if (w <= 0 || h <= 0) return;
		if (left != 0 && !SoftGlow.fade(g, x, y, w, h, apply(left), true)) {
			rect(g, x, y, w, h, ColorUtil.lerp(left, right, 0.5f));
			return;
		}
		if (right != 0) SoftGlow.fade(g, x, y, w, h, apply(right), false);
	}

	public static void roundRect(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
		if (w <= 0 || h <= 0) return;
		int c = apply(color);
		if (ColorUtil.alpha(c) == 0) return;
		int s = guiScale();
		begin(g, s);
		roundRectPx(g, px(x, s), px(y, s), px(x + w, s), px(y + h, s), radius * s, c);
		end(g);
	}

	/** A 1-screen-pixel border following the same rounded shape as {@link #roundRect}. */
	public static void outline(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
		outline(g, x, y, w, h, radius, 1, color);
	}

	public static void outline(GuiGraphics g, float x, float y, float w, float h, float radius, int thicknessPx, int color) {
		if (w <= 0 || h <= 0) return;
		int c = apply(color);
		if (ColorUtil.alpha(c) == 0) return;
		int s = guiScale();
		begin(g, s);
		ringPx(g, px(x, s), px(y, s), px(x + w, s), px(y + h, s), radius * s, thicknessPx, c);
		end(g);
	}

	public static void circle(GuiGraphics g, float cx, float cy, float radius, int color) {
		roundRect(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
	}

	/** A filled diamond (square rotated 45°), used for the Ooga mark. */
	public static void diamond(GuiGraphics g, float cx, float cy, float halfSize, int color) {
		int c = apply(color);
		if (ColorUtil.alpha(c) == 0) return;
		int s = guiScale();
		begin(g, s);
		float pcx = cx * s;
		float pcy = cy * s;
		float half = halfSize * s;
		int top = (int) Math.floor(pcy - half);
		int bottom = (int) Math.ceil(pcy + half);
		for (int row = top; row < bottom; row++) {
			float dy = Math.abs(row + 0.5f - pcy);
			float span = half - dy;
			if (span <= 0) continue;
			float left = pcx - span;
			float right = pcx + span;
			int l = (int) Math.ceil(left);
			int r = (int) Math.floor(right);
			if (r > l) g.fill(l, row, r, row + 1, c);
			// Anti-aliased edge pixels.
			float lc = l - left;
			float rc = right - r;
			if (lc > 0.03f) g.fill(l - 1, row, l, row + 1, ColorUtil.fade(c, lc));
			if (rc > 0.03f) g.fill(r, row, r + 1, row + 1, ColorUtil.fade(c, rc));
		}
		end(g);
	}

	/** A filled triangle, scanline-rasterised with anti-aliased horizontal edges. */
	public static void triangle(GuiGraphics g, float ax, float ay, float bx, float by, float cx, float cy, int color) {
		int c = apply(color);
		if (ColorUtil.alpha(c) == 0) return;
		int s = guiScale();
		begin(g, s);
		float[] xs = {ax * s, bx * s, cx * s};
		float[] ys = {ay * s, by * s, cy * s};
		int top = (int) Math.floor(Math.min(ys[0], Math.min(ys[1], ys[2])));
		int bottom = (int) Math.ceil(Math.max(ys[0], Math.max(ys[1], ys[2])));
		for (int row = top; row < bottom; row++) {
			float sy = row + 0.5f;
			float left = Float.MAX_VALUE, right = -Float.MAX_VALUE;
			for (int i = 0; i < 3; i++) {
				int j = (i + 1) % 3;
				float y0 = ys[i], y1 = ys[j];
				if ((sy < Math.min(y0, y1)) || (sy > Math.max(y0, y1)) || y0 == y1) continue;
				float x = xs[i] + (sy - y0) / (y1 - y0) * (xs[j] - xs[i]);
				left = Math.min(left, x);
				right = Math.max(right, x);
			}
			if (right <= left) continue;
			int l = (int) Math.ceil(left);
			int r = (int) Math.floor(right);
			if (r > l) g.fill(l, row, r, row + 1, c);
			float lc = l - left;
			float rc = right - r;
			if (lc > 0.03f) g.fill(l - 1, row, l, row + 1, ColorUtil.fade(c, lc));
			if (rc > 0.03f) g.fill(r, row, r + 1, row + 1, ColorUtil.fade(c, rc));
		}
		end(g);
	}

	/** A straight line of the given thickness (GUI units), rasterised with square stamps. */
	public static void line(GuiGraphics g, float x1, float y1, float x2, float y2, float thickness, int color) {
		int c = apply(color);
		if (ColorUtil.alpha(c) == 0) return;
		int s = guiScale();
		begin(g, s);
		float ax = x1 * s, ay = y1 * s, bx = x2 * s, by = y2 * s;
		float t = Math.max(1f, thickness * s);
		int steps = Math.max(1, Math.round(Math.max(Math.abs(bx - ax), Math.abs(by - ay))));
		int half = Math.round(t / 2f);
		int size = Math.max(1, Math.round(t));
		int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE;
		for (int i = 0; i <= steps; i++) {
			float f = i / (float) steps;
			int x = Math.round(ax + (bx - ax) * f) - half;
			int y = Math.round(ay + (by - ay) * f) - half;
			if (x == lastX && y == lastY) continue;
			g.fill(x, y, x + size, y + size, c);
			lastX = x;
			lastY = y;
		}
		end(g);
	}

	// ------------------------------------------------------------------ rasterisers (pixel space)

	/** Horizontal inset of a rounded corner at pixel row {@code row} (0 = outermost row). */
	private static float cornerInset(float radius, int row) {
		if (radius <= 0) return 0;
		float dy = radius - (row + 0.5f);
		if (dy <= 0) return 0;
		return radius - (float) Math.sqrt(Math.max(0, radius * radius - dy * dy));
	}

	static void roundRectPx(GuiGraphics g, int x1, int y1, int x2, int y2, float radius, int color) {
		int w = x2 - x1;
		int h = y2 - y1;
		if (w <= 0 || h <= 0) return;
		float r = Math.max(0, Math.min(radius, Math.min(w, h) / 2f));
		int rows = (int) Math.ceil(r);
		if (rows == 0) {
			g.fill(x1, y1, x2, y2, color);
			return;
		}
		if (h - rows * 2 > 0) g.fill(x1, y1 + rows, x2, y2 - rows, color);
		for (int i = 0; i < rows; i++) {
			float inset = cornerInset(r, i);
			int full = (int) Math.ceil(inset);
			float coverage = full - inset;
			int top = y1 + i;
			int bottom = y2 - 1 - i;
			if (x2 - full > x1 + full) {
				g.fill(x1 + full, top, x2 - full, top + 1, color);
				if (bottom != top) g.fill(x1 + full, bottom, x2 - full, bottom + 1, color);
			}
			if (coverage > 0.03f && full > 0) {
				int edge = ColorUtil.fade(color, coverage);
				g.fill(x1 + full - 1, top, x1 + full, top + 1, edge);
				g.fill(x2 - full, top, x2 - full + 1, top + 1, edge);
				if (bottom != top) {
					g.fill(x1 + full - 1, bottom, x1 + full, bottom + 1, edge);
					g.fill(x2 - full, bottom, x2 - full + 1, bottom + 1, edge);
				}
			}
		}
	}

	/**
	 * Fills the band between a rounded rectangle and the same rectangle inset by
	 * {@code thickness} pixels. Straight sections are merged into single fills so the cost is
	 * proportional to the corner radius, not the rectangle's size — important for glow, which
	 * draws many of these per element.
	 */
	static void ringPx(GuiGraphics g, int x1, int y1, int x2, int y2, float radius, int thickness, int color) {
		int w = x2 - x1;
		int h = y2 - y1;
		if (w <= 0 || h <= 0 || thickness <= 0) return;
		if (thickness * 2 >= Math.min(w, h)) {
			roundRectPx(g, x1, y1, x2, y2, radius, color);
			return;
		}
		float r = Math.max(0, Math.min(radius, Math.min(w, h) / 2f));
		float innerR = Math.max(0, r - thickness);
		int band = Math.max((int) Math.ceil(r), thickness);
		band = Math.min(band, h / 2);
		for (int i = 0; i < band; i++) {
			int outer = Math.round(cornerInset(r, i));
			int top = y1 + i;
			int bottom = y2 - 1 - i;
			if (i < thickness) {
				g.fill(x1 + outer, top, x2 - outer, top + 1, color);
				if (bottom != top) g.fill(x1 + outer, bottom, x2 - outer, bottom + 1, color);
			} else {
				int inner = thickness + Math.round(cornerInset(innerR, i - thickness));
				if (inner > outer) {
					g.fill(x1 + outer, top, x1 + inner, top + 1, color);
					g.fill(x2 - inner, top, x2 - outer, top + 1, color);
					if (bottom != top) {
						g.fill(x1 + outer, bottom, x1 + inner, bottom + 1, color);
						g.fill(x2 - inner, bottom, x2 - outer, bottom + 1, color);
					}
				}
			}
		}
		if (h - band * 2 > 0) {
			g.fill(x1, y1 + band, x1 + thickness, y2 - band, color);
			g.fill(x2 - thickness, y1 + band, x2, y2 - band, color);
		}
	}
}
