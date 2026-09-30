package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.Delay;

/**
 * Jumps the instant you take a hit on the ground. Jumping as knockback lands cancels part of
 * it (the "jump reset" players do by hand), so you get knocked back less.
 */
public class JumpResetModule extends Module {
	public final NumberSetting chance = add(new NumberSetting("Chance", "Percent of hits to jump-reset.", 100, 0, 100, 5, "%"));

	private int lastHurtTime;

	public JumpResetModule() {
		super("Auto Jump Reset", "Jumps as you get hit to take less knockback.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.screen != null) return;
		int hurt = mc.player.hurtTime;
		// hurtTime jumps to its maximum on the tick a hit lands, then counts down.
		boolean justHit = hurt > lastHurtTime && hurt == mc.player.hurtDuration;
		lastHurtTime = hurt;
		if (justHit && mc.player.onGround() && !mc.player.isInWater() && Delay.chance(chance.get())) {
			mc.player.jumpFromGround();
		}
	}
}
