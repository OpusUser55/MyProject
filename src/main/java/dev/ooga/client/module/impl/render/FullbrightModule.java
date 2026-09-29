package dev.ooga.client.module.impl.render;

import dev.ooga.client.mixin.OptionInstanceAccessor;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.client.OptionInstance;

public class FullbrightModule extends Module {
	public final NumberSetting brightness = add(new NumberSetting("Brightness", "Gamma level while enabled.", 12.0, 1.0, 16.0, 0.5));

	private Double savedGamma;

	public FullbrightModule() {
		super("Fullbright", "See clearly in the dark without potions.", Category.RENDER);
	}

	@Override
	protected void onEnable() {
		savedGamma = mc.options.gamma().get();
		applyGamma(brightness.get());
	}

	@Override
	public void onTick() {
		// Re-assert in case the video settings slider changed it.
		if (mc.options.gamma().get() != brightness.get().doubleValue()) applyGamma(brightness.get());
	}

	@Override
	protected void onDisable() {
		applyGamma(savedGamma == null ? 0.5 : savedGamma);
		savedGamma = null;
	}

	/**
	 * Puts the user's own gamma back without changing the module's saved state, so the game
	 * never writes an out-of-range brightness to options.txt on shutdown.
	 */
	public void restoreGamma() {
		if (isEnabled() && savedGamma != null) applyGamma(savedGamma);
	}

	/** The option's validator caps gamma at 1.0, so write the backing value directly. */
	private void applyGamma(double value) {
		OptionInstance<Double> gamma = mc.options.gamma();
		((OptionInstanceAccessor) (Object) gamma).ooga$setValue(value);
	}
}
