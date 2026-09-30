package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;

/** Sets the time of day on your screen only: permanent day for base hunting, or night for looks. */
public class TimeChangerModule extends Module {
	public final NumberSetting time = add(new NumberSetting("Time", "0 sunrise, 6000 noon, 13000 night, 18000 midnight.", 6000, 0, 23999, 500));

	public TimeChangerModule() {
		super("Time Changer", "Change the time of day on your screen.", Category.RENDER);
	}

	@Override
	public void onTick() {
		if (mc.level != null) mc.level.getLevelData().setDayTime(time.getInt());
	}
}
