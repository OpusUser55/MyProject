package dev.ooga.client.mixin;

import dev.ooga.client.camera.CameraController;
import dev.ooga.client.camera.CameraMode;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.render.FreelookModule;
import dev.ooga.client.render.Projection;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	/** Lets the active camera mode (or Freelook) override the transform vanilla just computed. */
	@Inject(method = "setup", at = @At("TAIL"))
	private void ooga$applyCameraMode(Level level, Entity entity, boolean detached, boolean mirror, float partialTick, CallbackInfo ci) {
		CameraController.get().apply((CameraAccessor) this, partialTick);
		ModuleManager.get().get(FreelookModule.class).apply((CameraAccessor) this, entity, partialTick);
		ooga$captureForProjection(entity, detached, mirror, partialTick);
	}

	/**
	 * Records the rotation we know the camera ended up with (the active camera mode's, Freelook's,
	 * or the entity's view, mirrored for the front-facing view) for projecting HUD labels.
	 */
	@Unique
	private void ooga$captureForProjection(Entity entity, boolean detached, boolean mirror, float partialTick) {
		CameraMode mode = CameraController.get().activeMode();
		FreelookModule freelook = ModuleManager.get().get(FreelookModule.class);
		float yaw, pitch;
		if (mode != null) {
			yaw = mode.yaw(partialTick);
			pitch = mode.pitch(partialTick);
		} else if (freelook.isEnabled()) {
			yaw = freelook.yaw();
			pitch = freelook.pitch();
		} else if (entity != null) {
			yaw = entity.getViewYRot(partialTick);
			pitch = entity.getViewXRot(partialTick);
			if (detached && mirror) {
				yaw += 180f;
				pitch = -pitch;
			}
		} else {
			return;
		}
		Projection.captureCamera(((Camera) (Object) this).position(), yaw, pitch);
	}
}
