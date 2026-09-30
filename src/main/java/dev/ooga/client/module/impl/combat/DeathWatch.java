package dev.ooga.client.module.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Notices other players dying nearby, so explosive macros can hold off and not blow up the
 * loot that just dropped.
 */
final class DeathWatch {
	private long pausedUntil;

	/** Call every tick; returns true while paused after a nearby death. */
	boolean paused(boolean enabled, double range, double seconds) {
		if (!enabled) return false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return false;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity instanceof Player other && other != mc.player && other.isDeadOrDying() && other.distanceTo(mc.player) <= range) {
				pausedUntil = System.currentTimeMillis() + Math.round(seconds * 1000);
			}
		}
		return System.currentTimeMillis() < pausedUntil;
	}
}
