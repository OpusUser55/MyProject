package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.client.KeystrokesModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.Anim;
import dev.ooga.client.util.ClickTracker;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.Map;

public class KeystrokesHud extends HudElement {
	private static final float KEY = 18f;
	private static final float GAP = 2f;

	private final KeystrokesModule module;
	private final Map<String, Anim> press = new HashMap<>();

	public KeystrokesHud(KeystrokesModule module) {
		super("keystrokes", "Keystrokes", 0.006f, 0.72f);
		this.module = module;
	}

	@Override
	public boolean isVisible() {
		return module.isEnabled();
	}

	@Override
	public NumberSetting scaleSetting() {
		return module.scale;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		Options o = Minecraft.getInstance().options;
		float scale = module.scale.getFloat();
		float full = KEY * 3 + GAP * 2;
		float h = KEY * 2 + GAP;
		if (module.mouse.get()) h += GAP + KEY;
		if (module.space.get()) h += GAP + 9f;
		width = full * scale;
		height = h * scale;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);

		key(g, "W", o.keyUp, KEY + GAP, 0, KEY, KEY, null);
		key(g, "A", o.keyLeft, 0, KEY + GAP, KEY, KEY, null);
		key(g, "S", o.keyDown, KEY + GAP, KEY + GAP, KEY, KEY, null);
		key(g, "D", o.keyRight, (KEY + GAP) * 2, KEY + GAP, KEY, KEY, null);
		float rowY = (KEY + GAP) * 2;
		if (module.mouse.get()) {
			float half = (full - GAP) / 2f;
			key(g, "LMB", o.keyAttack, 0, rowY, half, KEY, ClickTracker.leftCps() + " CPS");
			key(g, "RMB", o.keyUse, half + GAP, rowY, half, KEY, ClickTracker.rightCps() + " CPS");
			rowY += KEY + GAP;
		}
		if (module.space.get()) {
			float t = anim("space", o.keyJump);
			drawKeyBase(g, 0, rowY, full, 9f, t);
			float barW = full * 0.4f;
			Render2D.roundRect(g, (full - barW) / 2f, rowY + 4f, barW, 1.2f, 0.6f,
					ColorUtil.lerp(OogaTheme.TEXT_MUTED, OogaTheme.ON_GOLD, t));
		}
		g.pose().popMatrix();
	}

	private float anim(String id, KeyMapping mapping) {
		// Fast attack, slower release: presses register instantly, releases fade out.
		Anim a = press.computeIfAbsent(id, k -> new Anim(0f, 22f));
		if (mapping.isDown()) {
			a.snap(1f);
			return 1f;
		}
		return a.update(0f);
	}

	private void drawKeyBase(GuiGraphics g, float x, float y, float w, float h, float t) {
		if (t > 0.01f) GlowRenderer.glow(g, x, y, w, h, OogaTheme.RADIUS_CONTROL + 1, OogaTheme.GOLD, 0.6f * t, 3f);
		Render2D.roundRect(g, x, y, w, h, OogaTheme.RADIUS_CONTROL + 1, ColorUtil.lerp(0xCC0E0F12, OogaTheme.GOLD, t));
		Render2D.outline(g, x, y, w, h, OogaTheme.RADIUS_CONTROL + 1, ColorUtil.lerp(OogaTheme.BORDER, 0x00000000, t));
	}

	private void key(GuiGraphics g, String label, KeyMapping mapping, float x, float y, float w, float h, String sub) {
		float t = anim(label, mapping);
		drawKeyBase(g, x, y, w, h, t);
		int color = ColorUtil.lerp(OogaTheme.TEXT, OogaTheme.ON_GOLD, t);
		if (sub == null) {
			OogaFonts.drawCentered(g, label, x + w / 2f, y + h / 2f - 4f, color, Weight.SEMIBOLD, 1f);
		} else {
			OogaFonts.drawCentered(g, label, x + w / 2f, y + 3f, color, Weight.SEMIBOLD, 0.8f);
			OogaFonts.drawCentered(g, sub, x + w / 2f, y + 11f, ColorUtil.lerp(OogaTheme.TEXT_MUTED, OogaTheme.ON_GOLD, t), Weight.REGULAR, 0.6f);
		}
	}
}
