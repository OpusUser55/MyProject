package dev.ooga.client.module.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;

/**
 * Temporary hotbar switch for one hit: swap to a weapon right before an attack packet goes out,
 * then back to what you were holding on the next tick.
 */
final class WeaponSwap {
	private int returnSlot = -1;
	private int ticks;

	/** Selects {@code slot} now and remembers where to go back to. */
	void swapTo(int slot) {
		Inventory inventory = Minecraft.getInstance().player.getInventory();
		if (inventory.getSelectedSlot() == slot) return;
		if (returnSlot < 0) returnSlot = inventory.getSelectedSlot();
		inventory.setSelectedSlot(slot);
		ticks = 2;
	}

	/** Call every tick; switches back once the hit has gone out. */
	void tick() {
		if (returnSlot < 0 || --ticks > 0) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) mc.player.getInventory().setSelectedSlot(returnSlot);
		returnSlot = -1;
	}

	/** Switches back right away, if a swap is pending. */
	void cancel() {
		ticks = 1;
		tick();
	}
}
