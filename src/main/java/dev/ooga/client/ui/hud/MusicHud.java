package dev.ooga.client.ui.hud;

import dev.ooga.client.media.MediaInfo;
import dev.ooga.client.media.MediaManager;
import dev.ooga.client.module.impl.client.MusicModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.Icon;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.Anim;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Now-playing card. The artwork slot is Ooga's own motif — a dark disc with a gold ring and a
 * mark that orbits while music plays — rather than album art, so the widget always matches
 * the client's palette whatever is playing.
 */
public class MusicHud extends HudElement {
	private static final float W = 158f;
	private static final float H = 44f;
	private static final float DISC = 30f;
	private static final float ICON = 7f;

	private final MusicModule module;
	private final Anim appear = new Anim(0f, 10f);
	private final Anim playState = new Anim(0f, 12f);
	private final Anim[] buttonHover = {new Anim(0f, 18f), new Anim(0f, 18f), new Anim(0f, 18f)};
	private MediaInfo lastInfo;
	private float discAngle;
	private long lastFrame = System.nanoTime();

	// Control hit boxes in screen space, refreshed every frame.
	private final float[][] buttons = new float[3][4];

	public MusicHud(MusicModule module) {
		super("music", "Music", Anchor.CENTER, 0.5f, Anchor.START, 0f);
		this.module = module;
	}

	@Override
	public NumberSetting scaleSetting() {
		return module.scale;
	}

	private boolean editing() {
		return Minecraft.getInstance().screen instanceof HudEditorScreen;
	}

	@Override
	public boolean isVisible() {
		if (!module.isEnabled()) return false;
		if (editing() || !module.hideIdle.get()) return true;
		// Stay visible while fading out.
		return MediaManager.get().current() != null || appear.get() > 0.01f;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		MediaInfo info = MediaManager.get().current();
		if (info != null) lastInfo = info;
		boolean show = info != null || editing() || !module.hideIdle.get();
		float t = Anim.ease(appear.update(show ? 1f : 0f));
		MediaInfo shown = info != null ? info : lastInfo;
		if (info == null && (editing() || !module.hideIdle.get())) shown = null;

		float scale = module.scale.getFloat();
		width = W * scale;
		height = H * scale;
		if (t <= 0.01f) return;

		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
		lastFrame = now;
		boolean playing = shown != null && shown.playing();
		float play = playState.update(playing ? 1f : 0f);
		discAngle += dt * 1.6f * play;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		Render2D.pushAlpha(t);

		Render2D.roundRect(g, 0, 0, W, H, OogaTheme.RADIUS_CARD + 1, 0xE60E0F12);
		Render2D.outline(g, 0, 0, W, H, OogaTheme.RADIUS_CARD + 1, OogaTheme.BORDER);

		drawDisc(g, 7f, 7f, play);

		float textX = 7f + DISC + 8f;
		float controlsW = module.controls.get() ? ICON * 3 + 16f : 0f;
		float textW = W - textX - 8f - (controlsW > 0 ? controlsW + 6f : 0f);
		long millis = System.currentTimeMillis();
		if (shown == null) {
			OogaFonts.draw(g, "Nothing playing", textX, 11f, OogaTheme.TEXT_SECONDARY, Weight.SEMIBOLD);
			OogaFonts.draw(g, "Start music in any player", textX, 22f, OogaTheme.TEXT_MUTED, Weight.REGULAR, 0.75f);
		} else {
			drawMarquee(g, shown.title().isEmpty() ? "Unknown title" : shown.title(), textX, 8f, textW, Weight.SEMIBOLD, 1f, OogaTheme.TEXT, millis);
			drawMarquee(g, shown.artist().isEmpty() ? "Unknown artist" : shown.artist(), textX, 19f, textW + controlsW, Weight.REGULAR, 0.78f, OogaTheme.TEXT_SECONDARY, millis);

			float barY = 33f;
			float barW = W - textX - 8f;
			float progress = (float) shown.progressAt(millis);
			Render2D.roundRect(g, textX, barY, barW, 2f, 1f, OogaTheme.SURFACE_CONTROL);
			if (progress > 0) {
				Render2D.roundRect(g, textX, barY, Math.max(2f, barW * progress), 2f, 1f, OogaTheme.GOLD);
				GlowRenderer.glowCircle(g, textX + barW * progress, barY + 1f, 1.6f, OogaTheme.GOLD, 0.5f * play);
			}
			if (module.times.get()) {
				String elapsed = MediaInfo.formatTime(shown.positionAt(millis));
				String total = shown.durationSeconds() > 0 ? MediaInfo.formatTime(shown.durationSeconds()) : "--:--";
				OogaFonts.draw(g, elapsed, textX, barY + 3.5f, OogaTheme.TEXT_MUTED, Weight.REGULAR, 0.58f);
				float tw = OogaFonts.width(total, Weight.REGULAR, 0.58f);
				OogaFonts.draw(g, total, textX + barW - tw, barY + 3.5f, OogaTheme.TEXT_MUTED, Weight.REGULAR, 0.58f);
			}
		}

		if (module.controls.get()) drawControls(g, x, y, scale, playing, shown != null);

		Render2D.popAlpha();
		g.pose().popMatrix();
	}

