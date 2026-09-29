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
 * Typography. Ooga ships Inter (see assets/ooga/font) in three weights and falls back to the
 * vanilla font per glyph for anything outside Latin. Users can switch back to the vanilla font
 * entirely from Client Settings.
 */
public final class OogaFonts {
	public enum Weight {
		REGULAR("ui"),
		SEMIBOLD("ui_bold"),
		DISPLAY("display");

		private final FontDescription description;

		Weight(String path) {
			this.description = new FontDescription.Resource(Identifier.fromNamespaceAndPath("ooga", path));
		}
	}

	private OogaFonts() {
	}

	private static Font font() {
		return Minecraft.getInstance().font;
	}

	public static Component text(String text, Weight weight) {
		MutableComponent component = Component.literal(text);
		if (ClientSettings.customFont()) {
			return component.withStyle(style -> style.withFont(weight.description));
		}
		return weight == Weight.REGULAR ? component : component.withStyle(style -> style.withBold(weight == Weight.DISPLAY));
	}

	public static float width(String text, Weight weight) {
		return font().width(text(text, weight));
	}

	public static float width(String text, Weight weight, float scale) {
		return width(text, weight) * scale;
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
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		if (scale != 1f) g.pose().scale(scale, scale);
		g.drawString(font(), text(text, weight), 0, 0, c, false);
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
