package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.TracerOrigin;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.ChunkScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Ancient debris in loaded chunks. Only finds what the server actually sends: servers running
 * anti-xray replace hidden ores with fake blocks, and those can't be told apart client-side.
 */
public class DebrisFinderModule extends Module implements ChunkScanner.Listener {
	private static final int COLOR = 0xB5704F;

	public final BooleanSetting tracers = add(new BooleanSetting("Tracers", "Lines from your view to each block.", true));
	public final BooleanSetting chat = add(new BooleanSetting("Chat Alerts", "Post each new vein's coordinates in chat.", true));
	public final NumberSetting minY = add(new NumberSetting("Min Y", "Ignore debris below this height.", 8, 0, 128, 1));
	public final NumberSetting maxY = add(new NumberSetting("Max Y", "Ignore debris above this height.", 119, 0, 128, 1));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum highlight distance.", 128, 16, 512, 8, "m"));

	private final Map<Long, List<BlockPos>> found = new HashMap<>();
	private final Set<BlockPos> announced = new HashSet<>();
	private List<BlockPos> building;
	private int total;

	public DebrisFinderModule() {
		super("Debris Finder", "Highlights ancient debris in the Nether through netherrack.", Category.BASEFINDING);
		ChunkScanner.register(this);
		WorldOverlay.register(this::draw);
	}

	@Override
	protected void onEnable() {
		found.clear();
		announced.clear();
		ChunkScanner.rescanLoaded();
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public Predicate<BlockState> filter() {
		return state -> state.is(Blocks.ANCIENT_DEBRIS);
	}

	@Override
	public void beginChunk(ChunkPos pos) {
		building = new ArrayList<>();
	}

	@Override
	public void block(int x, int y, int z, BlockState state) {
		if (y >= minY.getInt() && y <= maxY.getInt()) building.add(new BlockPos(x, y, z));
	}

	@Override
	public void endChunk(ChunkPos pos) {
		if (building.isEmpty()) found.remove(pos.toLong());
		else {
			found.put(pos.toLong(), building);
			announce(building);
		}
		building = null;
		recount();
	}

	/** One chat line per vein: blocks touching an already-announced block don't repeat it. */
	private void announce(List<BlockPos> blocks) {
		if (!chat.get() || mc.player == null) return;
		for (BlockPos pos : blocks) {
			boolean known = announced.contains(pos);
			for (BlockPos other : announced) {
				if (known) break;
				known = other.distManhattan(pos) <= 2;
			}
			announced.add(pos);
			if (known) continue;
			int distance = (int) Math.sqrt(pos.distToCenterSqr(mc.player.position()));
			ChatUtil.info("Ancient debris at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " (" + distance + "m)");
		}
	}

	@Override
	public void forgetChunk(ChunkPos pos) {
		if (found.remove(pos.toLong()) != null) recount();
	}

	@Override
	public void clear() {
		found.clear();
		announced.clear();
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
		Vec3 eye = mc.player.getEyePosition(partialTick);
		Vec3 origin = TracerOrigin.get(drawer, partialTick);
		for (List<BlockPos> list : found.values()) {
			for (BlockPos pos : list) {
				if (pos.distToCenterSqr(eye) > maxSq) continue;
				// Chunks aren't rescanned on block changes, so skip debris that has been mined since.
				if (!mc.level.getBlockState(pos).is(Blocks.ANCIENT_DEBRIS)) continue;
				AABB box = new AABB(pos);
				drawer.box(box, ColorUtil.withAlpha(COLOR, 60), ColorUtil.withAlpha(COLOR, 235));
				if (tracers.get()) drawer.line(origin, box.getCenter(), ColorUtil.withAlpha(COLOR, 150));
			}
		}
	}

	@Override
	public String getSuffix() {
		return Integer.toString(total);
	}
}
