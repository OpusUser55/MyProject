package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.BlockUpdates;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Marks chunks the server generated just now. Freshly generated water and lava hasn't
 * settled yet, so the server streams flowing-liquid updates for a chunk right after sending
 * it; in chunks that existed before, liquids have long since stopped. Territory made of old
 * chunks has been explored by someone; a trail of old chunks through new ones is a path a
 * player took.
 */
public class NewChunksModule extends Module {
	private static final int NEW_COLOR = 0xE5484D;

	public final NumberSetting window = add(new NumberSetting("Window", "How long after a chunk loads flowing liquid still counts.", 10, 2, 30, 1, "s"));
	public final ModeSetting height = add(new ModeSetting("Height", "Draw the marker at a fixed height or at your feet.", "Player", "Player", "Fixed"));
	public final NumberSetting fixedY = add(new NumberSetting("Marker Y", "Height of the marker in Fixed mode.", 63, -64, 320, 1)
			.visibleWhen(() -> height.is("Fixed")));
	public final NumberSetting opacity = add(new NumberSetting("Opacity", "Marker opacity.", 0.25, 0.05, 0.8, 0.05));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance in chunks.", 32, 4, 64, 1));

	/** Game time each chunk arrived, while it's still inside the detection window. */
	private final Map<Long, Long> loadedAt = new HashMap<>();
	/** New chunks per dimension, so markers from one dimension never show in another. */
	private final Map<String, Set<Long>> fresh = new HashMap<>();

	public NewChunksModule() {
		super("New Chunks", "Marks freshly generated chunks, so old (explored) land stands out.", Category.BASEFINDING);
		ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			if (isEnabled()) loadedAt.put(chunk.getPos().toLong(), level.getGameTime());
		});
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> loadedAt.remove(chunk.getPos().toLong()));
		BlockUpdates.register(this::onBlockUpdate);
		WorldOverlay.register(this::draw);
	}

	@Override
	protected void onEnable() {
		loadedAt.clear();
		fresh.clear();
	}

	@Override
	protected void onDisable() {
		loadedAt.clear();
		fresh.clear();
	}

	private void onBlockUpdate(BlockPos pos, BlockState state) {
		if (!isEnabled() || mc.level == null) return;
		FluidState fluid = state.getFluidState();
		if (fluid.isEmpty() || fluid.isSource()) return;
		long key = ChunkPos.asLong(pos);
		Long loaded = loadedAt.get(key);
		if (loaded == null) return;
		if (mc.level.getGameTime() - loaded <= window.get() * 20) {
			fresh.computeIfAbsent(dimension(), k -> new HashSet<>()).add(key);
		}
	}

	private String dimension() {
		return mc.level.dimension().identifier().toString();
	}

	/** Called on disconnect: markers belong to one server. */
	public void clearWorld() {
		loadedAt.clear();
		fresh.clear();
	}

	@Override
	public void onTick() {
		if (mc.level == null || loadedAt.isEmpty()) return;
		long expired = mc.level.getGameTime() - Math.round(window.get() * 20);
		loadedAt.values().removeIf(time -> time < expired);
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null || mc.level == null) return;
		Set<Long> chunks = fresh.get(dimension());
		if (chunks == null || chunks.isEmpty()) return;
		double y = height.is("Fixed") ? fixedY.get() : Math.floor(mc.player.getPosition(partialTick).y);
		ChunkPos here = mc.player.chunkPosition();
		int max = range.getInt();
		int fill = ColorUtil.withAlpha(NEW_COLOR, Math.round(255 * opacity.getFloat()));
		int line = ColorUtil.withAlpha(NEW_COLOR, Math.min(255, Math.round(255 * opacity.getFloat() * 3)));
		for (long key : chunks) {
			ChunkPos pos = new ChunkPos(key);
			if (Math.abs(pos.x - here.x) > max || Math.abs(pos.z - here.z) > max) continue;
			AABB slab = new AABB(pos.getMinBlockX(), y, pos.getMinBlockZ(), pos.getMaxBlockX() + 1, y + 0.05, pos.getMaxBlockZ() + 1);
			drawer.box(slab, fill, line);
		}
	}

	@Override
	public String getSuffix() {
		Set<Long> chunks = mc.level == null ? null : fresh.get(dimension());
		return Integer.toString(chunks == null ? 0 : chunks.size());
	}
}
