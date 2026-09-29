package dev.ooga.client.ui.render;

import dev.ooga.client.module.impl.client.ClientSettings;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The single place Ooga draws its soft golden glow. Components ask for glow around a rounded
 * shape; this class decides how (or whether) to draw it based on the user's Glow settings, so
 * low-end machines can switch it off globally and every component respects that.
 *
 * <p>The glow is built from concentric one-pixel rings whose opacity falls off quadratically.
 * Only the rings are drawn — never the interior — so translucent panels don't get tinted.
 */
public final class GlowRenderer {
	/** Upper bound on rings per element, so a large radius at high GUI scale stays cheap. */
	private static final int MAX_LAYERS = 18;

	private GlowRenderer() {
	}

	public static boolean enabled() {
		return ClientSettings.glowEnabled();
	}

	/**
	 * @param strength per-call multiplier (0..1), e.g. an animation's progress, applied on top
	 *                 of the global intensity.
	 */
	public static void glow(GuiGraphics g, float x, float y, float w, float h, float cornerRadius, int color, float strength) {
		glow(g, x, y, w, h, cornerRadius, color, strength, ClientSettings.glowRadius());
	}

	public static void glow(GuiGraphics g, float x, float y, float w, float h, float cornerRadius, int color, float strength, float radius) {
		if (!enabled() || strength <= 0.01f || w <= 0 || h <= 0) return;

		float intensity = ClientSettings.glowIntensity() * strength * pulse();
		if (intensity <= 0.01f) return;

		int scale = Render2D.guiScale();
		int layers = Math.min(MAX_LAYERS, Math.max(1, Math.round(radius * scale)));
		// Peak opacity of the innermost ring. Kept low: glow should be felt more than seen.
		float peak = 0.34f * intensity * Render2D.alpha();

		int x1 = Math.round(x * scale);
		int y1 = Math.round(y * scale);
		int x2 = Math.round((x + w) * scale);
		int y2 = Math.round((y + h) * scale);
		float r = cornerRadius * scale;

		g.pose().pushMatrix();
		g.pose().scale(1f / scale, 1f / scale);
		for (int k = 1; k <= layers; k++) {
			float falloff = 1f - (k - 0.5f) / layers;
			int alpha = Math.round(255 * peak * falloff * falloff);
			if (alpha <= 0) continue;
			int ringColor = ColorUtil.withAlpha(color, alpha);
			Render2D.ringPx(g, x1 - k, y1 - k, x2 + k, y2 + k, r + k, 1, ringColor);
		}
		g.pose().popMatrix();
	}

	/** Glow for circular shapes such as toggle knobs. */
	public static void glowCircle(GuiGraphics g, float cx, float cy, float radius, int color, float strength) {
		glow(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color, strength);
	}

	/** Slow "breathing" modulation, controlled by the glow animation speed setting. */
	private static float pulse() {
		float speed = ClientSettings.glowPulseSpeed();
		if (speed <= 0) return 1f;
		double t = System.currentTimeMillis() / 1000.0 * speed;
		return 0.86f + 0.14f * (float) (0.5 + 0.5 * Math.sin(t * Math.PI * 2 / 3));
	}
}
