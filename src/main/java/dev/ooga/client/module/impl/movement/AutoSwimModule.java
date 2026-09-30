package dev.ooga.client.module.impl.movement;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.util.Keys;

/** Holds jump in water and lava so you float instead of sinking. Sneak to dive. */
public class AutoSwimModule extends Module {
	private boolean holding;

	public AutoSwimModule() {
		super("Auto Swim", "Keeps you afloat in water and lava.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		boolean want = mc.player != null && mc.screen == null
				&& (mc.player.isInWater() || mc.player.isInLava())
				&& !Keys.physicallyDown(mc.options.keyShift);
		if (want) {
			Keys.hold(mc.options.keyJump);
			holding = true;
		} else if (holding) {
			holding = false;
			Keys.release(mc.options.keyJump);
		}
	}

	@Override
	protected void onDisable() {
		if (holding) Keys.release(mc.options.keyJump);
		holding = false;
	}
}
