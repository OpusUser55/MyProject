package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

/** Fires rockets from your hotbar while you glide, whenever you slow down (or on a timer). */
public class AutoFireworkModule extends Module {
	public final NumberSetting minSpeed = add(new NumberSetting("Min Speed", "Boost when you drop below this speed.", 20, 5, 40, 1, "b/s"));
	public final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Least time between rockets.", 1.5, 0.5, 10, 0.5, "s"));
	public final BooleanSetting switchBack = add(new BooleanSetting("Switch Back", "Go back to your slot after firing.", true));

	private long lastRocket;

	public AutoFireworkModule() {
		super("Auto Firework", "Keeps your elytra flight going with rockets.", Category.MISC);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null || mc.screen != null || !mc.player.isFallFlying()) return;
		long now = System.currentTimeMillis();
		if (now - lastRocket < cooldown.get() * 1000) return;
		double speed = mc.player.getDeltaMovement().length() * 20;
		if (speed >= minSpeed.get()) return;
		Inventory inventory = mc.player.getInventory();
		int slot = -1;
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (inventory.getItem(i).is(Items.FIREWORK_ROCKET)) {
				slot = i;
				break;
			}
		}
		if (slot < 0) return;
		int previous = inventory.getSelectedSlot();
		inventory.setSelectedSlot(slot);
		mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
		if (switchBack.get()) inventory.setSelectedSlot(previous);
		lastRocket = now;
	}
}
