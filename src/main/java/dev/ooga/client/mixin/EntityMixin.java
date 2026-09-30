package dev.ooga.client.mixin;

import dev.ooga.client.camera.CameraController;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.combat.HitboxModule;
import dev.ooga.client.module.impl.render.ZoomModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

	/** Routes mouse look to a detached camera instead of turning the player. */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void ooga$turnCamera(double deltaYaw, double deltaPitch, CallbackInfo ci) {
		if (ooga$isLocalPlayer() && CameraController.get().onTurn(deltaYaw, deltaPitch)) {
			ci.cancel();
		}
	}

	/** Hitbox: grows the radius used when picking the entity under the crosshair. */
	@Inject(method = "getPickRadius", at = @At("RETURN"), cancellable = true, require = 0)
	private void ooga$hitbox(CallbackInfoReturnable<Float> cir) {
		float extra = HitboxModule.extra((Entity) (Object) this);
		if (extra > 0) cir.setReturnValue(cir.getReturnValueF() + extra);
	}

	@Unique
	private boolean ooga$isLocalPlayer() {
		return (Object) this == Minecraft.getInstance().player;
	}
}
