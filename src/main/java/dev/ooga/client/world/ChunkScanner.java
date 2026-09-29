package dev.ooga.client.world;

import dev.ooga.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Walks the blocks of newly loaded chunks on behalf of finder modules, a few chunks per tick
 * so joining a world never causes a frame spike. Sections that can't contain anything a
 * listener cares about are skipped via the palette, which is most of them.
 */
public final class ChunkScanner {
	/** Receives the blocks of a chunk that match its filter. */
	public interface Listener {
		boolean active();

		Predicate<BlockState> filter();

		void beginChunk(ChunkPos pos);

		void block(int x, int y, int z, BlockState state);

		void endChunk(ChunkPos pos);

		void forgetChunk(ChunkPos pos);

		void clear();

		/** Lets a listener skip whole 16-block sections, e.g. everything above a height limit. */
		default boolean wantsSection(int minY) {
			return true;
		}
	}

	private static final int CHUNKS_PER_TICK = 3;
	/** Quiet period after a block change before its chunk is rescanned, so mining doesn't rescan every tick. */
	private static final int RESCAN_DELAY_TICKS = 20;
	private static final List<Listener> LISTENERS = new ArrayList<>();
	private static final Deque<ChunkPos> QUEUE = new ArrayDeque<>();
	private static final Set<ChunkPos> QUEUED = new HashSet<>();
	/** Chunks with recent block changes, and the tick at which each may be rescanned. */
	private static final Map<Long, Integer> DIRTY = new HashMap<>();
	private static int ticks;

	private ChunkScanner() {
	}

	public static void init() {
		ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> enqueue(chunk.getPos()));
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
			ChunkPos pos = chunk.getPos();
			QUEUED.remove(pos);
			for (Listener listener : LISTENERS) listener.forgetChunk(pos);
		});
	}

	public static void register(Listener listener) {
		LISTENERS.add(listener);
	}

	private static void enqueue(ChunkPos pos) {
		if (QUEUED.add(pos)) QUEUE.addLast(pos);
	}

	/** Queues every loaded chunk around the player, e.g. when a finder is switched on. */
	public static void rescanLoaded() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		int radius = mc.options.getEffectiveRenderDistance() + 1;
		ChunkPos center = mc.player.chunkPosition();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				int cx = center.x + dx, cz = center.z + dz;
				if (mc.level.getChunkSource().hasChunk(cx, cz)) enqueue(new ChunkPos(cx, cz));
			}
		}
	}

	/** Schedules a rescan once the chunk has stopped changing for a moment. */
	public static void markDirty(ChunkPos pos) {
		DIRTY.put(pos.toLong(), ticks + RESCAN_DELAY_TICKS);
	}

	public static void clear() {
		QUEUE.clear();
		QUEUED.clear();
		DIRTY.clear();
		for (Listener listener : LISTENERS) listener.clear();
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		ticks++;
		List<Listener> active = new ArrayList<>();
		for (Listener listener : LISTENERS) if (listener.active()) active.add(listener);
		if (active.isEmpty()) {
			// Nothing wants results; drop the backlog rather than scanning for nobody.
			QUEUE.clear();
			QUEUED.clear();
			DIRTY.clear();
			return;
		}
		if (!DIRTY.isEmpty()) {
			Iterator<Map.Entry<Long, Integer>> it = DIRTY.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Long, Integer> entry = it.next();
				if (entry.getValue() > ticks) continue;
				it.remove();
				enqueue(new ChunkPos(entry.getKey()));
			}
		}
		for (int i = 0; i < CHUNKS_PER_TICK && !QUEUE.isEmpty(); i++) {
			ChunkPos pos = QUEUE.pollFirst();
			QUEUED.remove(pos);
			if (!mc.level.getChunkSource().hasChunk(pos.x, pos.z)) continue;
			try {
				scan(mc.level.getChunk(pos.x, pos.z), active);
			} catch (RuntimeException e) {
				ModuleManager.LOGGER.debug("Chunk scan failed at {}", pos, e);
			}
		}
	}

	private static void scan(LevelChunk chunk, List<Listener> listeners) {
		ChunkPos pos = chunk.getPos();
		for (Listener listener : listeners) listener.beginChunk(pos);
		LevelChunkSection[] sections = chunk.getSections();
		int baseX = pos.getMinBlockX();
		int baseZ = pos.getMinBlockZ();
		for (int index = 0; index < sections.length; index++) {
			LevelChunkSection section = sections[index];
			if (section == null || section.hasOnlyAir()) continue;
			int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
			for (Listener listener : listeners) {
				Predicate<BlockState> filter = listener.filter();
				if (!section.maybeHas(filter)) continue;
				for (int y = 0; y < 16; y++) {
					for (int z = 0; z < 16; z++) {
						for (int x = 0; x < 16; x++) {
							BlockState state = section.getBlockState(x, y, z);
							if (filter.test(state)) listener.block(baseX + x, baseY + y, baseZ + z, state);
						}
					}
				}
			}
		}
		for (Listener listener : listeners) listener.endChunk(pos);
	}
}
