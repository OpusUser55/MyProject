package dev.ooga.client.camera;

import dev.ooga.client.module.impl.render.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A free-flying camera. Position advances once per tick with eased velocity and is
 * interpolated every frame, so motion is smooth at any frame rate. Rotation is applied
 * immediately from mouse input, like vanilla, so looking around never lags.
 */
public final class FreeCamera implements CameraMode {
	/** Blocks per tick at speed 1.0 (about 8 blocks per second). */
	private static final double BASE_SPEED = 0.4;
	private static final double HALF_EXTENT = 0.12;

	private final FreecamModule settings;
	private Vec3 position = Vec3.ZERO;
	private Vec3 previousPosition = Vec3.ZERO;
	private Vec3 velocity = Vec3.ZERO;
	private float yaw;
	private float pitch;

	public FreeCamera(FreecamModule settings) {
		this.settings = settings;
	}

	@Override
	public void begin(Vec3 eyePosition, float yaw, float pitch) {
		this.position = eyePosition;
		this.previousPosition = eyePosition;
		this.velocity = Vec3.ZERO;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	@Override
	public void tick() {
		previousPosition = position;
		Vec3 target = desiredVelocity();
		double smoothing = settings.smoothing.get();
		velocity = velocity.lerp(target, 1.0 - smoothing);
		if (velocity.lengthSqr() < 1.0E-6 && target.lengthSqr() == 0) velocity = Vec3.ZERO;
		position = move(position, velocity);
	}

	private Vec3 desiredVelocity() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null) return Vec3.ZERO;
		Options o = mc.options;

		double forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
		double strafe = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
		double vertical = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);

		double yawRad = Math.toRadians(yaw);
		double sin = Math.sin(yawRad);
		double cos = Math.cos(yawRad);

		Vec3 horizontal;
		if (settings.flightMode.is("Look")) {
			// Forward follows the view direction, including pitch, like spectator flight.
			double pitchRad = Math.toRadians(pitch);
			double cp = Math.cos(pitchRad);
			Vec3 look = new Vec3(-sin * cp, -Math.sin(pitchRad), cos * cp);
			Vec3 side = new Vec3(cos, 0, sin);
			horizontal = look.scale(forward).add(side.scale(strafe));
		} else {
			horizontal = new Vec3(strafe * cos - forward * sin, 0, forward * cos + strafe * sin);
		}
		if (horizontal.lengthSqr() > 1) horizontal = horizontal.normalize();

		double speed = BASE_SPEED * settings.speed.get();
		if (o.keySprint.isDown()) speed *= settings.sprintBoost.get();
		Vec3 up = new Vec3(0, vertical * settings.verticalSpeed.get(), 0);
		return horizontal.add(up).scale(speed);
	}

	private Vec3 move(Vec3 from, Vec3 motion) {
		if (motion.lengthSqr() == 0) return from;
		if (!settings.collision.is("Blocks")) return from.add(motion);

		// Resolve each axis separately so the camera slides along walls instead of sticking.
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return from.add(motion);
		double x = from.x, y = from.y, z = from.z;
		if (free(mc, x, y + motion.y, z)) y += motion.y;
		else velocity = new Vec3(velocity.x, 0, velocity.z);
		if (free(mc, x + motion.x, y, z)) x += motion.x;
		else velocity = new Vec3(0, velocity.y, velocity.z);
		if (free(mc, x, y, z + motion.z)) z += motion.z;
		else velocity = new Vec3(velocity.x, velocity.y, 0);
		return new Vec3(x, y, z);
	}

	private static boolean free(Minecraft mc, double x, double y, double z) {
		AABB box = new AABB(x - HALF_EXTENT, y - HALF_EXTENT, z - HALF_EXTENT, x + HALF_EXTENT, y + HALF_EXTENT, z + HALF_EXTENT);
		return mc.level.noCollision(box);
	}

	@Override
	public void turn(double deltaYaw, double deltaPitch) {
		// Same 0.15 factor vanilla applies in Entity.turn, times our own sensitivities.
		yaw += (float) (deltaYaw * 0.15 * settings.horizontalSensitivity.get());
		pitch = Mth.clamp(pitch + (float) (deltaPitch * 0.15 * settings.verticalSensitivity.get()), -90f, 90f);
	}

	@Override
	public Vec3 position(float partialTick) {
		return previousPosition.lerp(position, partialTick);
	}

	@Override
	public float yaw(float partialTick) {
		return yaw;
	}

	@Override
	public float pitch(float partialTick) {
		return pitch;
	}

	@Override
	public boolean renderPlayerBody() {
		return settings.showPlayer.get();
	}

	@Override
	public boolean renderHand() {
		return settings.showHand.get();
	}

	@Override
	public boolean lightHandAtCamera() {
		return settings.cameraLighting.get();
	}

	@Override
	public boolean allowInteraction() {
		return settings.allowInteraction.get();
	}

	/** Moves the camera back to the player's eyes without leaving Freecam. */
	public void recenter() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		begin(mc.player.getEyePosition(), mc.player.getYRot(), mc.player.getXRot());
	}
}
