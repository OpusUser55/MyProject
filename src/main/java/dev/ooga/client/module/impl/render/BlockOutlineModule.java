package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Replaces vanilla's thin black block outline with an accent-coloured box and light fill. */
public class BlockOutlineModule extends Module {
	private static BlockOutlineModule instance;

	public final NumberSetting fill = add(new NumberSetting("Fill", "Opacity of the fill.", 0.12, 0.0, 0.5, 0.01));

	public BlockOutlineModule() {
		super("Block Outline", "An accent-coloured outline on the block you look at.", Category.RENDER);
		instance = this;
		WorldOverlay.register(this::draw);
	}

	/** Whether the vanilla outline should be hidden. */
	public static boolean replacesVanilla() {
		return instance != null && instance.isEnabled();
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
		BlockPos pos = hit.getBlockPos();
		VoxelShape shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
		if (shape.isEmpty()) return;
		AABB box = shape.bounds().move(pos).inflate(0.002);
		drawer.box(box, ColorUtil.withAlpha(OogaTheme.GOLD, Math.round(255 * fill.getFloat())), OogaTheme.accent(0xE6));
	}
}
