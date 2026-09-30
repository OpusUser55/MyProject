package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.world.level.ChunkPos;

/** Chunk borders around you, handy for sus-chunk and new-chunk hunting. */
public class ChunkBordersModule extends Module {
	public final NumberSetting radius = add(new NumberSetting("Radius", "Chunks around you to outline.", 1, 0, 4, 1));

	public ChunkBordersModule() {
		super("Chunk Borders", "Outlines the chunks around you.", Category.RENDER);
		WorldOverlay.register(this::draw);
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null || mc.level == null) return;
		ChunkPos here = mc.player.chunkPosition();
		double y = Math.floor(mc.player.getPosition(partialTick).y) + 0.02;
		int r = radius.getInt();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				ChunkPos c = new ChunkPos(here.x + dx, here.z + dz);
				boolean current = dx == 0 && dz == 0;
				int color = current ? OogaTheme.accent(0xE0) : ColorUtil.withAlpha(0xFFFFFF, 0x50);
				double x0 = c.getMinBlockX(), z0 = c.getMinBlockZ(), x1 = x0 + 16, z1 = z0 + 16;
				drawer.line(x0, y, z0, x1, y, z0, color);
				drawer.line(x1, y, z0, x1, y, z1, color);
				drawer.line(x1, y, z1, x0, y, z1, color);
				drawer.line(x0, y, z1, x0, y, z0, color);
				if (current) {
					// Corner pillars on your own chunk.
					double top = y + 24, bottom = y - 24;
					drawer.line(x0, bottom, z0, x0, top, z0, color);
					drawer.line(x1, bottom, z0, x1, top, z0, color);
					drawer.line(x1, bottom, z1, x1, top, z1, color);
					drawer.line(x0, bottom, z1, x0, top, z1, color);
				}
			}
		}
	}
}
