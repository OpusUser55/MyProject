package dev.ooga.client.module.impl.world;

import dev.ooga.client.mixin.MultiPlayerGameModeAccessor;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;

/** Removes the 5-tick pause vanilla adds between breaking one block and starting the next. */
public class FastBreakModule extends Module {
	public FastBreakModule() {
		super("Fast Break", "No pause between blocks when mining.", Category.WORLD);
	}

	@Override
	public void onTick() {
		if (mc.gameMode != null) ((MultiPlayerGameModeAccessor) mc.gameMode).ooga$setDestroyDelay(0);
	}
}
