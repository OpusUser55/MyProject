package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.client.FindsHudModule;
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

import java.util.ArrayList;
import java.util.List;

/**
 * The last few base-finder hits in this dimension: what, where, how far, and an arrow that
 * points to it relative to where you're looking.
 */
public class FindsHud extends HudElement {
	private static final float LINE = 11f;
	private static final float PAD = 5f;
	private static final float ARROW = 8f;

	private final FindsHudModule module;

	public FindsHud(FindsHudModule module) {
		super("finds", "Finds", Anchor.START, 0f, Anchor.START, 0.45f);
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

	private record Row(String what, String where, String distance, float angle) {
	}

	private List<Row> rows() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		List<Row> rows = new ArrayList<>();
		if (player == null || mc.level == null) return rows;
		String dimension = mc.level.dimension().identifier().toString();
		int max = module.count.getInt();
		for (Finds.Find find : Finds.recent()) {
			if (!find.dimension().equals(dimension)) continue;
			double dx = find.pos().getX() + 0.5 - player.getX();
			double dz = find.pos().getZ() + 0.5 - player.getZ();
			double distance = Math.sqrt(dx * dx + dz * dz);
			// Bearing to the find relative to the view: 0 = straight ahead, clockwise.
			float bearing = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
			float angle = Mth.wrapDegrees(bearing - player.getYRot());
			String what = find.detail() == null || find.detail().isEmpty() ? find.type() : find.type() + " · " + find.detail();
			String where = find.pos().getX() + " " + find.pos().getY() + " " + find.pos().getZ();
			rows.add(new Row(what, where, Math.round(distance) + "m", angle));
			if (rows.size() >= max) break;
		}
		return rows;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		List<Row> rows = rows();
		float scale = module.scale.getFloat();
		String title = "FINDS";
		float titleScale = 0.62f;
		float w = OogaFonts.width(title, Weight.SEMIBOLD, titleScale);
		for (Row row : rows) {
			float lw = ARROW + 4f + OogaFonts.width(row.what(), Weight.SEMIBOLD) + 6f
					+ OogaFonts.width(row.where(), Weight.REGULAR, 0.8f) + 6f
					+ OogaFonts.width(row.distance(), Weight.REGULAR, 0.8f);
			w = Math.max(w, lw);
		}
		w += PAD * 2;
		float h = PAD * 2 + 8f + (rows.isEmpty() ? LINE : rows.size() * LINE) - 2f;
		width = w * scale;
		height = h * scale;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		Render2D.roundRect(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, 0xD90E0F12);
		Render2D.outline(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, OogaTheme.BORDER);
		OogaFonts.draw(g, title, PAD, PAD, OogaTheme.TEXT_MUTED, Weight.SEMIBOLD, titleScale);

		float ly = PAD + 8f;
		if (rows.isEmpty()) {
			OogaFonts.draw(g, "Nothing yet", PAD, ly, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.8f);
		}
		for (Row row : rows) {
			drawArrow(g, PAD + ARROW / 2f, ly + 4f, row.angle());
			float tx = PAD + ARROW + 4f;
			OogaFonts.draw(g, row.what(), tx, ly, OogaTheme.TEXT, Weight.SEMIBOLD);
			tx += OogaFonts.width(row.what(), Weight.SEMIBOLD) + 6f;
			OogaFonts.draw(g, row.where(), tx, ly + 1f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.8f);
			tx += OogaFonts.width(row.where(), Weight.REGULAR, 0.8f) + 6f;
			OogaFonts.draw(g, row.distance(), tx, ly + 1f, OogaTheme.GOLD_TEXT, Weight.REGULAR, 0.8f);
			ly += LINE;
		}
		g.pose().popMatrix();
	}

	/** A small triangle pointing toward the find; up means straight ahead. */
	private static void drawArrow(GuiGraphics g, float cx, float cy, float degrees) {
		float r = (float) Math.toRadians(degrees);
		float sin = Mth.sin(r), cos = Mth.cos(r);
		float[][] pts = {{0f, -3.5f}, {2.8f, 3f}, {-2.8f, 3f}};
		float[] out = new float[6];
		for (int i = 0; i < 3; i++) {
			float px = pts[i][0], py = pts[i][1];
			out[i * 2] = cx + px * cos - py * sin;
			out[i * 2 + 1] = cy + px * sin + py * cos;
		}
		Render2D.triangle(g, out[0], out[1], out[2], out[3], out[4], out[5], OogaTheme.GOLD);
	}
}
