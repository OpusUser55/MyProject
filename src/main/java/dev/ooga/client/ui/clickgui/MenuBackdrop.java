package dev.ooga.client.ui.clickgui;

import dev.ooga.client.module.impl.client.ClickGuiModule;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/**
 * Everything behind the menu panels: an optional blur of the world, a dim, slow "aurora" of
 * accent-coloured light drifting across the screen, and small glowing particles floating up.
 * All of it fades with the menu's open animation.
 */
public final class MenuBackdrop {
	private static final int PARTICLES = 42;

	private final float[] px = new float[PARTICLES];
	private final float[] py = new float[PARTICLES];
	private final float[] speed = new float[PARTICLES];
	private final float[] size = new float[PARTICLES];
	private final float[] phase = new float[PARTICLES];
	private final Random random = new Random();
	private boolean seeded;
	private long lastFrame;

	/** Call from {@code renderBackground}; {@code open} is the 0..1 open animation. */
	public void draw(GuiGraphics g, ClickGuiModule config, float w, float h, float open) {
		if (open <= 0.01f) return;
		if (config.blur.get()) g.blurBeforeThisStratum();
		float dim = open * config.dim.getFloat();
		if (dim > 0.01f) Render2D.rect(g, 0, 0, w, h, ColorUtil.fade(0xB0050507, dim));

		double t = System.currentTimeMillis() / 1000.0;
		if (config.aurora.get()) {
			float strength = 0.30f * open;
			// Three large lights on slow Lissajous paths, in both accent colours.
			GlowRenderer.light(g, (float) (w * (0.25 + 0.15 * Math.sin(t * 0.13))), (float) (h * (0.30 + 0.12 * Math.cos(t * 0.17))),
					Math.max(w, h) * 0.42f, OogaTheme.GOLD, strength);
			GlowRenderer.light(g, (float) (w * (0.75 + 0.14 * Math.cos(t * 0.11))), (float) (h * (0.65 + 0.14 * Math.sin(t * 0.15))),
					Math.max(w, h) * 0.40f, OogaTheme.ACCENT_2, strength * 0.9f);
			GlowRenderer.light(g, (float) (w * (0.50 + 0.25 * Math.sin(t * 0.07 + 2))), (float) (h * (1.02 + 0.05 * Math.sin(t * 0.21))),
					Math.max(w, h) * 0.35f, ColorUtil.lerp(OogaTheme.GOLD, OogaTheme.ACCENT_2, 0.5f), strength * 0.8f);
		}
		if (config.particles.get()) drawParticles(g, w, h, open, t);
	}

	private void drawParticles(GuiGraphics g, float w, float h, float open, double t) {
		long now = System.nanoTime();
		float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1.0E9f);
		lastFrame = now;
		if (!seeded) {
			for (int i = 0; i < PARTICLES; i++) respawn(i, w, h, true);
			seeded = true;
		}
		for (int i = 0; i < PARTICLES; i++) {
			py[i] -= speed[i] * dt;
			px[i] += (float) Math.sin(t * 0.6 + phase[i]) * 4f * dt;
			if (py[i] < -8) respawn(i, w, h, false);
			if (px[i] < -8 || px[i] > w + 8) respawn(i, w, h, true);
			// Twinkle, and fade in near the bottom / out near the top.
			float twinkle = 0.55f + 0.45f * (float) Math.sin(t * 1.7 + phase[i] * 3);
			float edge = Math.min(1f, Math.min(py[i] / (h * 0.15f), (h - py[i]) / (h * 0.1f)));
			float a = open * twinkle * Math.max(0f, edge);
			if (a <= 0.02f) continue;
			int color = i % 3 == 0 ? OogaTheme.ACCENT_2 : OogaTheme.GOLD;
			GlowRenderer.light(g, px[i], py[i], size[i] * 3.5f, color, 0.35f * a);
			Render2D.circle(g, px[i], py[i], size[i] * 0.5f, ColorUtil.withAlpha(OogaTheme.GOLD_BRIGHT, Math.round(200 * a)));
		}
	}

	private void respawn(int i, float w, float h, boolean anywhere) {
		px[i] = random.nextFloat() * w;
		py[i] = anywhere ? random.nextFloat() * h : h + random.nextFloat() * 20f;
		speed[i] = 6f + random.nextFloat() * 16f;
		size[i] = 0.8f + random.nextFloat() * 1.6f;
		phase[i] = random.nextFloat() * 6.28f;
	}
}
