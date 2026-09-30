package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.RegionMapHud;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

/**
 * Splits the world into a square grid of numbered regions around a centre point and shows
 * which one you're in, which you've visited this session, and where your finds are.
 * Numbers run left to right, top (north) to bottom, starting at 1.
 */
public class RegionMapModule extends Module {
	public final NumberSetting gridSize = add(new NumberSetting("Grid Size", "Regions per side.", 9, 3, 15, 1));
	public final NumberSetting regionSize = add(new NumberSetting("Region Size", "Width of one region in blocks.", 10000, 500, 100000, 500, "m"));
	public final NumberSetting centerX = add(new NumberSetting("Center X", "X coordinate the grid is centred on.", 0, -1000000, 1000000, 500));
	public final NumberSetting centerZ = add(new NumberSetting("Center Z", "Z coordinate the grid is centred on.", 0, -1000000, 1000000, 500));
	public final BooleanSetting netherScale = add(new BooleanSetting("Nether Scale", "In the Nether, use overworld coordinates (x8) so regions match.", true));
	public final BooleanSetting showFinds = add(new BooleanSetting("Show Finds", "Mark regions where base finders turned something up.", true));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Map size.", 1.0, 0.5, 2.0, 0.05, "x"));

	private final Set<Integer> visited = new HashSet<>();

	public RegionMapModule() {
		super("Region Map", "Numbered grid of world regions, highlighting the one you're in.", Category.HUD);
		hideFromList();
		HudManager.get().register(new RegionMapHud(this));
	}

	/** Grid cell (column, row) for world coordinates, or null outside the grid. */
	public int[] cellOf(double x, double z, boolean nether) {
		double factor = nether && netherScale.get() ? 8 : 1;
		int n = gridSize.getInt();
		double size = regionSize.get();
		double half = n * size / 2.0;
		int col = (int) Math.floor((x * factor - centerX.get() + half) / size);
		int row = (int) Math.floor((z * factor - centerZ.get() + half) / size);
		if (col < 0 || row < 0 || col >= n || row >= n) return null;
		return new int[]{col, row};
	}

	public int idOf(int col, int row) {
		return row * gridSize.getInt() + col + 1;
	}

	public boolean inNether() {
		return mc.level != null && mc.level.dimension() == Level.NETHER;
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		int[] cell = cellOf(mc.player.getX(), mc.player.getZ(), inNether());
		if (cell != null) visited.add(idOf(cell[0], cell[1]));
	}

	public boolean visited(int id) {
		return visited.contains(id);
	}
}
