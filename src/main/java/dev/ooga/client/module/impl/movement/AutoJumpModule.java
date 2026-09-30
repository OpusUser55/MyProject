package dev.ooga.client.module.impl.movement;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.util.Keys;

/** Jumps whenever you're moving on the ground, for sprint-jumping without spamming space. */
public class AutoJumpModule extends Module {
	private boolean holding;

	public AutoJumpModule() {
		super("Auto Jump", "Sprint-jumps continuously while you move.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		boolean want = mc.player != null && mc.screen == null
				&& mc.player.input.hasForwardImpulse() && !mc.player.isInWater() && !mc.player.isShiftKeyDown();
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
