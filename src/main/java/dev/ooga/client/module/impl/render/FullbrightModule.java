package dev.ooga.client.module.impl.render;

import dev.ooga.client.mixin.OptionInstanceAccessor;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.client.OptionInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class FullbrightModule extends Module {
	public final ModeSetting mode = add(new ModeSetting("Mode", "Gamma brightens the lightmap; Night Vision applies the effect locally.", "Gamma", "Gamma", "Night Vision"));
	public final NumberSetting brightness = add(new NumberSetting("Brightness", "Gamma level while enabled.", 12.0, 1.0, 16.0, 0.5)
			.visibleWhen(() -> mode.is("Gamma")));
	public final BooleanSetting smooth = add(new BooleanSetting("Smooth", "Fade brightness in and out instead of snapping.", true)
			.visibleWhen(() -> mode.is("Gamma")));

	/** Long enough to never visibly run out; also how we recognise our own effect. */
	private static final int EFFECT_TICKS = 1_000_000;

	/** The user's own gamma while we're overriding it (or fading back to it); null otherwise. */
	private Double savedGamma;
	private double current;

	public FullbrightModule() {
		super("Fullbright", "See clearly in the dark without potions.", Category.RENDER);
	}

	@Override
	protected void onEnable() {
		// If a fade-out is still running, savedGamma already holds the user's own value.
		if (savedGamma == null) {
			savedGamma = mc.options.gamma().get();
			current = savedGamma;
		}
	}

	@Override
	public void onTick() {
		if (mode.is("Night Vision")) {
			restoreGammaNow();
			if (mc.player != null && !mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
				mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_TICKS, 0, false, false));
			}
			return;
		}
		removeEffect();
		if (savedGamma == null) {
			savedGamma = mc.options.gamma().get();
			current = savedGamma;
		}
		step(brightness.get());
	}

	@Override
	protected void onDisable() {
		removeEffect();
		if (!smooth.get() || !mode.is("Gamma")) restoreGammaNow();
	}

	@Override
	public void onDisabledTick() {
		if (savedGamma == null) return;
		step(savedGamma);
		if (current == savedGamma) savedGamma = null;
	}

	/** Moves gamma toward the target, gradually when Smooth is on. */
	private void step(double target) {
		if (!smooth.get()) current = target;
		else current += (target - current) * 0.35;
		if (Math.abs(target - current) < 0.02) current = target;
		applyGamma(current);
	}

	private void restoreGammaNow() {
		if (savedGamma != null) applyGamma(savedGamma);
		savedGamma = null;
	}

	private void removeEffect() {
		if (mc.player == null) return;
		MobEffectInstance effect = mc.player.getEffect(MobEffects.NIGHT_VISION);
		// Only remove the effect we added (ours is invisible and effectively infinite).
		if (effect != null && !effect.isVisible() && effect.getDuration() > EFFECT_TICKS - 20 * 60 * 60) mc.player.removeEffect(MobEffects.NIGHT_VISION);
	}

	/**
	 * Puts the user's own gamma back without changing the module's saved state, so the game
	 * never writes an out-of-range brightness to options.txt on shutdown.
	 */
	public void restoreGamma() {
		if (savedGamma != null) applyGamma(savedGamma);
	}

	/** The option's validator caps gamma at 1.0, so write the backing value directly. */
	private void applyGamma(double value) {
		OptionInstance<Double> gamma = mc.options.gamma();
		((OptionInstanceAccessor) (Object) gamma).ooga$setValue(value);
	}
}
