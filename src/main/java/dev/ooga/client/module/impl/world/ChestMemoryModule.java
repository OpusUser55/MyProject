package dev.ooga.client.module.impl.world;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.TracerOrigin;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.waypoint.WaypointStore;
import dev.ooga.client.world.BlockUpdates;
import dev.ooga.client.world.ContainerMemory;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers what's in every chest, barrel and shulker box you open (as of when you close it),
 * so {@code .find <item>} can tell you where you put something and light those containers up.
 * Containers you break are forgotten.
 */
public class ChestMemoryModule extends Module implements BlockUpdates.Listener {
	private static final int COLOR = 0xF2C14E;

	public final NumberSetting highlightTime = add(new NumberSetting("Highlight Time", "How long .find results stay highlighted.", 30, 5, 300, 5, "s"));
	public final BooleanSetting showKnown = add(new BooleanSetting("Show Known", "Faintly outline every container Chest Memory knows about.", false));

	private List<BlockPos> highlighted = List.of();
	private long highlightUntil;

	public ChestMemoryModule() {
		super("Chest Memory", "Remembers container contents. Search with .find <item>.", Category.WORLD);
		enableByDefault();
		BlockUpdates.register(this);
		WorldOverlay.register(this::draw);
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!isEnabled() || !(screen instanceof AbstractContainerScreen<?> containerScreen)) return;
			BlockPos pos = targetedStorage();
			if (pos == null) return;
			ScreenEvents.remove(screen).register(closed -> snapshot(containerScreen, pos));
		});
	}

	/** The chest, barrel or shulker box you're looking at, which is the one being opened. */
	private BlockPos targetedStorage() {
		if (mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
		BlockEntity be = mc.level.getBlockEntity(hit.getBlockPos());
		boolean storage = be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity;
		return storage ? hit.getBlockPos().immutable() : null;
	}

	private void snapshot(AbstractContainerScreen<?> screen, BlockPos pos) {
		if (mc.player == null) return;
		Map<String, Integer> items = new HashMap<>();
		Map<String, String> names = new HashMap<>();
		for (Slot slot : screen.getMenu().slots) {
			// Skip your own inventory rows shown under the container.
			if (slot.container == mc.player.getInventory()) continue;
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) continue;
			String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().replace("minecraft:", "");
			items.merge(id, stack.getCount(), Integer::sum);
			names.putIfAbsent(id, stack.getHoverName().getString());
		}
		ContainerMemory.remember(pos, items, names);
	}

	@Override
	public void blockChanged(BlockPos pos, BlockState state) {
		if (state.isAir() && ContainerMemory.knows(pos)) ContainerMemory.forget(pos);
	}

	@Override
	public void onTick() {
		ContainerMemory.tick();
	}

	/** Runs a search, prints the nearest results and highlights them. */
	public void find(String query) {
		if (mc.player == null) return;
		List<Map.Entry<ContainerMemory.Entry, Integer>> hits = new ArrayList<>(ContainerMemory.search(query));
		if (hits.isEmpty()) {
			ChatUtil.info("No remembered container has \"" + query + "\" (" + ContainerMemory.size() + " containers known here).");
			return;
		}
		Vec3 me = mc.player.position();
		hits.sort(Comparator.comparingDouble(e -> e.getKey().pos().distToCenterSqr(me)));
		int total = hits.stream().mapToInt(Map.Entry::getValue).sum();
		ChatUtil.info(total + "× \"" + query + "\" in " + hits.size() + " container" + (hits.size() == 1 ? "" : "s") + ":");
		List<BlockPos> glow = new ArrayList<>();
		for (int i = 0; i < hits.size(); i++) {
			ContainerMemory.Entry e = hits.get(i).getKey();
			if (i < 8) {
				BlockPos p = e.pos();
				String where = e.dimension().equals(WaypointStore.currentDimension())
						? (int) Math.sqrt(p.distToCenterSqr(me)) + "m" : e.dimension();
				ChatUtil.info("  " + hits.get(i).getValue() + " at " + p.getX() + ", " + p.getY() + ", " + p.getZ() + " (" + where + ")");
			}
			glow.add(e.pos());
		}
		if (hits.size() > 8) ChatUtil.info("  …and " + (hits.size() - 8) + " more (highlighted)");
		highlighted = glow;
		highlightUntil = System.currentTimeMillis() + highlightTime.getInt() * 1000L;
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (mc.player == null) return;
		if (showKnown.get() && isEnabled()) {
			for (ContainerMemory.Entry e : ContainerMemory.hereAndNow()) {
				drawer.outline(new AABB(e.pos()), ColorUtil.withAlpha(COLOR, 70));
			}
		}
		if (System.currentTimeMillis() > highlightUntil || highlighted.isEmpty()) return;
		Vec3 origin = TracerOrigin.get(drawer, partialTick);
		for (BlockPos pos : highlighted) {
			AABB box = new AABB(pos);
			drawer.box(box, ColorUtil.withAlpha(COLOR, 70), ColorUtil.withAlpha(COLOR, 240));
			drawer.line(origin, box.getCenter(), ColorUtil.withAlpha(COLOR, 170));
		}
	}

	@Override
	public String getSuffix() {
		return Integer.toString(ContainerMemory.size());
	}
}
