package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.TracerOrigin;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.ChunkScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/** Highlights chosen blocks (valuable ores, portals, beacons…) through walls. */
public class BlockEspModule extends Module implements ChunkScanner.Listener {
	private record Target(BooleanSupplier enabled, int rgb, Block... blocks) {
	}

	public final BooleanSetting diamonds = add(new BooleanSetting("Diamond Ore", "Diamond and deepslate diamond ore.", true));
	public final BooleanSetting debris = add(new BooleanSetting("Ancient Debris", "Ancient debris in the Nether.", true));
	public final BooleanSetting emeralds = add(new BooleanSetting("Emerald Ore", "Emerald and deepslate emerald ore.", false));
	public final BooleanSetting gold = add(new BooleanSetting("Gold Ore", "Gold ore of every kind.", false));
	public final BooleanSetting portals = add(new BooleanSetting("Nether Portals", "Lit nether portals: players build these.", true));
	public final BooleanSetting endFrames = add(new BooleanSetting("End Portal Frames", "Stronghold portal rooms.", false));
	public final BooleanSetting beacons = add(new BooleanSetting("Beacons", "Beacons.", true));
	public final BooleanSetting amethyst = add(new BooleanSetting("Budding Amethyst", "Budding amethyst in geodes.", false));
	public final ModeSetting style = add(new ModeSetting("Style", "How each block is drawn.", "Both", "Both", "Outline", "Fill"));
	public final BooleanSetting tracers = add(new BooleanSetting("Tracers", "Lines from your view to each block.", false));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance.", 96, 16, 256, 8, "m"));
	public final NumberSetting limit = add(new NumberSetting("Limit", "Most blocks drawn at once, nearest first.", 400, 50, 2000, 50));

	private final List<Target> targets = List.of(
			new Target(diamonds::get, 0x4FD8E8, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
			new Target(debris::get, 0xB0714F, Blocks.ANCIENT_DEBRIS),
			new Target(emeralds::get, 0x3ED67A, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE),
			new Target(gold::get, 0xF2C94C, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE),
			new Target(portals::get, 0xA05CF0, Blocks.NETHER_PORTAL),
			new Target(endFrames::get, 0x6FD3B0, Blocks.END_PORTAL_FRAME),
			new Target(beacons::get, 0x8FF3FF, Blocks.BEACON),
			new Target(amethyst::get, 0xC58AF0, Blocks.BUDDING_AMETHYST));

	/** Matching blocks per chunk, each packed with its colour. */
	private final Map<Long, List<long[]>> found = new HashMap<>();
	private final Map<Block, Target> byBlock = new HashMap<>();
	private List<long[]> building;
	private int shown;

	public BlockEspModule() {
		super("Block ESP", "Ores, portals and beacons through walls.", Category.RENDER);
		for (Target target : targets) for (Block block : target.blocks()) byBlock.put(block, target);
		for (BooleanSetting toggle : List.of(diamonds, debris, emeralds, gold, portals, endFrames, beacons, amethyst)) {
			toggle.onChange(this::rescan);
		}
		ChunkScanner.register(this);
		WorldOverlay.register(this::draw);
	}

	private void rescan() {
		if (!isEnabled()) return;
		found.clear();
		ChunkScanner.rescanLoaded();
	}

	@Override
	protected void onEnable() {
		found.clear();
		ChunkScanner.rescanLoaded();
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public Predicate<BlockState> filter() {
		return state -> {
			Target target = byBlock.get(state.getBlock());
			return target != null && target.enabled().getAsBoolean();
		};
	}

	@Override
	public void beginChunk(ChunkPos pos) {
		building = new ArrayList<>();
	}

	@Override
	public void block(int x, int y, int z, BlockState state) {
		building.add(new long[]{BlockPos.asLong(x, y, z), byBlock.get(state.getBlock()).rgb()});
	}

	@Override
	public void endChunk(ChunkPos pos) {
		if (building.isEmpty()) found.remove(pos.toLong());
		else found.put(pos.toLong(), building);
		building = null;
	}

	@Override
	public void forgetChunk(ChunkPos pos) {
		found.remove(pos.toLong());
	}

	@Override
	public void clear() {
		found.clear();
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null || found.isEmpty()) return;
		double maxSq = range.get() * range.get();
		Vec3 eye = mc.player.getEyePosition(partialTick);
		List<long[]> near = new ArrayList<>();
		for (List<long[]> list : found.values()) {
			for (long[] entry : list) {
				long p = entry[0];
				double dx = BlockPos.getX(p) + 0.5 - eye.x, dy = BlockPos.getY(p) + 0.5 - eye.y, dz = BlockPos.getZ(p) + 0.5 - eye.z;
				double d = dx * dx + dy * dy + dz * dz;
				if (d <= maxSq) near.add(new long[]{p, entry[1], Double.doubleToLongBits(d)});
			}
		}
		int max = limit.getInt();
		if (near.size() > max) {
			near.sort((a, b) -> Double.compare(Double.longBitsToDouble(a[2]), Double.longBitsToDouble(b[2])));
			near = near.subList(0, max);
		}
		Vec3 origin = tracers.get() ? TracerOrigin.get(drawer, partialTick) : null;
		for (long[] entry : near) {
			BlockPos pos = BlockPos.of(entry[0]);
			int rgb = (int) entry[1];
			AABB box = new AABB(pos);
			int fill = style.is("Outline") ? 0 : ColorUtil.withAlpha(rgb, 45);
			int line = style.is("Fill") ? 0 : ColorUtil.withAlpha(rgb, 220);
			drawer.box(box, fill, line);
			if (origin != null) drawer.line(origin, box.getCenter(), ColorUtil.withAlpha(rgb, 150));
		}
		shown = near.size();
	}

	@Override
	public String getSuffix() {
		return Integer.toString(shown);
	}
}
