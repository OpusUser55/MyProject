package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.client.WatermarkModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

/**
 * The Ooga watermark: a charcoal capsule holding the diamond mark and a letter-spaced
 * wordmark. The mark carries the glow; the text stays crisp and unglowed for legibility.
 */
public class WatermarkHud extends HudElement {
	private static final float BASE_HEIGHT = 17f;
	private static final float TRACKING = 1.1f;
	private static final DateTimeFormatter CLOCK_24 = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter CLOCK_12 = DateTimeFormatter.ofPattern("h:mm a");

	private final WatermarkModule module;

	public WatermarkHud(WatermarkModule module) {
		super("watermark", "Watermark", Anchor.START, 0f, Anchor.START, 0f);
		this.module = module;
	}

	@Override
	public NumberSetting scaleSetting() {
		return module.scale;
	}

	@Override
	public boolean isVisible() {
		return module.isEnabled();
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		float scale = module.scale.getFloat();
		String style = module.style.get();
		boolean showWord = !style.equals("Mark");
		boolean showFull = style.equals("Full");
		String info = infoText();

		// Measure in unscaled units first.
		float pad = 6f;
		float markSize = 9f;
		float w = pad + markSize;
		if (showWord) w += 5f + trackedWidth("OOGA", Weight.DISPLAY, 1f);
		if (showFull) w += 3f + trackedWidth("CLIENT", Weight.SEMIBOLD, 0.72f);
		if (info != null) w += 12f + OogaFonts.width(info, Weight.REGULAR, 0.8f);
		w += pad;
		float h = BASE_HEIGHT;

		this.width = w * scale;
		this.height = h * scale;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		Render2D.pushAlpha(module.opacity.getFloat());

		Render2D.roundRect(g, 0, 0, w, h, h / 2f, 0xE00E0F12);
		Render2D.outline(g, 0, 0, w, h, h / 2f, OogaTheme.BORDER);

		float cx = pad + markSize / 2f;
		float cy = h / 2f;
		float glowStrength = module.glow.getFloat();
		if (glowStrength > 0) GlowRenderer.glow(g, cx - 3.5f, cy - 3.5f, 7f, 7f, 3.5f, OogaTheme.GOLD, glowStrength);
		Render2D.diamond(g, cx, cy, markSize / 2f, OogaTheme.accent(0x59));
		Render2D.diamond(g, cx, cy, markSize / 4f, OogaTheme.GOLD_BRIGHT);

		float tx = pad + markSize + 5f;
		if (showWord) {
			tx = drawTracked(g, "OOGA", tx, cy - 5f, OogaTheme.TEXT, Weight.DISPLAY, 1f);
		}
		if (showFull) {
			tx += 3f;
			tx = drawTracked(g, "CLIENT", tx, cy - 2.6f, OogaTheme.GOLD_TEXT, Weight.SEMIBOLD, 0.72f);
		}
		if (info != null) {
			tx += 6f;
			Render2D.rect(g, tx - 0.5f, cy - 4f, 1f, 8f, OogaTheme.BORDER_STRONG);
			tx += 6f;
			OogaFonts.draw(g, info, tx, cy - 3.4f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.8f);
		}

		Render2D.popAlpha();
		g.pose().popMatrix();
	}

	/** The trailing readout: any of FPS, time and server, separated by middots. */
	private String infoText() {
		Minecraft mc = Minecraft.getInstance();
		StringJoiner joiner = new StringJoiner("  ·  ");
		if (module.fps.get()) joiner.add(mc.getFps() + " fps");
		if (module.time.get()) {
			joiner.add(LocalTime.now().format(module.clock.is("12h") ? CLOCK_12 : CLOCK_24));
		}
		if (module.server.get()) {
			ServerData server = mc.getCurrentServer();
			joiner.add(server != null ? server.ip : "Singleplayer");
		}
		return joiner.length() == 0 ? null : joiner.toString();
	}

	private static float trackedWidth(String text, Weight weight, float scale) {
		float w = 0;
		for (int i = 0; i < text.length(); i++) {
			w += OogaFonts.width(String.valueOf(text.charAt(i)), weight, scale);
			if (i < text.length() - 1) w += TRACKING * scale;
		}
		return w;
	}

	private static float drawTracked(GuiGraphics g, String text, float x, float y, int color, Weight weight, float scale) {
		for (int i = 0; i < text.length(); i++) {
			String ch = String.valueOf(text.charAt(i));
			OogaFonts.draw(g, ch, x, y, color, weight, scale);
			x += OogaFonts.width(ch, weight, scale);
			if (i < text.length() - 1) x += TRACKING * scale;
		}
		return x;
	}
}
