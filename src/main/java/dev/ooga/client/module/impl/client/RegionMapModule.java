package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.RegionMapHud;

/**
 * A grid of the server's numbered map regions with your current one highlighted, like the
 * region maps DonutSMP players use to pick RTP areas. The layout is DonutSMP's 9×9 grid;
 * region size and the centre offset are settings in case they differ from the defaults.
 */
public class RegionMapModule extends Module {
	/** Region numbers, north (negative Z) at the top, west (negative X) on the left. Spawn is the centre cell. */
	public static final int[][] REGIONS = {
			{82, 100, 101, 102, 103, 104, 105, 106, 91},
			{83, 44, 75, 42, 41, 40, 39, 38, 92},
			{84, 45, 14, 13, 12, 11, 10, 37, 93},
			{85, 46, 74, 3, 2, 1, 25, 36, 94},
			{86, 47, 72, 71, 5, 4, 24, 35, 95},
			{87, 51, 17, 9, 8, 7, 23, 34, 96},
			{88, 54, 18, 61, 62, 21, 22, 33, 97},
			{89, 26, 27, 28, 29, 30, 59, 32, 98},
			{90, 107, 108, 109, 110, 111, 112, 113, 99},
	};

	public final NumberSetting regionSize = add(new NumberSetting("Region Size", "Width of one region in blocks.", 50000, 1000, 200000, 1000, "m"));
	public final NumberSetting offsetX = add(new NumberSetting("Center X", "X coordinate at the middle of the centre region.", 0, -100000, 100000, 500));
	public final NumberSetting offsetZ = add(new NumberSetting("Center Z", "Z coordinate at the middle of the centre region.", 0, -100000, 100000, 500));
	public final BooleanSetting coords = add(new BooleanSetting("Coordinates", "Show your position and region number under the map.", true));
	public final BooleanSetting overworldOnly = add(new BooleanSetting("Overworld Only", "Hide the map in the Nether and End.", true));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public RegionMapModule() {
		super("Region Map", "Numbered server regions with your position.", Category.HUD);
		hideFromList();
		HudManager.get().register(new RegionMapHud(this));
	}

	/** {col, row} of the region containing (x, z); may be outside the grid. */
	public int[] cellAt(double x, double z) {
		double size = regionSize.get();
		int center = REGIONS.length / 2;
		int col = center + (int) Math.floor((x - offsetX.get()) / size + 0.5);
		int row = center + (int) Math.floor((z - offsetZ.get()) / size + 0.5);
		return new int[]{col, row};
	}

	/** Region number at (x, z), or -1 outside the grid. */
	public int regionAt(double x, double z) {
		int[] cell = cellAt(x, z);
		if (cell[1] < 0 || cell[1] >= REGIONS.length || cell[0] < 0 || cell[0] >= REGIONS[cell[1]].length) return -1;
		return REGIONS[cell[1]][cell[0]];
	}
}
