package dev.ooga.client.mixin;

import dev.ooga.client.camera.CameraController;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.render.FreelookModule;
import dev.ooga.client.module.impl.render.ZoomModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@ModifyVariable(method = "turn", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private double ooga$zoomYaw(double deltaYaw) {
		return ooga$isLocalPlayer() ? deltaYaw * ModuleManager.get().get(ZoomModule.class).sensitivityMultiplier() : deltaYaw;
	}

	@ModifyVariable(method = "turn", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private double ooga$zoomPitch(double deltaPitch) {
		return ooga$isLocalPlayer() ? deltaPitch * ModuleManager.get().get(ZoomModule.class).sensitivityMultiplier() : deltaPitch;
	}

	/** Routes mouse look to a detached camera (or Freelook) instead of turning the player. */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void ooga$turnCamera(double deltaYaw, double deltaPitch, CallbackInfo ci) {
		if (!ooga$isLocalPlayer()) return;
		if (CameraController.get().onTurn(deltaYaw, deltaPitch)
				|| ModuleManager.get().get(FreelookModule.class).onTurn(deltaYaw, deltaPitch)) {
			ci.cancel();
		}
	}

	@Unique
	private boolean ooga$isLocalPlayer() {
		return (Object) this == Minecraft.getInstance().player;
	}
}
