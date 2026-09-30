package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.TracerOrigin;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.BlockUpdates;
import dev.ooga.client.world.ChunkScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Ancient debris the client knows about, grouped into veins.
 *
 * <p>Two sources feed it: the initial block data of every loaded chunk ({@link ChunkScanner})
 * and every block change the server sends afterwards ({@link BlockUpdates}). The second one
 * matters on anti-xray servers, which hide buried ores in chunk data but send the real block
 * once it's exposed by mining or an explosion; those show up here the moment they arrive.
 * Nothing the server never sends can be found.
 */
public class DebrisFinderModule extends Module implements ChunkScanner.Listener, BlockUpdates.Listener {
	private static final int COLOR = 0xB5704F;
	/** Blocks this close on every axis (Chebyshev distance) belong to the same vein. */
	private static final int VEIN_GAP = 2;

	public final BooleanSetting groupVeins = add(new BooleanSetting("Group Veins", "One box around each vein instead of one per block.", true)
			.onChange(() -> this.dirty = true));
	public final BooleanSetting tracers = add(new BooleanSetting("Tracers", "Lines from your view to each vein.", true));
	public final BooleanSetting chat = add(new BooleanSetting("Chat Alerts", "Post each new vein's coordinates in chat.", true));
	public final BooleanSetting revealedOnly = add(new BooleanSetting("Revealed Only",
			"Only trust debris the server sent as a block update (after mining or TNT). Use on servers whose anti-xray sends fake ores.", false)
			.onChange(() -> this.dirty = true));
	public final NumberSetting maxVeins = add(new NumberSetting("Max Veins", "Show only this many veins, nearest first.", 50, 1, 500, 1)
			.onChange(() -> this.dirty = true));
	public final NumberSetting minY = add(new NumberSetting("Min Y", "Ignore debris below this height.", 8, 0, 128, 1));
	public final NumberSetting maxY = add(new NumberSetting("Max Y", "Ignore debris above this height.", 119, 0, 128, 1));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum highlight distance.", 128, 16, 512, 8, "m"));

	private record Vein(AABB box, int blocks) {
	}

	/** From chunk data, per chunk so unloading a chunk drops its entries. */
	private final Map<Long, Set<BlockPos>> scanned = new HashMap<>();
	/** From block updates; survives rescans, since chunk data would re-hide them on anti-xray servers. */
	private final Map<Long, Set<BlockPos>> revealed = new HashMap<>();
	private final Set<BlockPos> announced = new HashSet<>();
	private Set<BlockPos> building;
	private List<Vein> veins = List.of();
	private boolean dirty;
	private int total;

	public DebrisFinderModule() {
		super("Debris Finder", "Highlights ancient debris in the Nether through netherrack.", Category.BASEFINDING);
		ChunkScanner.register(this);
		BlockUpdates.register(this);
		WorldOverlay.register(this::draw);
	}

	@Override
	protected void onEnable() {
		clear();
		ChunkScanner.rescanLoaded();
	}

	private boolean inRange(int y) {
		return y >= minY.getInt() && y <= maxY.getInt();
	}

