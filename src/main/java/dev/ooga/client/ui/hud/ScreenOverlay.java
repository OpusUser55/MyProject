package dev.ooga.client.ui.hud;

import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.misc.VisualRangeModule;
import dev.ooga.client.module.impl.render.WaypointsModule;
import dev.ooga.client.render.Projection;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/**
 * Full-screen, non-movable layer for things tied to the world or the whole screen: waypoint
 * labels and the Visual Range arrival flash.
 */
public class ScreenOverlay extends HudElement {
	public ScreenOverlay() {
		super("overlay", "Overlay", Anchor.START, 0f, Anchor.START, 0f);
	}

	@Override
	public boolean isVisible() {
		return Minecraft.getInstance().player != null;
	}

	@Override
	public boolean isMovable() {
		return false;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		width = height = 0;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		ModuleManager modules = ModuleManager.get();
		drawWaypointLabels(g, mc, modules.get(WaypointsModule.class));
		drawFlash(g, mc, modules.get(VisualRangeModule.class).flashAlpha());
	}

	private void drawWaypointLabels(GuiGraphics g, Minecraft mc, WaypointsModule waypoints) {
		if (!waypoints.isEnabled() || !waypoints.labels.get()) return;
		float scale = waypoints.labelScale.getFloat();
		for (WaypointsModule.Shown s : waypoints.visible()) {
			float[] screen = Projection.toScreen(s.pos().add(0, 1.6, 0));
			if (screen == null) continue;
			int distance = (int) Math.sqrt(s.pos().distanceToSqr(mc.player.position()));
			String name = s.waypoint().name();
			String dist = distance + "m";
			float nw = OogaFonts.width(name, Weight.SEMIBOLD) * scale;
			float dw = OogaFonts.width(dist, Weight.REGULAR, 0.8f) * scale;
			float w = nw + 5f * scale + dw + 8f * scale;
			float h = 12f * scale;
			float lx = screen[0] - w / 2f, ly = screen[1] - h;
			int accent = s.converted() ? 0xFFC58CFF : 0xFF5CC8FF;
			Render2D.roundRect(g, lx, ly, w, h, OogaTheme.RADIUS_CONTROL, 0xC80E0F12);
			Render2D.outline(g, lx, ly, w, h, OogaTheme.RADIUS_CONTROL, ColorUtil.withAlpha(accent, 0x90));
			OogaFonts.draw(g, name, lx + 4f * scale, ly + 2f * scale, accent, Weight.SEMIBOLD, scale);
			OogaFonts.draw(g, dist, lx + 4f * scale + nw + 5f * scale, ly + 2.8f * scale, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.8f * scale);
		}
	}

	private void drawFlash(GuiGraphics g, Minecraft mc, float alpha) {
		if (alpha <= 0.01f) return;
		float w = mc.getWindow().getGuiScaledWidth();
		float h = mc.getWindow().getGuiScaledHeight();
		// Soft red edges, fading inward and out over time.
		for (int i = 0; i < 6; i++) {
			int color = ColorUtil.withAlpha(0xE8594A, Math.round(alpha * 110 * (1f - i / 6f)));
			float t = i * 2f;
			Render2D.rect(g, t, t, w - 2 * t, 2f, color);
			Render2D.rect(g, t, h - t - 2f, w - 2 * t, 2f, color);
			Render2D.rect(g, t, t + 2f, 2f, h - 2 * t - 4f, color);
			Render2D.rect(g, w - t - 2f, t + 2f, 2f, h - 2 * t - 4f, color);
		}
	}
}
