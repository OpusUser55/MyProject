package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.render.WaypointsModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** Nearest waypoints with distance, styled like the Info HUD. */
public class WaypointHud extends HudElement {
	private static final float LINE = 11f;
	private static final float PAD = 5f;

	private final WaypointsModule module;

	public WaypointHud(WaypointsModule module) {
		super("waypoints", "Waypoints", Anchor.START, 0f, Anchor.START, 0.35f);
		this.module = module;
	}

	@Override
	public boolean isVisible() {
		return module.isEnabled() && module.hud.get() && Minecraft.getInstance().player != null && !module.visible().isEmpty();
	}

	@Override
	public NumberSetting scaleSetting() {
		return module.scale;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		Minecraft mc = Minecraft.getInstance();
		List<WaypointsModule.Shown> shown = module.visible();
		if (mc.player == null || shown.isEmpty()) {
			width = height = 0;
			return;
		}
		if (shown.size() > module.hudCount.getInt()) shown = shown.subList(0, module.hudCount.getInt());

		float scale = module.scale.getFloat();
		float nameW = 0, distW = 0;
		String[] dists = new String[shown.size()];
		for (int i = 0; i < shown.size(); i++) {
			WaypointsModule.Shown s = shown.get(i);
			dists[i] = (int) Math.sqrt(s.pos().distanceToSqr(mc.player.position())) + "m";
			nameW = Math.max(nameW, OogaFonts.width(s.waypoint().name(), Weight.REGULAR));
			distW = Math.max(distW, OogaFonts.width(dists[i], Weight.REGULAR, 0.8f));
		}
		float w = PAD + nameW + 8f + distW + PAD;
		float h = shown.size() * LINE + PAD * 2 - 2f;
		width = w * scale;
		height = h * scale;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		Render2D.roundRect(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, 0xD90E0F12);
		Render2D.outline(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, OogaTheme.BORDER);
		float ly = PAD;
		for (int i = 0; i < shown.size(); i++) {
			WaypointsModule.Shown s = shown.get(i);
			int color = s.converted() ? 0xFFC58CFF : 0xFF5CC8FF;
			OogaFonts.draw(g, s.waypoint().name(), PAD, ly, color, Weight.REGULAR);
			float dw = OogaFonts.width(dists[i], Weight.REGULAR, 0.8f);
			OogaFonts.draw(g, dists[i], w - PAD - dw, ly + 1f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.8f);
			ly += LINE;
		}
		g.pose().popMatrix();
	}
}
