package dev.ooga.client.mixin;

import dev.ooga.client.camera.CameraController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/** Routes mouse look to a detached camera instead of turning the player. */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void ooga$turnCamera(double deltaYaw, double deltaPitch, CallbackInfo ci) {
		if ((Object) this == Minecraft.getInstance().player && CameraController.get().onTurn(deltaYaw, deltaPitch)) {
			ci.cancel();
		}
	}
}
