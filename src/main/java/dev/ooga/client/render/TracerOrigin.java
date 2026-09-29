package dev.ooga.client.render;

import dev.ooga.client.camera.CameraController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Where tracers start: a point just in front of the eye, so lines appear to leave the crosshair. */
public final class TracerOrigin {
	private TracerOrigin() {
	}

	public static Vec3 get(WorldOverlay.Drawer drawer, float partialTick) {
		Vec3 view = CameraController.get().viewVector(partialTick);
		return drawer.camera().add(view.scale(0.6));
	}
}
