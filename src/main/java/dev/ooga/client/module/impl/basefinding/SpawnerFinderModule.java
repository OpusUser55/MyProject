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
import dev.ooga.client.world.BlockEntityTracker;
import dev.ooga.client.world.Finds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Finds mob spawners as their chunks load and tells natural ones (dungeons, mineshafts,
 * fortresses, strongholds) apart from ones players have placed, which is what a spawner base
 * looks like. Spawners that load together are grouped into one alert per cluster, with the
 * mob types, so a stacked spawner room doesn't flood chat.
 */
public class SpawnerFinderModule extends Module {
	private static final int PLACED_COLOR = 0xE8594A;
	private static final int NATURAL_COLOR = 0xB08A5A;
	/** Blocks that surround generated spawners: dungeon floors, mineshaft webs, fortress and stronghold bricks. */
	private static final Set<Block> STRUCTURE = Set.of(
			Blocks.MOSSY_COBBLESTONE, Blocks.COBWEB, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_FENCE,
			Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.INFESTED_STONE_BRICKS,
			Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
	private static final double CLUSTER_RADIUS = 16;

	public final BooleanSetting trial = add(new BooleanSetting("Trial Spawners", "Include trial spawners from trial chambers.", false));
	public final BooleanSetting hideNatural = add(new BooleanSetting("Hide Natural", "Skip spawners that look generated (dungeons, mineshafts, fortresses).", false));
	public final BooleanSetting chat = add(new BooleanSetting("Chat Alerts", "Post new spawners in chat with their mob and coordinates.", true));
	public final BooleanSetting toast = add(new BooleanSetting("Notifications", "Show a notification for new spawners.", true));
	public final BooleanSetting tracers = add(new BooleanSetting("Tracers", "Lines from your view to each spawner.", true));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum highlight distance.", 256, 16, 512, 8, "m"));

	private record Info(String mob, boolean natural) {
	}

	private final Map<BlockPos, Info> known = new HashMap<>();
	private int visible;
	/** The level our state belongs to; a new one (dimension change, new server) resets it. */
	private Object lastLevel;

	public SpawnerFinderModule() {
		super("Spawner Finder", "Alerts and highlights spawners, telling placed ones from dungeons.", Category.BASEFINDING);
		WorldOverlay.register(this::draw);
	}

	@Override
	protected void onEnable() {
		known.clear();
	}

	private boolean matches(BlockEntity be) {
		return be instanceof SpawnerBlockEntity || (trial.get() && be instanceof TrialSpawnerBlockEntity);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null) return;
		if (mc.level != lastLevel) {
			lastLevel = mc.level;
			known.clear();
		}
		List<BlockPos> fresh = new ArrayList<>();
		for (BlockEntity be : BlockEntityTracker.all()) {
			if (!matches(be) || be.isRemoved()) continue;
			BlockPos pos = be.getBlockPos().immutable();
			if (known.containsKey(pos)) continue;
			known.put(pos, new Info(mobOf(be), looksNatural(pos)));
			fresh.add(pos);
		}
		if (!fresh.isEmpty()) announce(fresh);
	}

	private String mobOf(BlockEntity be) {
		if (be instanceof TrialSpawnerBlockEntity) return "Trial";
		try {
			Entity display = ((SpawnerBlockEntity) be).getSpawner().getOrCreateDisplayEntity(mc.level, be.getBlockPos());
			return display == null ? "Empty" : display.getType().getDescription().getString();
		} catch (RuntimeException e) {
			return "Unknown";
		}
	}

	/** Generated spawners sit inside their structure; placed ones usually don't. */
	private boolean looksNatural(BlockPos center) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -3; dz <= 3; dz++) {
					cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (STRUCTURE.contains(mc.level.getBlockState(cursor).getBlock())) return true;
				}
			}
		}
		return false;
	}

	/** One alert per cluster of spawners that appeared this tick. */
	private void announce(List<BlockPos> fresh) {
		List<List<BlockPos>> clusters = new ArrayList<>();
		for (BlockPos pos : fresh) {
			List<BlockPos> home = null;
			for (List<BlockPos> cluster : clusters) {
				if (cluster.get(0).distToCenterSqr(Vec3.atCenterOf(pos)) <= CLUSTER_RADIUS * CLUSTER_RADIUS) {
					home = cluster;
					break;
				}
			}
			if (home == null) clusters.add(home = new ArrayList<>());
			home.add(pos);
		}
		for (List<BlockPos> cluster : clusters) {
			Map<String, Integer> mobs = new LinkedHashMap<>();
			boolean natural = true;
			for (BlockPos pos : cluster) {
				Info info = known.get(pos);
				if (hideNatural.get() && info.natural()) continue;
				natural &= info.natural();
				mobs.merge(info.mob(), 1, Integer::sum);
				Finds.report("Spawner", info.mob() + (info.natural() ? " (natural)" : ""), pos);
			}
			if (mobs.isEmpty()) continue;
			// A lone generated spawner is a dungeon; several placed together is a spawner base.
			if (cluster.size() > 1) natural = false;

			StringJoiner what = new StringJoiner(", ");
			mobs.forEach((mob, n) -> what.add(n > 1 ? n + "x " + mob : mob));
			BlockPos pos = cluster.get(0);
			String where = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
			int distance = (int) Math.sqrt(pos.distToCenterSqr(mc.player.position()));
			int count = mobs.values().stream().mapToInt(Integer::intValue).sum();
			String title = count > 1 ? count + " spawners" : "Spawner";
			String kind = natural ? " (likely natural)" : "";
			if (chat.get()) ChatUtil.info(title + " at " + where + ": " + what + kind + " (" + distance + "m)");
			if (toast.get()) NotificationManager.get().push(title + " found", what + " · " + where, Notification.Kind.INFO);
		}
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null) return;
		double maxSq = range.get() * range.get();
		Vec3 eye = mc.player.getEyePosition(partialTick);
		Vec3 origin = TracerOrigin.get(drawer, partialTick);
		int count = 0;
		for (BlockEntity be : BlockEntityTracker.all()) {
			if (!matches(be) || be.isRemoved()) continue;
			BlockPos pos = be.getBlockPos();
			Info info = known.get(pos);
			boolean natural = info != null && info.natural();
			if (natural && hideNatural.get()) continue;
			if (pos.distToCenterSqr(eye) > maxSq) continue;
			int rgb = natural ? NATURAL_COLOR : PLACED_COLOR;
			AABB box = new AABB(pos);
			drawer.box(box, ColorUtil.withAlpha(rgb, 50), ColorUtil.withAlpha(rgb, 230));
			if (tracers.get()) drawer.line(origin, box.getCenter(), ColorUtil.withAlpha(rgb, 170));
			count++;
		}
		visible = count;
	}

	@Override
	public String getSuffix() {
		return Integer.toString(visible);
	}
}
