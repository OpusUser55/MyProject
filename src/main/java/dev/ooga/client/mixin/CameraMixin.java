package dev.ooga.client.mixin;

import dev.ooga.client.camera.CameraController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	/** Lets the active camera mode override the transform vanilla just computed. */
	@Inject(method = "setup", at = @At("TAIL"))
	private void ooga$applyCameraMode(Level level, Entity entity, boolean detached, boolean mirror, float partialTick, CallbackInfo ci) {
		CameraController.get().apply((CameraAccessor) this, partialTick);
	}
}
