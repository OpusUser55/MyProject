package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.TracerOrigin;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.ChunkScanner;
import dev.ooga.client.world.ChunkView;
import dev.ooga.client.world.Passable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Vertical 1x1 shafts: a column of air (or ladders) with solid blocks on all four sides.
 * Terrain almost never generates these, but players dig them straight down to get to (and
 * hide) underground bases.
 */
public class HoleEspModule extends Module implements ChunkScanner.Listener {
	private static final int COLOR = 0xE08A4F;

	public final NumberSetting minDepth = add(new NumberSetting("Min Depth", "Shortest shaft that counts.", 6, 3, 32, 1, "m"));
	public final NumberSetting minY = add(new NumberSetting("Min Y", "Ignore anything below this height.", -64, -64, 320, 1));
	public final NumberSetting maxY = add(new NumberSetting("Max Y", "Ignore anything above this height.", 320, -64, 320, 1));
	public final BooleanSetting tracers = add(new BooleanSetting("Tracers", "Lines from your view to each shaft.", false));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum highlight distance.", 160, 16, 512, 8, "m"));

	private final Map<Long, List<AABB>> found = new HashMap<>();
	private final ChunkView view = new ChunkView();
	/** Shaft cells in the chunk being scanned, grouped by column (x, z). */
	private Map<Long, List<Integer>> columns;
	private int total;

	public HoleEspModule() {
		super("Hole ESP", "Highlights 1x1 vertical shafts players dig down to hidden bases.", Category.BASEFINDING);
		ChunkScanner.register(this);
		WorldOverlay.register(this::draw);
	}

	@Override
	protected void onEnable() {
		clear();
		ChunkScanner.rescanLoaded();
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public Predicate<BlockState> filter() {
		return Passable::is;
	}

	@Override
	public boolean wantsSection(int sectionMinY) {
		return sectionMinY + 15 >= minY.getInt() && sectionMinY <= maxY.getInt();
	}

	@Override
	public void beginChunk(ChunkPos pos) {
		columns = new HashMap<>();
		view.begin(pos);
	}

	@Override
	public void block(int x, int y, int z, BlockState state) {
		if (y < minY.getInt() || y > maxY.getInt()) return;
		if (!view.solid(x - 1, y, z) || !view.solid(x + 1, y, z) || !view.solid(x, y, z - 1) || !view.solid(x, y, z + 1)) return;
		long column = ((long) x << 32) | (z & 0xFFFFFFFFL);
		columns.computeIfAbsent(column, k -> new ArrayList<>()).add(y);
	}

	@Override
	public void endChunk(ChunkPos pos) {
		List<AABB> shafts = new ArrayList<>();
		int min = minDepth.getInt();
		for (Map.Entry<Long, List<Integer>> entry : columns.entrySet()) {
			List<Integer> ys = entry.getValue();
			if (ys.size() < min) continue;
			int x = (int) (entry.getKey() >> 32);
			int z = (int) (long) entry.getKey();
			// Sections are scanned bottom-up, so each column's heights arrive in order.
			int start = ys.get(0), prev = start;
			for (int i = 1; i <= ys.size(); i++) {
				boolean more = i < ys.size();
				int next = more ? ys.get(i) : 0;
				if (more && next == prev + 1) {
					prev = next;
					continue;
				}
				if (prev - start + 1 >= min) shafts.add(new AABB(x, start, z, x + 1, prev + 1, z + 1));
				start = prev = next;
			}
		}
		if (shafts.isEmpty()) found.remove(pos.toLong());
		else found.put(pos.toLong(), shafts);
		columns = null;
		view.end();
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
		for (List<AABB> list : found.values()) n += list.size();
		total = n;
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null || found.isEmpty()) return;
		double maxSq = range.get() * range.get();
		Vec3 eye = mc.player.getEyePosition(partialTick);
		Vec3 origin = tracers.get() ? TracerOrigin.get(drawer, partialTick) : null;
		for (List<AABB> list : found.values()) {
			for (AABB box : list) {
				Vec3 center = box.getCenter();
				if (center.distanceToSqr(eye) > maxSq) continue;
				drawer.box(box, ColorUtil.withAlpha(COLOR, 45), ColorUtil.withAlpha(COLOR, 220));
				if (origin != null) drawer.line(origin, new Vec3(center.x, box.maxY, center.z), ColorUtil.withAlpha(COLOR, 150));
			}
		}
	}

	@Override
	public String getSuffix() {
		return Integer.toString(total);
	}
}
