package dev.ooga.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Projects world positions onto the HUD, for labels that sit over things in the world.
 *
 * <p>The camera transform and FOV are captured from the frame being rendered (after Freecam,
 * Freelook and Zoom have had their say), so labels track what's actually on screen. View
 * bobbing and hurt shake aren't modelled; labels can drift by a pixel or two while those play.
 */
public final class Projection {
	private static Vec3 cameraPos = Vec3.ZERO;
	private static float yaw;
	private static float pitch;
	private static float fov = 70f;

	private Projection() {
	}

	/** From {@code CameraMixin}, once the final camera transform is known. */
	public static void captureCamera(Vec3 position, float yRot, float xRot) {
		cameraPos = position;
		yaw = yRot;
		pitch = xRot;
	}

	/** From {@code GameRendererMixin}: the world FOV in degrees, zoom included. */
	public static void captureFov(float degrees) {
		fov = degrees;
	}

	/** @return {x, y} in GUI-scaled pixels, or null when the point is behind the camera. */
	public static float[] toScreen(Vec3 world) {
		Minecraft mc = Minecraft.getInstance();
		double yawRad = Math.toRadians(yaw);
		double pitchRad = Math.toRadians(pitch);
		// Camera basis: forward matches Entity.getViewVector; right and up follow from it.
		double fx = -Math.sin(yawRad) * Math.cos(pitchRad), fy = -Math.sin(pitchRad), fz = Math.cos(yawRad) * Math.cos(pitchRad);
		double rx = -Math.cos(yawRad), ry = 0, rz = -Math.sin(yawRad);
		double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;

		double dx = world.x - cameraPos.x, dy = world.y - cameraPos.y, dz = world.z - cameraPos.z;
		double depth = dx * fx + dy * fy + dz * fz;
		if (depth < 0.05) return null;
		double cx = dx * rx + dy * ry + dz * rz;
		double cy = dx * ux + dy * uy + dz * uz;

		float w = mc.getWindow().getGuiScaledWidth();
		float h = mc.getWindow().getGuiScaledHeight();
		double tan = Math.tan(Math.toRadians(fov) / 2);
		double ndcX = cx / (depth * tan * (w / h));
		double ndcY = cy / (depth * tan);
		return new float[]{(float) ((ndcX + 1) / 2 * w), (float) ((1 - ndcY) / 2 * h)};
	}
}
