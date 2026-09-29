package dev.ooga.client.world;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Fast block reads around one chunk while a finder is scanning it. Reads inside the chunk go
 * straight to the section palettes; reads just across the border fall back to the level.
 * Shape detectors (tunnels, holes) make several neighbour reads per candidate block, so this
 * keeps them cheap enough to run on every chunk.
 */
public final class ChunkView {
	private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
	private LevelChunk chunk;
	private LevelChunkSection[] sections;
	private int minX;
	private int minZ;

	public void begin(ChunkPos pos) {
		Minecraft mc = Minecraft.getInstance();
		chunk = mc.level == null ? null : mc.level.getChunk(pos.x, pos.z);
		sections = chunk == null ? null : chunk.getSections();
		minX = pos.getMinBlockX();
		minZ = pos.getMinBlockZ();
	}

	public void end() {
		chunk = null;
		sections = null;
	}

	public BlockState get(int x, int y, int z) {
		int lx = x - minX, lz = z - minZ;
		if (chunk != null && lx >= 0 && lx < 16 && lz >= 0 && lz < 16) {
			int index = chunk.getSectionIndex(y);
			if (index < 0 || index >= sections.length) return Blocks.AIR.defaultBlockState();
			LevelChunkSection section = sections[index];
			return section == null ? Blocks.AIR.defaultBlockState() : section.getBlockState(lx, y & 15, lz);
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return Blocks.AIR.defaultBlockState();
		return mc.level.getBlockState(cursor.set(x, y, z));
	}

	/** A full, opaque block: the kind of thing a tunnel wall or floor is made of. */
	public boolean solid(int x, int y, int z) {
		return get(x, y, z).isSolidRender();
	}
}
