package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

/**
 * Removes a crystal on your screen the instant you hit it, instead of waiting for the server
 * to confirm, so the next crystal can go down right away.
 */
public class CrystalOptimizerModule extends Module {
	public CrystalOptimizerModule() {
		super("Crystal Optimizer", "Crystals vanish the moment you hit them, for faster crystal PvP.", Category.COMBAT);
	}

	/** Called right after an attack packet is sent. */
	public void afterAttack(Entity target) {
		if (!isEnabled() || mc.level == null || !(target instanceof EndCrystal)) return;
		mc.level.removeEntity(target.getId(), Entity.RemovalReason.KILLED);
	}
}
