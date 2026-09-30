package dev.ooga.client.module.impl.misc;

import dev.ooga.client.mixin.MinecraftAccessor;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.SafetyModule;
import dev.ooga.client.module.setting.NumberSetting;

/** Shortens the 4-tick pause between block placements (and item uses) while holding right click. */
public class FastPlaceModule extends Module {
	public final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between placements. Vanilla is 4.", 0, 0, 3, 1));

	public FastPlaceModule() {
		super("Fast Place", "Place blocks as fast as you can hold right click.", Category.MISC);
	}

	@Override
	public void onTick() {
		MinecraftAccessor accessor = (MinecraftAccessor) mc;
		int wanted = (int) SafetyModule.atLeast(delay.getInt(), 1);
		if (accessor.ooga$getRightClickDelay() > wanted) accessor.ooga$setRightClickDelay(wanted);
	}

	@Override
	public String getSuffix() {
		return delay.getInt() + "t";
	}

	@Override
	public boolean isBlatant() {
		return true;
	}
}
