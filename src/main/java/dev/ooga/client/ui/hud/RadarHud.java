package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.client.RadarModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.world.Finds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * A round minimap that turns with you: straight ahead is always up. Players are gold dots,
 * hostiles red, and base-finder hits small diamonds coloured by kind.
 */
public class RadarHud extends HudElement {
	private static final float RADIUS = 34f;
	private static final int HOSTILE = 0xFFE5484D;

	private final RadarModule module;

	public RadarHud(RadarModule module) {
		super("radar", "Radar", Anchor.START, 0f, Anchor.START, 0.3f);
		this.module = module;
	}

	@Override
	public boolean isVisible() {
		return module.isEnabled() && Minecraft.getInstance().player != null;
	}

	@Override
	public NumberSetting scaleSetting() {
		return module.scale;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		float scale = module.scale.getFloat();
		float size = RADIUS * 2;
		width = size * scale;
		height = size * scale;
		if (player == null || mc.level == null) return;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		float c = RADIUS;
		Render2D.circle(g, c, c, RADIUS, OogaTheme.BORDER_STRONG);
		Render2D.circle(g, c, c, RADIUS - 1f, 0xD90E0F12);
		Render2D.circle(g, c, c, RADIUS * 0.5f, 0x0DFFFFFF);
		Render2D.circle(g, c, c, RADIUS * 0.5f - 1f, 0xD90E0F12);
		Render2D.rect(g, c - 0.5f, 3f, 1f, size - 6f, OogaTheme.DIVIDER);
		Render2D.rect(g, 3f, c - 0.5f, size - 6f, 1f, OogaTheme.DIVIDER);

		double px = player.getX(), pz = player.getZ();
		float yaw = player.getYRot() * Mth.DEG_TO_RAD;
		// Forward and right in world (x, z) for the current yaw.
		float fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
		float rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);
		float perBlock = (RADIUS - 3f) / module.range.getFloat();

		// North marker on the rim.
		float nx = -rz * (RADIUS - 5f), ny = fz * (RADIUS - 5f);
		OogaFonts.drawCentered(g, "N", c + nx, c + ny - 3f, OogaTheme.TEXT_SECONDARY, Weight.SEMIBOLD, 0.6f);

		if (module.finds.get()) {
			String dimension = mc.level.dimension().identifier().toString();
			for (Finds.Find find : Finds.recent()) {
				if (!find.dimension().equals(dimension)) continue;
				float[] p = project(find.pos().getX() + 0.5 - px, find.pos().getZ() + 0.5 - pz, fx, fz, rx, rz, perBlock);
				if (p == null) continue;
				Render2D.diamond(g, c + p[0], c + p[1], 2.2f, colorOf(find.type()));
			}
		}
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity == player) continue;
			boolean isPlayer = entity instanceof Player;
			if (isPlayer ? !module.players.get() : !(module.hostiles.get() && entity instanceof Enemy)) continue;
			float[] p = project(entity.getX() - px, entity.getZ() - pz, fx, fz, rx, rz, perBlock);
			if (p == null) continue;
			Render2D.circle(g, c + p[0], c + p[1], isPlayer ? 1.8f : 1.4f, isPlayer ? OogaTheme.GOLD : HOSTILE);
		}
		// You: a small arrow pointing up.
		Render2D.triangle(g, c, c - 3.5f, c + 2.6f, c + 2.5f, c - 2.6f, c + 2.5f, OogaTheme.TEXT);
		g.pose().popMatrix();
	}

	/** Radar-space offset for a world offset, or null if it falls outside the rim. */
	private static float[] project(double dx, double dz, float fx, float fz, float rx, float rz, float perBlock) {
		float sx = (float) (dx * rx + dz * rz) * perBlock;
		float sy = (float) -(dx * fx + dz * fz) * perBlock;
		float limit = RADIUS - 3f;
		return sx * sx + sy * sy > limit * limit ? null : new float[]{sx, sy};
	}

	private static int colorOf(String type) {
		return switch (type) {
			case "Spawner" -> 0xFFE8594A;
			case "Tunnel" -> 0xFF5FB3F0;
			case "Stash" -> 0xFFD9A441;
			default -> OogaTheme.GOLD;
		};
	}
}
