package dev.ooga.client.module.impl.movement;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import net.minecraft.client.player.LocalPlayer;

public class SprintModule extends Module {
	public SprintModule() {
		super("Sprint", "Sprint automatically whenever you move forward.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		LocalPlayer player = mc.player;
		if (player == null) return;
		// Reads the player's own input rather than the sprint key, so a locked input (Freecam)
		// simply means "not moving" and the camera's sprint boost isn't triggered by accident.
		boolean canSprint = player.input.hasForwardImpulse()
				&& !player.isShiftKeyDown()
				&& !player.isUsingItem()
				&& !player.horizontalCollision
				&& (player.getFoodData().getFoodLevel() > 6 || player.getAbilities().mayfly);
		if (canSprint && !player.isSprinting()) player.setSprinting(true);
	}
}
