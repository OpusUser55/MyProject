package dev.ooga.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Projects world positions onto the HUD. The matrices are copied from the last world render
 * (including view bobbing and FOV effects), so labels line up exactly with what was drawn.
 */
public final class Projector {
	private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
	private static final Vector4f SCRATCH = new Vector4f();
	private static Vec3 camera = Vec3.ZERO;
	private static boolean ready;

	private Projector() {
	}

	/** Called at the start of every world render. */
	public static void capture(Matrix4f view, Matrix4f projection, Vec3 cameraPos) {
		VIEW_PROJECTION.set(projection).mul(view);
		camera = cameraPos;
		ready = true;
	}

	public static Vec3 camera() {
		return camera;
	}

	/**
	 * @return {x, y} in GUI-scaled pixels, or null if the point is behind the camera or no
	 * world has been rendered yet.
	 */
	public static float[] toScreen(double x, double y, double z) {
		if (!ready) return null;
		Vector4f clip = SCRATCH.set((float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z), 1f);
		VIEW_PROJECTION.transform(clip);
		if (clip.w <= 0.05f) return null;
		float ndcX = clip.x / clip.w;
		float ndcY = clip.y / clip.w;
		var window = Minecraft.getInstance().getWindow();
		return new float[]{
				(ndcX + 1f) * 0.5f * window.getGuiScaledWidth(),
				(1f - ndcY) * 0.5f * window.getGuiScaledHeight()};
	}
}
