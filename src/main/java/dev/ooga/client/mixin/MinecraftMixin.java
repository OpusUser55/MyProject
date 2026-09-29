package dev.ooga.client.mixin;

import dev.ooga.client.camera.CameraController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops clicks reaching the world while a detached camera doesn't allow interaction. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void ooga$blockAttack(CallbackInfoReturnable<Boolean> cir) {
		if (CameraController.get().blocksInteraction()) cir.setReturnValue(false);
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void ooga$blockMining(boolean leftClick, CallbackInfo ci) {
		if (CameraController.get().blocksInteraction()) ci.cancel();
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void ooga$blockUse(CallbackInfo ci) {
		if (CameraController.get().blocksInteraction()) ci.cancel();
	}

	@Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
	private void ooga$blockPick(CallbackInfo ci) {
		if (CameraController.get().blocksInteraction()) ci.cancel();
	}
}
