package dev.ooga.client.world;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Blocks a player-dug tunnel or shaft is "made of": plain air plus the few things people put
 * in them. Cave air is deliberately left out: carvers fill caves with it, so a corridor of
 * ordinary air underground was far more likely dug than generated.
 */
public final class Passable {
	private static final Set<Block> OPEN = Set.of(
			Blocks.AIR, Blocks.LADDER, Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
			Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH, Blocks.RAIL, Blocks.POWERED_RAIL);

	private Passable() {
	}

	public static boolean is(BlockState state) {
		return OPEN.contains(state.getBlock());
	}
}