	// ------------------------------------------------------------------ chunk data

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
		building = new HashSet<>();
	}

	@Override
	public void block(int x, int y, int z, BlockState state) {
		if (inRange(y)) building.add(new BlockPos(x, y, z));
	}

	@Override
	public void endChunk(ChunkPos pos) {
		if (building.isEmpty()) scanned.remove(pos.toLong());
		else scanned.put(pos.toLong(), building);
		building = null;
		dirty = true;
	}

	@Override
	public void forgetChunk(ChunkPos pos) {
		boolean changed = scanned.remove(pos.toLong()) != null;
		changed |= revealed.remove(pos.toLong()) != null;
		if (changed) dirty = true;
	}

	@Override
	public void clear() {
		scanned.clear();
		revealed.clear();
		announced.clear();
		veins = List.of();
		total = 0;
		dirty = false;
	}

	// ------------------------------------------------------------------ live updates

	@Override
	public void blockChanged(BlockPos pos, BlockState state) {
		if (!isEnabled()) return;
		long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
		if (state.is(Blocks.ANCIENT_DEBRIS) && inRange(pos.getY())) {
			if (revealed.computeIfAbsent(chunk, k -> new HashSet<>()).add(pos)) dirty = true;
			return;
		}
		// Mined, blown up, or turned out to be a fake: forget it everywhere.
		Set<BlockPos> fromScan = scanned.get(chunk);
		if (fromScan != null && fromScan.remove(pos)) dirty = true;
		Set<BlockPos> fromUpdate = revealed.get(chunk);
		if (fromUpdate != null && fromUpdate.remove(pos)) dirty = true;
	}

	// ------------------------------------------------------------------ veins

	@Override
	public void onTick() {
		if (!dirty || mc.player == null) return;
		dirty = false;
		rebuildVeins();
	}

	private void rebuildVeins() {
		Set<BlockPos> all = new HashSet<>();
		if (!revealedOnly.get()) for (Set<BlockPos> set : scanned.values()) all.addAll(set);
		for (Set<BlockPos> set : revealed.values()) all.addAll(set);
		total = all.size();

		List<Vein> result = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		for (BlockPos start : all) {
			if (!visited.add(start)) continue;
			// Flood fill through neighbours up to VEIN_GAP away; debris veins are tiny, so this is cheap.
			List<BlockPos> members = new ArrayList<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(start);
			while (!queue.isEmpty()) {
				BlockPos pos = queue.poll();
				members.add(pos);
				for (BlockPos near : BlockPos.betweenClosed(pos.offset(-VEIN_GAP, -VEIN_GAP, -VEIN_GAP), pos.offset(VEIN_GAP, VEIN_GAP, VEIN_GAP))) {
					if (all.contains(near) && visited.add(near.immutable())) queue.add(near.immutable());
				}
			}
			announce(members);
			if (groupVeins.get()) result.add(new Vein(bounds(members), members.size()));
			else for (BlockPos pos : members) result.add(new Vein(new AABB(pos), 1));
		}

		Vec3 me = mc.player.position();
		result.sort(Comparator.comparingDouble(v -> v.box().getCenter().distanceToSqr(me)));
		veins = result.size() > maxVeins.getInt() ? List.copyOf(result.subList(0, maxVeins.getInt())) : result;
	}

	private static AABB bounds(List<BlockPos> blocks) {
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for (BlockPos p : blocks) {
			minX = Math.min(minX, p.getX());
			minY = Math.min(minY, p.getY());
			minZ = Math.min(minZ, p.getZ());
			maxX = Math.max(maxX, p.getX());
			maxY = Math.max(maxY, p.getY());
			maxZ = Math.max(maxZ, p.getZ());
		}
		return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
	}

	/** One chat line per vein, the first time any of its blocks is seen. */
	private void announce(List<BlockPos> vein) {
		boolean known = false;
		for (BlockPos pos : vein) known |= announced.contains(pos);
		announced.addAll(vein);
		if (known || !chat.get() || mc.player == null) return;
		BlockPos pos = vein.get(0);
		int distance = (int) Math.sqrt(pos.distToCenterSqr(mc.player.position()));
		String size = vein.size() == 1 ? "" : " ×" + vein.size();
		ChatUtil.info("Ancient debris" + size + " at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " (" + distance + "m)");
	}

	// ------------------------------------------------------------------ rendering

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null) return;
		double maxSq = range.get() * range.get();
		Vec3 eye = mc.player.getEyePosition(partialTick);
		Vec3 origin = TracerOrigin.get(drawer, partialTick);
		for (Vein vein : veins) {
			Vec3 center = vein.box().getCenter();
			if (center.distanceToSqr(eye) > maxSq) continue;
			drawer.box(vein.box(), ColorUtil.withAlpha(COLOR, 60), ColorUtil.withAlpha(COLOR, 235));
			if (tracers.get()) drawer.line(origin, center, ColorUtil.withAlpha(COLOR, 150));
		}
	}

	@Override
	public String getSuffix() {
		return Integer.toString(total);
	}
}
