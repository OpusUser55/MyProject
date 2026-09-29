package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.TracerOrigin;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.BlockEntityTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Highlights containers and utility blocks through walls, each kind in its own colour. */
public class StorageEspModule extends Module {
	private enum Kind {
		CHEST(0xD9A441), BARREL(0xB07A45), SHULKER(0xB57EDC), ENDER_CHEST(0x4FC3A8),
		HOPPER(0x8A96A8), FURNACE(0xC9CBD1), DISPENSER(0x9FB26A), BREWING(0xE0717A), CRAFTER(0x7DA4E0);

		final int rgb;

		Kind(int rgb) {
			this.rgb = rgb;
		}
	}

	public final BooleanSetting chests = add(new BooleanSetting("Chests", "Chests and trapped chests.", true));
	public final BooleanSetting barrels = add(new BooleanSetting("Barrels", "Barrels.", true));
	public final BooleanSetting shulkers = add(new BooleanSetting("Shulkers", "Shulker boxes.", true));
	public final BooleanSetting enderChests = add(new BooleanSetting("Ender Chests", "Ender chests: almost always player-placed.", true));
	public final BooleanSetting hoppers = add(new BooleanSetting("Hoppers", "Hoppers.", true));
	public final BooleanSetting furnaces = add(new BooleanSetting("Furnaces", "Furnaces, smokers and blast furnaces.", false));
	public final BooleanSetting dispensers = add(new BooleanSetting("Dispensers", "Dispensers and droppers.", false));
	public final BooleanSetting brewing = add(new BooleanSetting("Brewing Stands", "Brewing stands.", false));
	public final BooleanSetting crafters = add(new BooleanSetting("Crafters", "Crafters.", false));
	public final ModeSetting style = add(new ModeSetting("Style", "How each block is drawn.", "Both", "Both", "Outline", "Fill"));
	public final NumberSetting fillOpacity = add(new NumberSetting("Fill Opacity", "Opacity of the filled box.", 0.18, 0.05, 0.6, 0.01)
			.visibleWhen(() -> !style.is("Outline")));
	public final BooleanSetting tracers = add(new BooleanSetting("Tracers", "Lines from your view to each block.", false));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance.", 128, 16, 512, 8, "m"));

	private int shown;

	public StorageEspModule() {
		super("Storage ESP", "Chests, shulkers, barrels and more, visible through walls.", Category.BASEFINDING);
		WorldOverlay.register(this::draw);
	}

	private Kind kindOf(BlockEntity be) {
		if (be instanceof ShulkerBoxBlockEntity) return shulkers.get() ? Kind.SHULKER : null;
		if (be instanceof EnderChestBlockEntity) return enderChests.get() ? Kind.ENDER_CHEST : null;
		if (be instanceof ChestBlockEntity) return chests.get() ? Kind.CHEST : null;
		if (be instanceof BarrelBlockEntity) return barrels.get() ? Kind.BARREL : null;
		if (be instanceof HopperBlockEntity) return hoppers.get() ? Kind.HOPPER : null;
		if (be instanceof AbstractFurnaceBlockEntity) return furnaces.get() ? Kind.FURNACE : null;
		if (be instanceof DispenserBlockEntity) return dispensers.get() ? Kind.DISPENSER : null;
		if (be instanceof BrewingStandBlockEntity) return brewing.get() ? Kind.BREWING : null;
		if (be instanceof CrafterBlockEntity) return crafters.get() ? Kind.CRAFTER : null;
		return null;
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null) return;
		double maxSq = range.get() * range.get();
		Vec3 eye = mc.player.getEyePosition(partialTick);
		Vec3 tracerStart = TracerOrigin.get(drawer, partialTick);
		int count = 0;
		for (BlockEntity be : BlockEntityTracker.all()) {
			Kind kind = kindOf(be);
			if (kind == null || be.isRemoved()) continue;
			BlockPos pos = be.getBlockPos();
			if (pos.distToCenterSqr(eye) > maxSq) continue;
			// Chests and ender chests are slightly smaller than a full block.
			double inset = kind == Kind.CHEST || kind == Kind.ENDER_CHEST ? 1 / 16.0 : 0;
			AABB box = new AABB(pos.getX() + inset, pos.getY(), pos.getZ() + inset,
					pos.getX() + 1 - inset, pos.getY() + (inset > 0 ? 14 / 16.0 : 1), pos.getZ() + 1 - inset);
			int fill = style.is("Outline") ? 0 : ColorUtil.withAlpha(kind.rgb, Math.round(255 * fillOpacity.getFloat()));
			int line = style.is("Fill") ? 0 : ColorUtil.withAlpha(kind.rgb, 220);
			drawer.box(box, fill, line);
			if (tracers.get()) drawer.line(tracerStart, box.getCenter(), ColorUtil.withAlpha(kind.rgb, 160));
			count++;
		}
		shown = count;
	}

	@Override
	public String getSuffix() {
		return Integer.toString(shown);
	}
}
