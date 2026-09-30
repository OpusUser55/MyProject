package dev.ooga.client.module.impl.movement;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.Keys;

/**
 * Sneaks for you while you're moving near the edge of a drop, so vanilla's sneak edge-stop
 * keeps you on the block. Bridging and walking along roofs without holding shift.
 */
public class SafeWalkModule extends Module {
	public final NumberSetting drop = add(new NumberSetting("Min Drop", "Only guard drops at least this deep.", 1.0, 0.6, 5.0, 0.1, "m"));

	private boolean sneaking;

	public SafeWalkModule() {
		super("Safe Walk", "Stops you walking off edges.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		boolean moving = mc.player != null && mc.screen == null && (Keys.physicallyDown(mc.options.keyUp)
				|| Keys.physicallyDown(mc.options.keyDown) || Keys.physicallyDown(mc.options.keyLeft)
				|| Keys.physicallyDown(mc.options.keyRight) || mc.options.keyUp.isDown());
		if (moving && Edges.near(0.5, drop.get())) {
			Keys.hold(mc.options.keyShift);
			sneaking = true;
		} else {
			stop();
		}
	}

	private void stop() {
		if (!sneaking) return;
		sneaking = false;
		Keys.release(mc.options.keyShift);
	}

	@Override
	protected void onDisable() {
		stop();
	}
}
