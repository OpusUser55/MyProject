package dev.ooga.client.module.impl.movement;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.SafetyModule;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Walk straight up full blocks (or more) without jumping. */
public class StepModule extends Module {
	private static final double VANILLA = 0.6;

	public final NumberSetting height = add(new NumberSetting("Height", "How tall a step you can walk up.", 1.0, 0.6, 2.5, 0.1, "m"));

	public StepModule() {
		super("Step", "Walk up blocks without jumping.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		AttributeInstance step = attribute();
		double wanted = SafetyModule.atMost(height.get(), 1.0);
		if (step != null && step.getBaseValue() != wanted) step.setBaseValue(wanted);
	}

	@Override
	public void onPausedTick() {
		onDisable();
	}

	@Override
	protected void onDisable() {
		AttributeInstance step = attribute();
		if (step != null) step.setBaseValue(VANILLA);
	}

	private AttributeInstance attribute() {
		return mc.player == null ? null : mc.player.getAttribute(Attributes.STEP_HEIGHT);
	}

	@Override
	public String getSuffix() {
		return height.format();
	}

	@Override
	public boolean isBlatant() {
		return true;
	}
}
