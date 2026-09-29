package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.ChunkScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Player light sources underground. Caves and most structures generate no torches or
 * lanterns, so a torch deep underground almost always means someone has been there.
 */
public class LightFinderModule extends Module implements ChunkScanner.Listener {
	private static final int COLOR = 0xFFD36B;
	private static final Set<Block> LIGHTS = Set.of(
			Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
			Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH,
			Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.JACK_O_LANTERN, Blocks.GLOWSTONE, Blocks.SEA_LANTERN);

	public final NumberSetting maxY = add(new NumberSetting("Max Y", "Only report lights below this height.", 30, -64, 320, 1));
	public final BooleanSetting glowstone = add(new BooleanSetting("Glowstone & Sea Lanterns", "Also count placed light blocks (these generate naturally in some places).", false));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum highlight distance.", 160, 16, 512, 8, "m"));

	private final Map<Long, List<BlockPos>> found = new HashMap<>();
	private List<BlockPos> building;
	private int total;

	public LightFinderModule() {
		super("Light Finder", "Torches and lanterns deep underground, where caves have none.", Category.BASEFINDING);
		ChunkScanner.register(this);
		WorldOverlay.register(this::draw);
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
			Block block = state.getBlock();
			if (!LIGHTS.contains(block)) return false;
			return glowstone.get() || (block != Blocks.GLOWSTONE && block != Blocks.SEA_LANTERN);
		};
	}

	@Override
	public void beginChunk(ChunkPos pos) {
		building = new ArrayList<>();
	}

	@Override
	public void block(int x, int y, int z, BlockState state) {
		if (y < maxY.getInt()) building.add(new BlockPos(x, y, z));
	}

	@Override
	public void endChunk(ChunkPos pos) {
		if (building.isEmpty()) found.remove(pos.toLong());
		else found.put(pos.toLong(), building);
		building = null;
		recount();
	}

	@Override
	public void forgetChunk(ChunkPos pos) {
		if (found.remove(pos.toLong()) != null) recount();
	}

	@Override
	public void clear() {
		found.clear();
		total = 0;
	}

	private void recount() {
		int n = 0;
		for (List<BlockPos> list : found.values()) n += list.size();
		total = n;
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null) return;
		double maxSq = range.get() * range.get();
		var eye = mc.player.getEyePosition(partialTick);
		for (List<BlockPos> list : found.values()) {
			for (BlockPos pos : list) {
				if (pos.distToCenterSqr(eye) > maxSq) continue;
				AABB box = new AABB(pos).deflate(0.3);
				drawer.box(box, ColorUtil.withAlpha(COLOR, 60), ColorUtil.withAlpha(COLOR, 230));
			}
		}
	}

	@Override
	public String getSuffix() {
		return Integer.toString(total);
	}
}
