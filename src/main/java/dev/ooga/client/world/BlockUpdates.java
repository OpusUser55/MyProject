package dev.ooga.client.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Block changes the server sends after a chunk has loaded (single updates and section
 * batches), fanned out on the client thread. Finders use it to keep their results current
 * as blocks are placed, mined or liquids flow.
 */
public final class BlockUpdates {
	@FunctionalInterface
	public interface Listener {
		void onBlockUpdate(BlockPos pos, BlockState state);
	}

	private static final List<Listener> LISTENERS = new ArrayList<>();

	private BlockUpdates() {
	}

	public static void register(Listener listener) {
		LISTENERS.add(listener);
	}

	/** Called by the packet listener mixin; {@code pos} may be mutable, so copy it to keep it. */
	public static void fire(BlockPos pos, BlockState state) {
		for (Listener listener : LISTENERS) listener.onBlockUpdate(pos, state);
	}
}
