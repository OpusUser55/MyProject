package dev.ooga.client.mixin;

import dev.ooga.client.camera.CameraController;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.render.EspModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps clicks from reaching the world while a detached camera doesn't allow interaction,
 * and lets ESP opt entities into the vanilla glowing-outline pass.
 */
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

	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void ooga$esp(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (ModuleManager.get().get(EspModule.class).shouldOutline(entity)) cir.setReturnValue(true);
	}

	@Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
	private void ooga$blockPick(CallbackInfo ci) {
		if (CameraController.get().blocksInteraction()) ci.cancel();
	}
}
