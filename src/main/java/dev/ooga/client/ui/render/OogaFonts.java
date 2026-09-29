package dev.ooga.client.ui.render;

import dev.ooga.client.module.impl.client.ClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * Typography. Ooga ships Inter (see assets/ooga/font) at several real sizes rather than one
 * size scaled up and down: scaled glyphs go soft, glyphs rasterised at their size stay sharp.
 * A requested scale picks the nearest real size and only the small remainder is scaled.
 * Anything outside Latin falls back to the vanilla font per glyph, and users can switch back
 * to the vanilla font entirely from Client Settings.
 */
public final class OogaFonts {
	public enum Weight {
		REGULAR(9f, new int[]{7, 8, 9}, "ui_7", "ui_8", "ui"),
		SEMIBOLD(9f, new int[]{7, 8, 9}, "ui_bold_7", "ui_bold_8", "ui_bold"),
		DISPLAY(11f, new int[]{11}, "display");

		private final float baseSize;
		private final int[] sizes;
		private final FontDescription[] descriptions;

		Weight(float baseSize, int[] sizes, String... paths) {
			this.baseSize = baseSize;
			this.sizes = sizes;
			this.descriptions = new FontDescription[paths.length];
			for (int i = 0; i < paths.length; i++) {
				descriptions[i] = new FontDescription.Resource(Identifier.fromNamespaceAndPath("ooga", paths[i]));
			}
		}

		/** Index of the real size closest to {@code baseSize * scale}. */
		private int pick(float scale) {
			float target = baseSize * scale;
			int best = sizes.length - 1;
			for (int i = 0; i < sizes.length; i++) {
				if (Math.abs(sizes[i] - target) < Math.abs(sizes[best] - target)) best = i;
			}
			return best;
		}
	}

	private OogaFonts() {
	}

	private static Font font() {
		return Minecraft.getInstance().font;
	}

	private static Component text(String text, Weight weight, int sizeIndex) {
		MutableComponent component = Component.literal(text);
		if (ClientSettings.customFont()) {
			FontDescription description = weight.descriptions[sizeIndex];
			return component.withStyle(style -> style.withFont(description));
		}
		return weight == Weight.REGULAR ? component : component.withStyle(style -> style.withBold(weight == Weight.DISPLAY));
	}

	/** Remaining scale to apply after choosing a real size. */
	private static float residual(Weight weight, int sizeIndex, float scale) {
		if (!ClientSettings.customFont()) return scale;
		return weight.baseSize * scale / weight.sizes[sizeIndex];
	}

	public static float width(String text, Weight weight) {
		return width(text, weight, 1f);
	}

	public static float width(String text, Weight weight, float scale) {
		int index = weight.pick(scale);
		return font().width(text(text, weight, index)) * residual(weight, index, scale);
	}

	public static float height(float scale) {
		return font().lineHeight * scale;
	}

	public static void draw(GuiGraphics g, String text, float x, float y, int color, Weight weight) {
		draw(g, text, x, y, color, weight, 1f);
	}

	public static void draw(GuiGraphics g, String text, float x, float y, int color, Weight weight, float scale) {
		int c = Render2D.apply(color);
		if ((c >>> 24) < 4) return;
		int index = weight.pick(scale);
		float rest = residual(weight, index, scale);
		// Snap to the physical pixel grid: text between pixels is resampled and looks smeared.
		int gs = Render2D.guiScale();
		float sx = Math.round(x * gs) / (float) gs;
		float sy = Math.round(y * gs) / (float) gs;
		g.pose().pushMatrix();
		g.pose().translate(sx, sy);
		if (Math.abs(rest - 1f) > 0.01f) g.pose().scale(rest, rest);
		g.drawString(font(), text(text, weight, index), 0, 0, c, false);
		g.pose().popMatrix();
	}

	public static void drawCentered(GuiGraphics g, String text, float cx, float y, int color, Weight weight, float scale) {
		draw(g, text, cx - width(text, weight, scale) / 2f, y, color, weight, scale);
	}

	/** Truncates with an ellipsis so text never overflows its container. */
	public static String trim(String text, Weight weight, float scale, float maxWidth) {
		if (width(text, weight, scale) <= maxWidth) return text;
		String ellipsis = "…";
		int end = text.length();
		while (end > 0 && width(text.substring(0, end) + ellipsis, weight, scale) > maxWidth) end--;
		return text.substring(0, end) + ellipsis;
	}
}
