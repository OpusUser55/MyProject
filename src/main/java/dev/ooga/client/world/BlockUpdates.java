package dev.ooga.client.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Block changes the server sends after a chunk has loaded: mining, explosions, pistons, and
 * blocks an anti-xray plugin reveals once they're exposed. {@link ChunkScanner} only sees the
 * initial chunk data, so finders listen here to stay current.
 */
public final class BlockUpdates {
	public interface Listener {
		void blockChanged(BlockPos pos, BlockState state);
	}

	private static final List<Listener> LISTENERS = new ArrayList<>();

	private BlockUpdates() {
	}

	public static void register(Listener listener) {
		LISTENERS.add(listener);
	}

	/** Called on the client thread by {@code ClientLevelMixin}. */
	public static void fire(BlockPos pos, BlockState state) {
		BlockPos immutable = pos.immutable();
		for (Listener listener : LISTENERS) listener.blockChanged(immutable, state);
	}
}
