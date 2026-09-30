package dev.ooga.client.module.impl.combat;

import dev.ooga.client.mixin.MinecraftAccessor;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.SafetyModule;

/** Removes the half-second lockout vanilla adds after you swing at nothing. */
public class NoHitDelayModule extends Module {
	public NoHitDelayModule() {
		super("No Hit Delay", "Missed swings no longer stop you attacking for 10 ticks.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		((MinecraftAccessor) mc).ooga$setMissTime(0);
	}

	@Override
	public boolean isBlatant() {
		return true;
	}
}