	private void drawDisc(GuiGraphics g, float x, float y, float play) {
		float r = DISC / 2f;
		float cx = x + r;
		float cy = y + r;
		if (play > 0.01f) GlowRenderer.glowCircle(g, cx, cy, r, OogaTheme.GOLD, 0.35f * play);
		Render2D.circle(g, cx, cy, r, 0xFF16181D);
		Render2D.outline(g, cx - r, cy - r, r * 2, r * 2, r, 2, ColorUtil.lerp(OogaTheme.accent(0x55), OogaTheme.GOLD, play));
		// Grooves.
		Render2D.outline(g, cx - r * 0.72f, cy - r * 0.72f, r * 1.44f, r * 1.44f, r * 0.72f, 1, 0x14FFFFFF);
		Render2D.outline(g, cx - r * 0.5f, cy - r * 0.5f, r, r, r * 0.5f, 1, 0x10FFFFFF);
		// Label: the Ooga mark.
		Render2D.diamond(g, cx, cy, r * 0.28f, ColorUtil.lerp(OogaTheme.accent(0x80), OogaTheme.GOLD_BRIGHT, play));
		// Orbiting highlight.
		float orbit = r * 0.84f;
		float dx = (float) Math.cos(discAngle) * orbit;
		float dy = (float) Math.sin(discAngle) * orbit;
		Render2D.circle(g, cx + dx, cy + dy, 1.4f, ColorUtil.fade(OogaTheme.GOLD_BRIGHT, 0.35f + 0.65f * play));
	}

	private void drawMarquee(GuiGraphics g, String text, float x, float y, float maxW, Weight weight, float scale, int color, long millis) {
		float textW = OogaFonts.width(text, weight, scale);
		if (textW <= maxW || !module.marquee.get()) {
			OogaFonts.draw(g, OogaFonts.trim(text, weight, scale, maxW), x, y, color, weight, scale);
			return;
		}
		float gap = 24f;
		float cycle = textW + gap;
		// Pause at the start of each loop so the beginning of the title is readable.
		float travel = (millis / 1000f * 18f) % (cycle + 36f);
		float offset = Math.max(0f, travel - 36f);
		g.enableScissor(Math.round(x), Math.round(y - 1), Math.round(x + maxW), Math.round(y + 10 * scale + 1));
		OogaFonts.draw(g, text, x - offset, y, color, weight, scale);
		OogaFonts.draw(g, text, x - offset + cycle, y, color, weight, scale);
		g.disableScissor();
	}

	private void drawControls(GuiGraphics g, float originX, float originY, float scale, boolean playing, boolean active) {
		Icon[] icons = {Icon.PREVIOUS, playing ? Icon.PAUSE : Icon.PLAY, Icon.NEXT};
		float bx = W - 8f - ICON * 3 - 16f;
		float by = 9f;
		boolean interactive = Minecraft.getInstance().screen != null;
		double mx = mouseX();
		double my = mouseY();
		for (int i = 0; i < 3; i++) {
			float ix = bx + i * (ICON + 8f);
			float[] box = buttons[i];
			box[0] = originX + (ix - 3f) * scale;
			box[1] = originY + (by - 3f) * scale;
			box[2] = (ICON + 6f) * scale;
			box[3] = (ICON + 6f) * scale;
			boolean over = interactive && active && mx >= box[0] && mx < box[0] + box[2] && my >= box[1] && my < box[1] + box[3];
			float hv = buttonHover[i].update(over ? 1f : 0f);
			if (hv > 0.01f) Render2D.circle(g, ix + ICON / 2f, by + ICON / 2f, ICON * 0.9f, ColorUtil.fade(OogaTheme.accent(0x1F), hv));
			int base = i == 1 ? OogaTheme.TEXT : OogaTheme.TEXT_SECONDARY;
			int color = active ? ColorUtil.lerp(base, OogaTheme.GOLD, hv) : OogaTheme.TEXT_MUTED;
			icons[i].draw(g, ix, by, ICON, color);
		}
	}

	private static double mouseX() {
		Minecraft mc = Minecraft.getInstance();
		return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
	}

	private static double mouseY() {
		Minecraft mc = Minecraft.getInstance();
		return mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
	}

	/** Chat-screen click hook. @return true if a control consumed the click. */
	public boolean handleClick(double mouseX, double mouseY, int button) {
		if (button != 0 || !isVisible() || !module.controls.get() || MediaManager.get().current() == null) return false;
		for (int i = 0; i < 3; i++) {
			float[] box = buttons[i];
			if (mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1] && mouseY < box[1] + box[3]) {
				switch (i) {
					case 0 -> MediaManager.get().previous();
					case 1 -> MediaManager.get().playPause();
					default -> MediaManager.get().next();
				}
				return true;
			}
		}
		return false;
	}
}
