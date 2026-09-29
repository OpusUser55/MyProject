package dev.ooga.client.camera;

import dev.ooga.client.module.impl.render.FreeLookModule;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Free Look: a third-person camera that orbits the player on its own rotation. The mouse turns
 * the camera, not the player, so you can look around while walking (or flying) straight on.
 * The camera is pulled in when a block is between it and the player, like vanilla's.
 */
public final class OrbitCamera implements CameraMode {
	private final FreeLookModule settings;
	private float yaw;
	private float pitch;

	public OrbitCamera(FreeLookModule settings) {
		this.settings = settings;
	}

	@Override
	public void begin(Vec3 eyePosition, float yaw, float pitch) {
		this.yaw = yaw;
		this.pitch = pitch;
	}

	@Override
	public void tick() {
	}

	@Override
	public void turn(double deltaYaw, double deltaPitch) {
		yaw += (float) (deltaYaw * 0.15 * settings.sensitivity.get());
		pitch = Mth.clamp(pitch + (float) (deltaPitch * 0.15 * settings.sensitivity.get()), -90f, 90f);
	}

	@Override
	public Vec3 position(float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return Vec3.ZERO;
		Vec3 eye = mc.player.getEyePosition(partialTick);
		Vec3 back = Vec3.directionFromRotation(pitch, yaw).scale(-settings.distance.get());
		Vec3 target = eye.add(back);
		BlockHitResult hit = mc.level.clip(new ClipContext(eye, target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
		if (hit.getType() == HitResult.Type.MISS) return target;
		// Stop just short of the wall so the near plane doesn't cut into it.
		Vec3 toWall = hit.getLocation().subtract(eye);
		double length = toWall.length();
		return length < 0.3 ? eye : eye.add(toWall.scale((length - 0.2) / length));
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
		return true;
	}

	@Override
	public boolean renderHand() {
		return false;
	}

	@Override
	public boolean lightHandAtCamera() {
		return false;
	}

	@Override
	public boolean allowInteraction() {
		return true;
	}

	@Override
	public boolean lockPlayer() {
		return false;
	}
}
