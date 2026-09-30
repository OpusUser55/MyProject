package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * "Double hand": when you're low or a crystal or charged anchor is right next to you, switch
 * your hotbar to a totem, so you hold one in both hands and survive a double pop.
 */
public class AutoDoubleHandModule extends Module {
	public final NumberSetting health = add(new NumberSetting("Health", "Switch at or below this much health.", 8, 1, 20, 1));
	public final BooleanSetting nearCrystals = add(new BooleanSetting("Near Explosives", "Also switch when a crystal or charged anchor is within 5 blocks.", true));

	public AutoDoubleHandModule() {
		super("Auto Double Hand", "Holds a totem in your main hand when you're in danger.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null || mc.screen != null) return;
		if (mc.player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) return;
		boolean danger = mc.player.getHealth() <= health.getInt() || (nearCrystals.get() && explosivesNearby());
		if (!danger) return;
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
				inventory.setSelectedSlot(i);
				return;
			}
		}
	}

	private boolean explosivesNearby() {
		AABB area = mc.player.getBoundingBox().inflate(5);
		if (!mc.level.getEntities((Entity) null, area, e -> e instanceof EndCrystal).isEmpty()) return true;
		var origin = mc.player.blockPosition();
		for (var pos : net.minecraft.core.BlockPos.betweenClosed(origin.offset(-4, -2, -4), origin.offset(4, 3, 4))) {
			var state = mc.level.getBlockState(pos);
			if (state.is(Blocks.RESPAWN_ANCHOR) && state.getValue(net.minecraft.world.level.block.RespawnAnchorBlock.CHARGE) > 0) return true;
		}
		return false;
	}
}
