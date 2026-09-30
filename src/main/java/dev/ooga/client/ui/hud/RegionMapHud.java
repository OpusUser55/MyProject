package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.client.RegionMapModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.Finds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

/**
 * The region grid: each cell numbered, tinted by how far it is from the centre (a spectrum
 * from cool in the middle to warm at the edge), with your region highlighted and its number
 * in the header.
 */
public class RegionMapHud extends HudElement {
	private static final float CELL = 13f;
	private static final float GAP = 1.5f;
	private static final float PAD = 5f;
	private static final float HEADER = 13f;
	/** Centre-to-edge palette. */
	private static final int[] RING = {0xFF3E8FD6, 0xFF3AB7C9, 0xFF46C37B, 0xFF9BCB4A, 0xFFE2C13E, 0xFFE8903E, 0xFFE5604D, 0xFFC0507F};

	private final RegionMapModule module;

	public RegionMapHud(RegionMapModule module) {
		super("region_map", "Region Map", Anchor.END, 1f, Anchor.END, 0.78f);
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
		if (mc.player == null || mc.level == null) return;
		int n = module.gridSize.getInt();
		float grid = n * CELL + (n - 1) * GAP;
		float w = grid + PAD * 2;
		float h = grid + PAD * 2 + HEADER;
		float scale = module.scale.getFloat();
		width = w * scale;
		height = h * scale;

		boolean nether = module.inNether();
		int[] here = module.cellOf(mc.player.getX(), mc.player.getZ(), nether);
		Set<Integer> withFinds = new HashSet<>();
		if (module.showFinds.get()) {
			for (Finds.Find find : Finds.recent()) {
				boolean findNether = find.dimension().equals(Level.NETHER.identifier().toString());
				int[] cell = module.cellOf(find.pos().getX(), find.pos().getZ(), findNether);
				if (cell != null) withFinds.add(module.idOf(cell[0], cell[1]));
			}
		}

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		Render2D.roundRect(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, 0xE00E0F12);
		Render2D.outline(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, OogaTheme.BORDER);
		OogaFonts.draw(g, "REGION MAP", PAD, PAD, OogaTheme.TEXT, Weight.SEMIBOLD, 0.7f);
		String badge = here == null ? "#—" : "#" + module.idOf(here[0], here[1]);
		float bw = OogaFonts.width(badge, Weight.SEMIBOLD, 0.7f) + 6f;
		Render2D.roundRect(g, w - PAD - bw, PAD - 1.5f, bw, 9f, 2.5f, OogaTheme.accent(0x40));
		OogaFonts.draw(g, badge, w - PAD - bw + 3f, PAD, OogaTheme.GOLD_TEXT, Weight.SEMIBOLD, 0.7f);

		float top = PAD + HEADER;
		float mid = (n - 1) / 2f;
		for (int row = 0; row < n; row++) {
			for (int col = 0; col < n; col++) {
				int id = module.idOf(col, row);
				float cx = PAD + col * (CELL + GAP);
				float cy = top + row * (CELL + GAP);
				int ring = Math.round(Math.max(Math.abs(col - mid), Math.abs(row - mid)) / Math.max(1f, mid) * (RING.length - 1));
				boolean current = here != null && here[0] == col && here[1] == row;
				int base = RING[Math.min(RING.length - 1, ring)];
				int fill = current ? OogaTheme.GOLD : ColorUtil.withAlpha(base, module.visited(id) ? 235 : 150);
				Render2D.roundRect(g, cx, cy, CELL, CELL, 2f, fill);
				if (current) Render2D.outline(g, cx - 1f, cy - 1f, CELL + 2f, CELL + 2f, 3f, 0xFFFFFFFF);
				String label = Integer.toString(id);
				float scaleText = label.length() > 2 ? 0.42f : 0.5f;
				int text = current ? OogaTheme.ON_GOLD : 0xFFFFFFFF;
				OogaFonts.drawCentered(g, label, cx + CELL / 2f, cy + CELL / 2f - 2.3f, text, Weight.SEMIBOLD, scaleText);
				if (withFinds.contains(id)) Render2D.circle(g, cx + CELL - 2.5f, cy + 2.5f, 1.3f, 0xFFFFFFFF);
			}
		}
		g.pose().popMatrix();
	}
}
