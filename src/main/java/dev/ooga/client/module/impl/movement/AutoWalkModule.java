package dev.ooga.client.module.impl.movement;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;

/** Holds the forward key for you. Great for long nether-highway or tunnel trips. */
public class AutoWalkModule extends Module {
	private boolean holding;

	public AutoWalkModule() {
		super("Auto Walk", "Keeps walking forward.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		mc.options.keyUp.setDown(true);
		holding = true;
	}

	@Override
	protected void onDisable() {
		if (holding) mc.options.keyUp.setDown(false);
		holding = false;
	}
}
