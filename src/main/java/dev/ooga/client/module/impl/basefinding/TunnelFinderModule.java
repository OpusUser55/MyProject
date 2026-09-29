package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.TracerOrigin;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.ChunkScanner;
import dev.ooga.client.world.ChunkView;
import dev.ooga.client.world.Finds;
import dev.ooga.client.world.Passable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Finds player-dug tunnels: straight 1x2 corridors with a solid floor, ceiling and walls,
 * made of plain air. Caves are irregular and carved with cave air, so a long, perfectly
 * straight corridor underground is almost always someone travelling to (or from) a base.
 *
 * <p>Corridor cells are kept per chunk and joined into runs across chunk borders, so a tunnel
 * is measured by its real length, not just the part inside one chunk.
 */
public class TunnelFinderModule extends Module implements ChunkScanner.Listener {
	private static final int COLOR = 0x5FB3F0;

	public final NumberSetting minLength = add(new NumberSetting("Min Length", "Shortest straight run that counts as a tunnel.", 10, 4, 48, 1, "m"));
	public final NumberSetting minY = add(new NumberSetting("Min Y", "Ignore anything below this height.", -64, -64, 320, 1));
	public final NumberSetting maxY = add(new NumberSetting("Max Y", "Ignore anything above this height. Surface paths make noise.", 55, -64, 320, 1));
	public final NumberSetting alertLength = add(new NumberSetting("Alert Length", "Announce tunnels at least this long.", 32, 8, 256, 4, "m"));
	public final BooleanSetting chat = add(new BooleanSetting("Chat Alerts", "Post long tunnels in chat.", true));
	public final BooleanSetting toast = add(new BooleanSetting("Notifications", "Show a notification for long tunnels.", true));
	public final BooleanSetting tracers = add(new BooleanSetting("Tracers", "Lines from your view to each tunnel.", false));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum highlight distance.", 256, 16, 512, 8, "m"));

	/** Corridor cells (feet level) per chunk, split by the axis the corridor runs along. */
	private final Map<Long, long[]> cellsX = new HashMap<>();
	private final Map<Long, long[]> cellsZ = new HashMap<>();
	private final List<AABB> tunnels = new ArrayList<>();
	private final Set<Long> announced = new HashSet<>();
	private final ChunkView view = new ChunkView();
	private List<Long> buildingX;
	private List<Long> buildingZ;
	private boolean dirty;

	public TunnelFinderModule() {
		super("Tunnel Finder", "Highlights long, straight, player-dug tunnels underground.", Category.BASEFINDING);
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
		buildingX = new ArrayList<>();
		buildingZ = new ArrayList<>();
		view.begin(pos);
	}

	@Override
	public void block(int x, int y, int z, BlockState state) {
		if (y < minY.getInt() || y > maxY.getInt()) return;
		// A 2-high opening with solid floor and ceiling...
		if (!Passable.is(view.get(x, y + 1, z)) || !view.solid(x, y - 1, z) || !view.solid(x, y + 2, z)) return;
		// ...walled in on exactly one axis, so it runs along the other.
		boolean wallsZ = view.solid(x, y, z - 1) && view.solid(x, y, z + 1) && view.solid(x, y + 1, z - 1) && view.solid(x, y + 1, z + 1);
		boolean wallsX = view.solid(x - 1, y, z) && view.solid(x + 1, y, z) && view.solid(x - 1, y + 1, z) && view.solid(x + 1, y + 1, z);
		if (wallsZ == wallsX) return;
		long cell = BlockPos.asLong(x, y, z);
		if (wallsZ) buildingX.add(cell);
		else buildingZ.add(cell);
	}

	@Override
	public void endChunk(ChunkPos pos) {
		store(cellsX, pos, buildingX);
		store(cellsZ, pos, buildingZ);
		buildingX = buildingZ = null;
		view.end();
		dirty = true;
	}

	private static void store(Map<Long, long[]> map, ChunkPos pos, List<Long> cells) {
		if (cells.isEmpty()) {
			map.remove(pos.toLong());
			return;
		}
		long[] array = new long[cells.size()];
		for (int i = 0; i < array.length; i++) array[i] = cells.get(i);
		map.put(pos.toLong(), array);
	}

