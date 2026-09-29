package dev.ooga.client.ui.render;

import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Ooga's own line-icon set, drawn procedurally so it scales cleanly at every GUI scale and
 * needs no texture atlas. Each icon is designed on a unit square and stroked in one color.
 */
public enum Icon {
	COMBAT {
		@Override
		void paint(GuiGraphics g, float x, float y, float s, float t, int c) {
			// Two crossed blades.
			Render2D.line(g, x + s * 0.18f, y + s * 0.18f, x + s * 0.82f, y + s * 0.82f, t, c);
			Render2D.line(g, x + s * 0.82f, y + s * 0.18f, x + s * 0.18f, y + s * 0.82f, t, c);
			Render2D.line(g, x + s * 0.08f, y + s * 0.62f, x + s * 0.38f, y + s * 0.92f, t, c);
			Render2D.line(g, x + s * 0.92f, y + s * 0.62f, x + s * 0.62f, y + s * 0.92f, t, c);
		}
	},
	MOVEMENT {
		@Override
		void paint(GuiGraphics g, float x, float y, float s, float t, int c) {
			// Double chevron.
			for (float o : new float[]{0.12f, 0.46f}) {
				Render2D.line(g, x + s * o, y + s * 0.2f, x + s * (o + 0.3f), y + s * 0.5f, t, c);
				Render2D.line(g, x + s * (o + 0.3f), y + s * 0.5f, x + s * o, y + s * 0.8f, t, c);
			}
		}
	},
	RENDER {
		@Override
		void paint(GuiGraphics g, float x, float y, float s, float t, int c) {
			// Eye: almond outline with a solid pupil.
			Render2D.line(g, x + s * 0.05f, y + s * 0.5f, x + s * 0.5f, y + s * 0.2f, t, c);
			Render2D.line(g, x + s * 0.5f, y + s * 0.2f, x + s * 0.95f, y + s * 0.5f, t, c);
			Render2D.line(g, x + s * 0.05f, y + s * 0.5f, x + s * 0.5f, y + s * 0.8f, t, c);
			Render2D.line(g, x + s * 0.5f, y + s * 0.8f, x + s * 0.95f, y + s * 0.5f, t, c);
			Render2D.circle(g, x + s * 0.5f, y + s * 0.5f, s * 0.15f, c);
		}
	},
	WORLD {
		@Override
		void paint(GuiGraphics g, float x, float y, float s, float t, int c) {
			// Isometric cube.
			float cx = x + s * 0.5f;
			Render2D.line(g, cx, y + s * 0.08f, x + s * 0.9f, y + s * 0.3f, t, c);
			Render2D.line(g, cx, y + s * 0.08f, x + s * 0.1f, y + s * 0.3f, t, c);
			Render2D.line(g, x + s * 0.1f, y + s * 0.3f, cx, y + s * 0.52f, t, c);
			Render2D.line(g, x + s * 0.9f, y + s * 0.3f, cx, y + s * 0.52f, t, c);
			Render2D.line(g, x + s * 0.1f, y + s * 0.3f, x + s * 0.1f, y + s * 0.72f, t, c);
			Render2D.line(g, x + s * 0.9f, y + s * 0.3f, x + s * 0.9f, y + s * 0.72f, t, c);
			Render2D.line(g, cx, y + s * 0.52f, cx, y + s * 0.94f, t, c);
			Render2D.line(g, x + s * 0.1f, y + s * 0.72f, cx, y + s * 0.94f, t, c);
			Render2D.line(g, x + s * 0.9f, y + s * 0.72f, cx, y + s * 0.94f, t, c);
		}
	},
	MISC {
		@Override
		void paint(GuiGraphics g, float x, float y, float s, float t, int c) {
			// Four-square grid.
			float cell = s * 0.36f;
			float gap = s * 0.12f;
			float o = (s - cell * 2 - gap) / 2f;
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < 2; j++) {
					float cx = x + o + i * (cell + gap);
					float cy = y + o + j * (cell + gap);
					Render2D.outline(g, cx, cy, cell, cell, s * 0.08f, Math.max(1, Math.round(t * Render2D.guiScale())), c);
				}
			}
		}
	},
	CLIENT {
		@Override
		void paint(GuiGraphics g, float x, float y, float s, float t, int c) {
			// The Ooga mark: a translucent diamond around a solid core.
			Render2D.diamond(g, x + s / 2f, y + s / 2f, s * 0.48f, ColorUtil.fade(c, 0.35f));
			Render2D.diamond(g, x + s / 2f, y + s / 2f, s * 0.24f, c);
		}
	},
	SEARCH {
		@Override
		void paint(GuiGraphics g, float x, float y, float s, float t, int c) {
			Render2D.outline(g, x + s * 0.06f, y + s * 0.06f, s * 0.62f, s * 0.62f, s * 0.31f, Math.max(1, Math.round(t * Render2D.guiScale())), c);
			Render2D.line(g, x + s * 0.6f, y + s * 0.6f, x + s * 0.92f, y + s * 0.92f, t, c);
		}
	},
	CHEVRON {
		@Override
		void paint(GuiGraphics g, float x, float y, float s, float t, int c) {
			// Points down; callers rotate by choosing CHEVRON_RIGHT instead.
			Render2D.line(g, x + s * 0.2f, y + s * 0.35f, x + s * 0.5f, y + s * 0.65f, t, c);
			Render2D.line(g, x + s * 0.5f, y + s * 0.65f, x + s * 0.8f, y + s * 0.35f, t, c);
		}
	},
	CHEVRON_RIGHT {
		@Override
		void paint(GuiGraphics g, float x, float y, float s, float t, int c) {
			Render2D.line(g, x + s * 0.35f, y + s * 0.2f, x + s * 0.65f, y + s * 0.5f, t, c);
			Render2D.line(g, x + s * 0.65f, y + s * 0.5f, x + s * 0.35f, y + s * 0.8f, t, c);
		}
	},
	MOVE {
		@Override
		void paint(GuiGraphics g, float x, float y, float s, float t, int c) {
			float m = s / 2f;
			Render2D.line(g, x + m, y + s * 0.08f, x + m, y + s * 0.92f, t, c);
			Render2D.line(g, x + s * 0.08f, y + m, x + s * 0.92f, y + m, t, c);
			Render2D.circle(g, x + m, y + m, s * 0.12f, c);
		}
	};

	abstract void paint(GuiGraphics g, float x, float y, float size, float stroke, int color);

	/** Draws the icon in a {@code size}×{@code size} box at (x, y). */
	public void draw(GuiGraphics g, float x, float y, float size, int color) {
		// Stroke of ~1.2 GUI px at typical sizes, never thinner than one screen pixel.
		float stroke = Math.max(1f / Render2D.guiScale(), size * 0.11f);
		paint(g, x, y, size, stroke, color);
	}
}
