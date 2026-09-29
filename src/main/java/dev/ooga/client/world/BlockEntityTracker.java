package dev.ooga.client.world;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every block entity in loaded client chunks, maintained from Fabric's load/unload events,
 * so storage and spawner finders never have to rescan terrain.
 */
public final class BlockEntityTracker {
	private static final Set<BlockEntity> LOADED = ConcurrentHashMap.newKeySet();

	private BlockEntityTracker() {
	}

	public static void init() {
		ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> LOADED.add(blockEntity));
		ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, level) -> LOADED.remove(blockEntity));
	}

	public static Collection<BlockEntity> all() {
		return Collections.unmodifiableSet(LOADED);
	}

	public static void clear() {
		LOADED.clear();
	}
}
