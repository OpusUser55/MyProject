package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.client.FriendsModule;
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

		// Compass letters on the rim, turning with you.
		String[] letters = {"N", "E", "S", "W"};
		int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
		for (int i = 0; i < 4; i++) {
			float lx = (dirs[i][0] * rx + dirs[i][1] * rz) * (RADIUS - 6f);
			float ly = -(dirs[i][0] * fx + dirs[i][1] * fz) * (RADIUS - 6f);
			int color = i == 0 ? OogaTheme.GOLD_TEXT : OogaTheme.TEXT_SECONDARY;
			OogaFonts.drawCentered(g, letters[i], c + lx, c + ly - 3f, color, Weight.SEMIBOLD, 0.6f);
		}

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
			double dx = entity.getX() - px, dz = entity.getZ() - pz;
			float[] p = project(dx, dz, fx, fz, rx, rz, perBlock);
			if (p == null) continue;
			float dotX = c + p[0], dotY = c + p[1];
			if (!isPlayer) {
				Render2D.circle(g, dotX, dotY, 1.4f, HOSTILE);
				continue;
			}
			int dot = FriendsModule.highlights(entity) ? FriendsModule.COLOR : dotColor();
			Render2D.circle(g, dotX, dotY, 3.2f, dot & 0x40FFFFFF);
			Render2D.circle(g, dotX, dotY, 2f, dot);
			String label = module.names.get() ? entity.getName().getString()
					: module.distances.get() ? Math.round(Math.sqrt(dx * dx + dz * dz)) + "M" : null;
			if (label != null) {
				float lw = OogaFonts.width(label, Weight.SEMIBOLD, 0.5f) + 4f;
				Render2D.roundRect(g, dotX + 3f, dotY - 3f, lw, 6f, 2f, 0xE00E0F12);
				OogaFonts.draw(g, label, dotX + 5f, dotY - 2.2f, OogaTheme.TEXT, Weight.SEMIBOLD, 0.5f);
			}
		}
		// You: a small arrow pointing up.
		Render2D.triangle(g, c, c - 3.5f, c + 2.6f, c + 2.5f, c - 2.6f, c + 2.5f, OogaTheme.TEXT);
		g.pose().popMatrix();
	}

	private int dotColor() {
		return switch (module.dotColor.get()) {
			case "Accent" -> OogaTheme.GOLD;
			case "Red" -> HOSTILE;
			case "White" -> 0xFFEDEDF0;
			default -> 0xFFF0508C;
		};
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
