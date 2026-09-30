package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.client.RegionMapModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.Level;

import java.util.Locale;

/** The region grid, with your region in gold and a marker at your exact position within the grid. */
public class RegionMapHud extends HudElement {
	private static final float CELL = 15f;
	private static final float PAD = 4f;

	private final RegionMapModule module;

	public RegionMapHud(RegionMapModule module) {
		super("regionmap", "Region Map", Anchor.START, 0f, Anchor.START, 0.45f);
		this.module = module;
	}

	@Override
	public boolean isVisible() {
		Minecraft mc = Minecraft.getInstance();
		if (!module.isEnabled() || mc.player == null || mc.level == null) return false;
		return !module.overworldOnly.get() || mc.level.dimension() == Level.OVERWORLD;
	}

	@Override
	public NumberSetting scaleSetting() {
		return module.scale;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		int[][] grid = RegionMapModule.REGIONS;
		int size = grid.length;
		float scale = module.scale.getFloat();
		float gridW = CELL * size;
		float infoH = module.coords.get() ? 22f : 0;
		float w = PAD * 2 + gridW;
		float h = PAD * 2 + gridW + infoH;
		width = w * scale;
		height = h * scale;

		double px = mc.player.getX(), pz = mc.player.getZ();
		int[] cell = module.cellAt(px, pz);

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		Render2D.roundRect(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, 0xD90E0F12);
		Render2D.outline(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, OogaTheme.BORDER);

		for (int row = 0; row < size; row++) {
			for (int col = 0; col < grid[row].length; col++) {
				float cx = PAD + col * CELL, cy = PAD + row * CELL;
				boolean here = row == cell[1] && col == cell[0];
				boolean spawn = row == size / 2 && col == size / 2;
				int fill = here ? OogaTheme.GOLD : spawn ? 0xFF2A2D35 : 0xFF1A1C22;
				Render2D.rect(g, cx + 0.5f, cy + 0.5f, CELL - 1f, CELL - 1f, fill);
				String label = Integer.toString(grid[row][col]);
				float tw = OogaFonts.width(label, Weight.SEMIBOLD, 0.55f);
				OogaFonts.draw(g, label, cx + (CELL - tw) / 2f, cy + 4.5f, here ? OogaTheme.ON_GOLD : OogaTheme.TEXT_SECONDARY, Weight.SEMIBOLD, 0.55f);
			}
		}

		// Exact position within the grid, when inside it.
		double sizeBlocks = module.regionSize.get();
		double fx = (px - module.offsetX.get()) / sizeBlocks + size / 2.0;
		double fz = (pz - module.offsetZ.get()) / sizeBlocks + size / 2.0;
		if (fx >= 0 && fx <= size && fz >= 0 && fz <= size) {
			float mx = PAD + (float) fx * CELL, my = PAD + (float) fz * CELL;
			Render2D.circle(g, mx, my, 2.2f, 0xFFE8594A);
			Render2D.circle(g, mx, my, 1.1f, 0xFFFFFFFF);
		}

		if (module.coords.get()) {
			float ty = PAD + gridW + 3f;
			String pos = String.format(Locale.ROOT, "X %,d  Z %,d", (long) px, (long) pz);
			int region = module.regionAt(px, pz);
			OogaFonts.draw(g, pos, PAD + 1f, ty, OogaTheme.TEXT, Weight.REGULAR, 0.75f);
			OogaFonts.draw(g, region == -1 ? "Outside the map" : "Region " + region, PAD + 1f, ty + 9f,
					region == -1 ? OogaTheme.TEXT_MUTED : OogaTheme.GOLD_TEXT, Weight.SEMIBOLD, 0.75f);
		}
		g.pose().popMatrix();
	}
}
