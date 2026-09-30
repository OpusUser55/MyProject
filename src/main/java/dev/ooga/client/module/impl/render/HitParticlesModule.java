package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Extra crit / sharpness particles on every hit you land. Client-side only. */
public class HitParticlesModule extends Module {
	public final ModeSetting type = add(new ModeSetting("Particles", "Which particles to show.", "Both", "Crit", "Magic", "Both"));
	public final NumberSetting amount = add(new NumberSetting("Amount", "How many bursts per hit.", 2, 1, 6, 1, "x"));

	public HitParticlesModule() {
		super("Hit Particles", "More particles when you hit something.", Category.RENDER);
	}

	/** Called right after an attack packet is sent. */
	public void onHit(Entity target) {
		if (!isEnabled() || !(target instanceof LivingEntity)) return;
		for (int i = 0; i < amount.getInt(); i++) {
			if (!type.is("Magic")) mc.particleEngine.createTrackingEmitter(target, ParticleTypes.CRIT);
			if (!type.is("Crit")) mc.particleEngine.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT);
		}
	}
}
