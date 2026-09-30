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
import dev.ooga.client.world.Finds;
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

/** Ancient debris through walls, with a chat alert for every chunk that has some. */
public class NetheriteFinderModule extends Module implements ChunkScanner.Listener {
	private static final int COLOR = 0xB0714F;

	public final BooleanSetting alerts = add(new BooleanSetting("Alerts", "Post each chunk's debris in chat.", true));
	public final BooleanSetting tracers = add(new BooleanSetting("Tracers", "Lines to each debris block.", false));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum highlight distance.", 96, 16, 256, 8, "m"));

	private final Map<Long, List<BlockPos>> found = new HashMap<>();
	private final Set<Long> announced = new HashSet<>();
	private List<BlockPos> building;
	private int total;

	public NetheriteFinderModule() {
		super("Netherite Finder", "Ancient debris through walls, with alerts.", Category.BASEFINDING);
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
		return state -> state.is(Blocks.ANCIENT_DEBRIS);
	}

	@Override
	public void beginChunk(ChunkPos pos) {
		building = new ArrayList<>();
	}

	@Override
	public void block(int x, int y, int z, BlockState state) {
		building.add(new BlockPos(x, y, z));
	}

	@Override
	public void endChunk(ChunkPos pos) {
		if (building.isEmpty()) found.remove(pos.toLong());
		else {
			found.put(pos.toLong(), building);
			if (announced.add(pos.toLong())) {
				BlockPos first = building.get(0);
				Finds.report("Debris", building.size() + " ancient debris", first);
				if (alerts.get()) ChatUtil.info(building.size() + " ancient debris near " + first.getX() + ", " + first.getY() + ", " + first.getZ());
			}
		}
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
		Vec3 origin = tracers.get() ? TracerOrigin.get(drawer, partialTick) : null;
		for (List<BlockPos> list : found.values()) {
			for (BlockPos pos : list) {
				if (pos.distToCenterSqr(eye) > maxSq) continue;
				AABB box = new AABB(pos);
				drawer.box(box, ColorUtil.withAlpha(COLOR, 60), ColorUtil.withAlpha(COLOR, 230));
				if (origin != null) drawer.line(origin, box.getCenter(), ColorUtil.withAlpha(COLOR, 150));
			}
		}
	}

	@Override
	public String getSuffix() {
		return Integer.toString(total);
	}
}
