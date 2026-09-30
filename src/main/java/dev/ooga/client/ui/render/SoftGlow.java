package dev.ooga.client.ui.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.ooga.client.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Real soft glow: a blurred light sprite generated once at startup and stretched around any
 * rectangle as a nine-slice (corners stay round, edges stretch). One draw per slice, so even a
 * wide glow costs nine quads, and it's genuinely smooth rather than stepped rings.
 *
 * <p>Drawn additively by default ("bloom"): overlapping glows brighten each other like light
 * does, which is what makes dark UIs feel lit from within.
 */
public final class SoftGlow {
	/** Radius of the light falloff in the texture, in texels. */
	private static final int R = 32;
	private static final int SIZE = R * 2 + 1;
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("ooga", "soft_glow");
	/** 128x1: alpha ramps up over the left half and back down over the right half. */
	private static final Identifier FADE = Identifier.fromNamespaceAndPath("ooga", "fade");
	private static final int FADE_W = 128;
	private static final RenderPipeline ADDITIVE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath("ooga", "pipeline/gui_glow_additive"))
			.withBlend(BlendFunction.LIGHTNING)
			.build();

	private static boolean ready;
	private static boolean failed;

	private SoftGlow() {
	}

	private static boolean ensure() {
		if (ready) return true;
		if (failed) return false;
		try {
			NativeImage image = new NativeImage(SIZE, SIZE, false);
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					double d = Math.min(1.0, Math.hypot(x - R, y - R) / R);
					// Gaussian-ish core with a gentle tail that reaches exactly zero at the edge.
					double a = Math.exp(-3.2 * d * d) * (1 - d) * (1 - d * 0.35);
					int alpha = (int) Math.round(255 * Math.max(0, Math.min(1, a)));
					image.setPixel(x, y, (alpha << 24) | 0xFFFFFF);
				}
			}
			DynamicTexture texture = new DynamicTexture(() -> "ooga soft glow", image) {
				{
					// Smooth sampling: the sprite is scaled to any size.
					sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
				}
			};
			Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);

			NativeImage fade = new NativeImage(FADE_W, 1, false);
			int half = FADE_W / 2;
			for (int x = 0; x < FADE_W; x++) {
				float t = x < half ? (x + 0.5f) / half : (FADE_W - x - 0.5f) / half;
				// Smoothstep, so washes don't show a hard start.
				float a = t * t * (3 - 2 * t);
				fade.setPixel(x, 0, (Math.round(255 * a) << 24) | 0xFFFFFF);
			}
			DynamicTexture fadeTexture = new DynamicTexture(() -> "ooga fade", fade) {
				{
					sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
				}
			};
			Minecraft.getInstance().getTextureManager().register(FADE, fadeTexture);
			ready = true;
		} catch (RuntimeException e) {
			failed = true;
			ModuleManager.LOGGER.warn("Soft glow unavailable, falling back to ring glow", e);
		}
		return ready;
	}

	public static boolean available() {
		return ensure();
	}

	/**
	 * Glow around (and optionally inside) a rectangle, in GUI units.
	 *
	 * @param spread how far the light reaches past the edge
	 * @param color  ARGB; the alpha is the glow's peak strength
	 * @param hollow skip the centre slice, so translucent panels aren't tinted from behind
	 * @param additive bloom blending (true) or ordinary alpha blending
	 */
	public static boolean rect(GuiGraphics g, float x, float y, float w, float h, float spread, int color, boolean hollow, boolean additive) {
		if ((color >>> 24) == 0 || spread <= 0 || !ensure()) return ready;
		int s = Render2D.guiScale();
		int x1 = Math.round(x * s), y1 = Math.round(y * s);
		int x2 = Math.max(x1, Math.round((x + w) * s)), y2 = Math.max(y1, Math.round((y + h) * s));
		int r = Math.max(1, Math.round(spread * s));
		int cw = x2 - x1, ch = y2 - y1;
		RenderPipeline pipeline = additive ? ADDITIVE : RenderPipelines.GUI_TEXTURED;

		g.pose().pushMatrix();
		g.pose().scale(1f / s, 1f / s);
		// Corners.
		blit(g, pipeline, x1 - r, y1 - r, r, r, 0, 0, R, R, color);
		blit(g, pipeline, x2, y1 - r, r, r, R + 1, 0, R, R, color);
		blit(g, pipeline, x1 - r, y2, r, r, 0, R + 1, R, R, color);
		blit(g, pipeline, x2, y2, r, r, R + 1, R + 1, R, R, color);
		// Edges: the single centre row/column of the sprite, stretched.
		if (cw > 0) {
			blit(g, pipeline, x1, y1 - r, cw, r, R, 0, 1, R, color);
			blit(g, pipeline, x1, y2, cw, r, R, R + 1, 1, R, color);
		}
		if (ch > 0) {
			blit(g, pipeline, x1 - r, y1, r, ch, 0, R, R, 1, color);
			blit(g, pipeline, x2, y1, r, ch, R + 1, R, R, 1, color);
		}
		if (!hollow && cw > 0 && ch > 0) blit(g, pipeline, x1, y1, cw, ch, R, R, 1, 1, color);
		g.pose().popMatrix();
		return true;
	}

	/** A round blob of light centred on (cx, cy) with the given radius. */
	public static boolean blob(GuiGraphics g, float cx, float cy, float radius, int color, boolean additive) {
		return rect(g, cx, cy, 0, 0, radius, color, false, additive);
	}

	/**
	 * A colour wash that fades out to one side: {@code toRight} means full strength at the left
	 * edge, transparent at the right. Normal alpha blending.
	 */
	public static boolean fade(GuiGraphics g, float x, float y, float w, float h, int color, boolean toRight) {
		if ((color >>> 24) == 0 || w <= 0 || h <= 0 || !ensure()) return ready;
		int s = Render2D.guiScale();
		int x1 = Math.round(x * s), y1 = Math.round(y * s);
		int x2 = Math.round((x + w) * s), y2 = Math.round((y + h) * s);
		if (x2 <= x1 || y2 <= y1) return true;
		g.pose().pushMatrix();
		g.pose().scale(1f / s, 1f / s);
		int u = toRight ? FADE_W / 2 : 0;
		g.blit(RenderPipelines.GUI_TEXTURED, FADE, x1, y1, u, 0, x2 - x1, y2 - y1, FADE_W / 2, 1, FADE_W, 1, color);
		g.pose().popMatrix();
		return true;
	}

	private static void blit(GuiGraphics g, RenderPipeline pipeline, int x, int y, int w, int h, int u, int v, int uw, int vh, int color) {
		if (w <= 0 || h <= 0) return;
		g.blit(pipeline, TEXTURE, x, y, u, v, w, h, uw, vh, SIZE, SIZE, color);
	}
}
