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
import dev.ooga.client.waypoint.Waypoint;
import dev.ooga.client.waypoint.WaypointStore;
import dev.ooga.client.world.BlockEntityTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Flags chunks holding lots of storage: a pile of chests, barrels and shulkers in one chunk is
 * almost always someone's stash or storage room. Each one is announced once and outlined.
 */
public class StashFinderModule extends Module {
	private static final int COLOR = 0x6BE3A4;

	public final NumberSetting minContainers = add(new NumberSetting("Min Containers", "Containers in one chunk needed to count as a stash.", 12, 3, 100, 1));
	public final BooleanSetting shulkers = add(new BooleanSetting("Count Shulkers x3", "Placed shulker boxes count triple; they're rarely there by accident.", true));
	public final BooleanSetting chat = add(new BooleanSetting("Chat Alerts", "Post each stash's coordinates in chat.", true));
	public final BooleanSetting waypoints = add(new BooleanSetting("Save Waypoints", "Save a \"stash\" waypoint for each find.", false));
	public final BooleanSetting tracers = add(new BooleanSetting("Tracers", "Lines to each stash.", true));

	private record Stash(AABB box, int containers) {
	}

	private final Map<Long, Stash> stashes = new HashMap<>();
	private final Set<Long> announced = new HashSet<>();
	private int timer;

	public StashFinderModule() {
		super("Stash Finder", "Finds chunks packed with chests, barrels and shulkers.", Category.BASEFINDING);
		WorldOverlay.register(this::draw);
	}

	@Override
	protected void onEnable() {
		stashes.clear();
		announced.clear();
		timer = 0;
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null || timer-- > 0) return;
		timer = 20;

		Map<Long, int[]> counts = new HashMap<>();
		Map<Long, int[]> heights = new HashMap<>();
		for (BlockEntity be : BlockEntityTracker.all()) {
			if (be.isRemoved()) continue;
			int weight;
			if (be instanceof ShulkerBoxBlockEntity) weight = shulkers.get() ? 3 : 1;
			else if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) weight = 1;
			else continue;
			BlockPos pos = be.getBlockPos();
			long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
			counts.computeIfAbsent(key, k -> new int[1])[0] += weight;
			int[] h = heights.computeIfAbsent(key, k -> new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE});
			h[0] = Math.min(h[0], pos.getY());
			h[1] = Math.max(h[1], pos.getY());
		}

		stashes.clear();
		for (Map.Entry<Long, int[]> e : counts.entrySet()) {
			int count = e.getValue()[0];
			if (count < minContainers.getInt()) continue;
			ChunkPos chunk = new ChunkPos(e.getKey());
			int[] h = heights.get(e.getKey());
			AABB box = new AABB(chunk.getMinBlockX(), h[0], chunk.getMinBlockZ(), chunk.getMaxBlockX() + 1, h[1] + 1, chunk.getMaxBlockZ() + 1);
			stashes.put(e.getKey(), new Stash(box, count));
			if (announced.add(e.getKey())) announce(box, count);
		}
	}

	private void announce(AABB box, int count) {
		BlockPos center = BlockPos.containing(box.getCenter());
		String where = center.getX() + ", " + center.getY() + ", " + center.getZ();
		int distance = (int) Math.sqrt(center.distToCenterSqr(mc.player.position()));
		if (chat.get()) ChatUtil.info("Possible stash: " + count + " containers at " + where + " (" + distance + "m)");
		NotificationManager.get().push("Stash found", count + " containers at " + where, Notification.Kind.INFO);
		if (waypoints.get()) {
			WaypointStore.add(new Waypoint("stash-" + (center.getX() >> 4) + "_" + (center.getZ() >> 4), center, WaypointStore.currentDimension()));
		}
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null) return;
		Vec3 origin = TracerOrigin.get(drawer, partialTick);
		for (Stash stash : stashes.values()) {
			drawer.box(stash.box(), ColorUtil.withAlpha(COLOR, 25), ColorUtil.withAlpha(COLOR, 220));
			if (tracers.get()) drawer.line(origin, stash.box().getCenter(), ColorUtil.withAlpha(COLOR, 160));
		}
	}

	@Override
	public String getSuffix() {
		return Integer.toString(stashes.size());
	}
}
