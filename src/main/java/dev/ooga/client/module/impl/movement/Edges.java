package dev.ooga.client.module.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared "am I about to walk off something" check for Safe Walk and Parkour. */
final class Edges {
	private Edges() {
	}

	/**
	 * True when the player is on the ground and, after {@code ticksAhead} ticks at the current
	 * velocity, nothing would be under their feet within {@code drop} blocks.
	 */
	static boolean approaching(double ticksAhead, double drop) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || !player.onGround()) return false;
		Vec3 v = player.getDeltaMovement();
		if (v.x * v.x + v.z * v.z < 1.0E-4) return false;
		AABB ahead = player.getBoundingBox().move(v.x * ticksAhead, -drop, v.z * ticksAhead);
		// Only the slice below the feet matters; a wall ahead isn't an edge.
		AABB floor = new AABB(ahead.minX, ahead.minY, ahead.minZ, ahead.maxX, player.getBoundingBox().minY - 0.01, ahead.maxZ);
		return mc.level.noCollision(player, floor);
	}

	/** True when standing within {@code reach} blocks of a drop of at least {@code drop}, in any direction. */
	static boolean near(double reach, double drop) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || !player.onGround()) return false;
		AABB feet = player.getBoundingBox();
		double[][] offsets = {{reach, 0}, {-reach, 0}, {0, reach}, {0, -reach}};
		for (double[] o : offsets) {
			AABB shifted = feet.move(o[0], 0, o[1]);
			AABB floor = new AABB(shifted.minX, feet.minY - drop, shifted.minZ, shifted.maxX, feet.minY - 0.01, shifted.maxZ);
			if (mc.level.noCollision(player, floor)) return true;
		}
		return false;
	}
}
