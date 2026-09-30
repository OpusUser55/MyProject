package dev.ooga.client.module.impl.movement;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.util.Keys;

/** Holds W for you. Pressing S (or opening the menu and turning it off) stops it. */
public class AutoWalkModule extends Module {
	public final BooleanSetting stopOnBack = add(new BooleanSetting("Stop On Back", "Turn off when you press the back key.", true));

	public AutoWalkModule() {
		super("Auto Walk", "Keeps walking forward on its own.", Category.MOVEMENT);
	}

	@Override
	protected boolean canEnable() {
		return inWorld();
	}

	@Override
	public boolean persistsEnabledState() {
		return false;
	}

	@Override
	public void onTick() {
		if (!inWorld()) {
			setEnabled(false, false);
			return;
		}
		if (stopOnBack.get() && Keys.physicallyDown(mc.options.keyDown) && mc.screen == null) {
			setEnabled(false);
			return;
		}
		Keys.hold(mc.options.keyUp);
	}

	@Override
	protected void onDisable() {
		Keys.release(mc.options.keyUp);
	}
}
