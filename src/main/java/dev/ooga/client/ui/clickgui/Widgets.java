package dev.ooga.client.ui.clickgui;

import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.gui.GuiGraphics;

/** Reusable Ooga controls, drawn from animation progress values so callers own the state. */
public final class Widgets {
	private Widgets() {
	}

	/**
	 * Pill switch. {@code on} is the animated 0..1 state; {@code hover} brightens the track.
	 */
	public static void toggle(GuiGraphics g, float x, float y, float w, float h, float on, float hover) {
		float r = h / 2f;
		if (on > 0.01f) GlowRenderer.glow(g, x, y, w, h, r, OogaTheme.GOLD, on * 0.7f);
		int offTrack = ColorUtil.lerp(OogaTheme.SURFACE_CONTROL, 0xFF30343C, hover);
		int track = ColorUtil.lerp(offTrack, OogaTheme.GOLD, on);
		Render2D.roundRect(g, x, y, w, h, r, track);
		if (on < 0.99f) Render2D.outline(g, x, y, w, h, r, ColorUtil.fade(OogaTheme.BORDER, 1f - on));

		float knobR = r - 1.6f;
		float knobX = x + r + (w - h) * on;
		int knob = ColorUtil.lerp(0xFF8D9099, OogaTheme.ON_GOLD, on);
		Render2D.circle(g, knobX, y + r, knobR, knob);
	}

	/** Slider track with gold fill and a knob. {@code progress} is 0..1. */
	public static void slider(GuiGraphics g, float x, float y, float w, float progress, float active) {
		float trackH = 2.5f;
		float cy = y;
		Render2D.roundRect(g, x, cy - trackH / 2f, w, trackH, trackH / 2f, OogaTheme.SURFACE_CONTROL);
		float fill = Math.max(trackH, w * progress);
		Render2D.roundRect(g, x, cy - trackH / 2f, fill, trackH, trackH / 2f, OogaTheme.GOLD);
		float kx = x + w * progress;
		float kr = 3.4f + 0.8f * active;
		GlowRenderer.glowCircle(g, kx, cy, kr, OogaTheme.GOLD, 0.35f + 0.5f * active);
		Render2D.circle(g, kx, cy, kr, OogaTheme.GOLD_BRIGHT);
		Render2D.circle(g, kx, cy, kr - 1.6f, OogaTheme.ON_GOLD);
	}

	/** Small rounded chip with centered text, used for keybinds and mode values. */
	public static float chip(GuiGraphics g, float rightX, float cy, String text, int textColor, int fill, int border, float scale) {
		float tw = OogaFonts.width(text, Weight.SEMIBOLD, scale);
		float w = tw + 9f;
		float h = 11f;
		float x = rightX - w;
		float y = cy - h / 2f;
		Render2D.roundRect(g, x, y, w, h, OogaTheme.RADIUS_CONTROL, fill);
		if (border != 0) Render2D.outline(g, x, y, w, h, OogaTheme.RADIUS_CONTROL, border);
		OogaFonts.draw(g, text, x + 4.5f, cy - OogaFonts.height(scale) / 2f + 0.5f, textColor, Weight.SEMIBOLD, scale);
		return w;
	}

	public static void tooltip(GuiGraphics g, float mouseX, float mouseY, String text, float screenW, float screenH) {
		float scale = 0.85f;
		float maxW = 170f;
		String[] words = text.split(" ");
		java.util.List<String> lines = new java.util.ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : words) {
			String candidate = line.isEmpty() ? word : line + " " + word;
			if (OogaFonts.width(candidate, Weight.REGULAR, scale) > maxW && !line.isEmpty()) {
				lines.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(candidate);
			}
		}
		if (!line.isEmpty()) lines.add(line.toString());

		float lineH = OogaFonts.height(scale) + 2f;
		float w = 0;
		for (String l : lines) w = Math.max(w, OogaFonts.width(l, Weight.REGULAR, scale));
		w += 12f;
		float h = lines.size() * lineH + 8f;
		float x = Math.min(mouseX + 10f, screenW - w - 4f);
		float y = Math.min(mouseY + 12f, screenH - h - 4f);
		Render2D.roundRect(g, x, y, w, h, OogaTheme.RADIUS_CONTROL + 1, 0xF7101115);
		Render2D.outline(g, x, y, w, h, OogaTheme.RADIUS_CONTROL + 1, OogaTheme.BORDER_STRONG);
		float ty = y + 5f;
		for (String l : lines) {
			OogaFonts.draw(g, l, x + 6f, ty, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, scale);
			ty += lineH;
		}
	}
}
