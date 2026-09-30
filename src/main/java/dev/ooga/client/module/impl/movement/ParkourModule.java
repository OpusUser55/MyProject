package dev.ooga.client.module.impl.movement;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.util.Keys;

/** Jumps at the last moment before you run off an edge, for longer gaps. */
public class ParkourModule extends Module {
	public final BooleanSetting sprintOnly = add(new BooleanSetting("Sprinting Only", "Only jump while sprinting.", true));

	private boolean jumping;

	public ParkourModule() {
		super("Parkour", "Jumps automatically at the edge of blocks.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		if (jumping) {
			// One tick of jump is enough; hand the key back.
			jumping = false;
			Keys.release(mc.options.keyJump);
		}
		if (mc.player == null || mc.screen != null || mc.player.isShiftKeyDown()) return;
		if (sprintOnly.get() && !mc.player.isSprinting()) return;
		if (Edges.approaching(1.0, 0.6)) {
			Keys.hold(mc.options.keyJump);
			jumping = true;
		}
	}

	@Override
	protected void onDisable() {
		if (jumping) Keys.release(mc.options.keyJump);
		jumping = false;
	}
}
