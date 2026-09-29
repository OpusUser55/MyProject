package dev.ooga.client.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * First-person hand and held-item policy.
 *
 * <p>Vanilla draws the hand as a screen-space overlay tied to the options' first-person
 * perspective, not to where the camera is. With a detached camera that works in our favour:
 * the controller keeps the perspective in first person so the hand is always submitted, and
 * this class only adjusts what vanilla can't know about — that the eye is somewhere else —
 * by relighting the hand at the camera's position so it doesn't stay dark in a cave while
 * the camera floats in daylight (or vice versa).
 */
public final class FirstPersonRenderer {
	private FirstPersonRenderer() {
	}

	public static boolean shouldRenderHand() {
		CameraMode mode = CameraController.get().activeMode();
		return mode == null || mode.renderHand();
	}

	public static int handLight(int vanillaLight) {
		CameraController controller = CameraController.get();
		CameraMode mode = controller.activeMode();
		Minecraft mc = Minecraft.getInstance();
		if (mode == null || !mode.lightHandAtCamera() || mc.level == null) return vanillaLight;

		Vec3 eye = controller.lastAppliedPosition();
		if (eye == null) return vanillaLight;
		BlockPos pos = BlockPos.containing(eye);
		int block = mc.level.getBrightness(LightLayer.BLOCK, pos);
		int sky = mc.level.getBrightness(LightLayer.SKY, pos);
		return LightTexture.pack(block, sky);
	}
}