	@Override
	public void forgetChunk(ChunkPos pos) {
		boolean removed = cellsX.remove(pos.toLong()) != null;
		removed |= cellsZ.remove(pos.toLong()) != null;
		if (removed) dirty = true;
	}

	@Override
	public void clear() {
		cellsX.clear();
		cellsZ.clear();
		tunnels.clear();
		announced.clear();
		dirty = false;
	}

	@Override
	public void onTick() {
		if (!dirty) return;
		dirty = false;
		rebuild();
	}

	/** Joins corridor cells into straight runs, allowing one-block gaps for torches or ores. */
	private void rebuild() {
		tunnels.clear();
		collect(cellsX, true);
		collect(cellsZ, false);
	}

	private void collect(Map<Long, long[]> cells, boolean alongX) {
		// Group by the line each cell lies on: (y, z) for X-tunnels, (y, x) for Z-tunnels.
		Map<Long, List<Integer>> lines = new HashMap<>();
		for (long[] chunk : cells.values()) {
			for (long cell : chunk) {
				int x = BlockPos.getX(cell), y = BlockPos.getY(cell), z = BlockPos.getZ(cell);
				long line = ((long) y << 32) | ((alongX ? z : x) & 0xFFFFFFFFL);
				lines.computeIfAbsent(line, k -> new ArrayList<>()).add(alongX ? x : z);
			}
		}
		int min = minLength.getInt();
		for (Map.Entry<Long, List<Integer>> entry : lines.entrySet()) {
			List<Integer> along = entry.getValue();
			if (along.size() < min * 3 / 4) continue;
			Collections.sort(along);
			int y = (int) (entry.getKey() >> 32);
			int across = (int) (long) entry.getKey();
			int start = along.get(0), prev = start, count = 1;
			for (int i = 1; i <= along.size(); i++) {
				boolean more = i < along.size();
				int next = more ? along.get(i) : 0;
				if (more && next - prev <= 2) {
					prev = next;
					count++;
					continue;
				}
				int length = prev - start + 1;
				if (length >= min && count * 4 >= length * 3) addTunnel(alongX, y, across, start, prev, entry.getKey());
				start = prev = next;
				count = 1;
			}
		}
	}

	private void addTunnel(boolean alongX, int y, int across, int from, int to, long line) {
		AABB box = alongX
				? new AABB(from, y, across, to + 1, y + 2, across + 1)
				: new AABB(across, y, from, across + 1, y + 2, to + 1);
		tunnels.add(box);
		int length = to - from + 1;
		if (length < alertLength.getInt()) return;
		// One alert per tunnel line, however far it grows as more chunks load.
		long key = line * 31 + (alongX ? 1 : 2);
		if (!announced.add(key)) return;
		BlockPos mid = BlockPos.containing(box.getCenter());
		String where = mid.getX() + ", " + mid.getY() + ", " + mid.getZ();
		String detail = length + "m along " + (alongX ? "X" : "Z");
		if (!Finds.report("Tunnel", detail, mid)) return;
		if (chat.get()) ChatUtil.info("Tunnel at " + where + " (" + detail + ")");
		if (toast.get()) NotificationManager.get().push("Tunnel found", where, Notification.Kind.INFO);
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null || tunnels.isEmpty()) return;
		double maxSq = range.get() * range.get();
		Vec3 eye = mc.player.getEyePosition(partialTick);
		Vec3 origin = tracers.get() ? TracerOrigin.get(drawer, partialTick) : null;
		for (AABB box : tunnels) {
			Vec3 center = box.getCenter();
			if (center.distanceToSqr(eye) > maxSq) continue;
			drawer.box(box, ColorUtil.withAlpha(COLOR, 40), ColorUtil.withAlpha(COLOR, 210));
			if (origin != null) drawer.line(origin, center, ColorUtil.withAlpha(COLOR, 150));
		}
	}

	@Override
	public String getSuffix() {
		return Integer.toString(tunnels.size());
	}
}
