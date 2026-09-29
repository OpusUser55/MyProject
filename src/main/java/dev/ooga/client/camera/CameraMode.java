package dev.ooga.client.camera;

import net.minecraft.world.phys.Vec3;

/**
 * A source of camera transforms that can replace the normal player viewpoint.
 *
 * <p>The camera is deliberately independent of the player: a mode owns only where the eye is
 * and where it looks. What the player does, how the hand is drawn and whether the body is
 * visible are separate concerns answered by the controller, so each can change without
 * dragging the others along.
 */
public interface CameraMode {
	/** Called when the mode becomes active; seed the transform from the current view. */
	void begin(Vec3 eyePosition, float yaw, float pitch);

	/** Called once per client tick while active. */
	void tick();

	/** Raw mouse-look deltas (as passed to {@code Entity.turn}). */
	void turn(double deltaYaw, double deltaPitch);

	Vec3 position(float partialTick);

	float yaw(float partialTick);

	float pitch(float partialTick);

	/** Whether the local player's body should be drawn in the world while this mode is active. */
	boolean renderPlayerBody();

	/** Whether the first-person hand and held item should stay on screen. */
	boolean renderHand();

	/** Whether hand/item lighting should be sampled at the camera instead of at the player. */
	boolean lightHandAtCamera();

	/** Whether the player may attack, mine and use items while the camera is detached. */
	boolean allowInteraction();
}
