package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** Throws a splash healing potion at your feet when you're low, then looks back up. */
public class AutoPotModule extends Module {
	public final NumberSetting health = add(new NumberSetting("Health", "Throw at or below this much health.", 8, 2, 19, 1));
	public final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Least time between potions.", 1.0, 0.25, 5.0, 0.25, "s"));

	private long last;

	public AutoPotModule() {
		super("Auto Pot", "Throws healing potions when you're low.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null || mc.screen != null || mc.player.getHealth() > health.getInt()) return;
		if (System.currentTimeMillis() - last < cooldown.get() * 1000) return;
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (!isHealing(inventory.getItem(i))) continue;
			int previous = inventory.getSelectedSlot();
			float pitch = mc.player.getXRot();
			inventory.setSelectedSlot(i);
			// The use packet carries our rotation, so looking down just for this call is enough.
			mc.player.setXRot(90f);
			mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
			mc.player.setXRot(pitch);
			inventory.setSelectedSlot(previous);
			last = System.currentTimeMillis();
			return;
		}
	}

	private static boolean isHealing(ItemStack stack) {
		if (!stack.is(Items.SPLASH_POTION)) return false;
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		return contents != null && (contents.is(Potions.HEALING) || contents.is(Potions.STRONG_HEALING));
	}
}
