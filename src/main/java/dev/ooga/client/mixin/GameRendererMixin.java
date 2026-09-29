package dev.ooga.client.mixin;

import dev.ooga.client.camera.CameraController;
import dev.ooga.client.camera.FirstPersonRenderer;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.render.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void ooga$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
		// Only the world projection zooms; the hand keeps its own fixed FOV.
		if (!useFovSetting) return;
		float zoom = ModuleManager.get().get(ZoomModule.class).currentZoom();
		if (zoom != 1f) cir.setReturnValue(cir.getReturnValue() / zoom);
	}

	/** A detached camera that can't interact shouldn't highlight the block the body is facing. */
	@Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
	private void ooga$hideOutline(CallbackInfoReturnable<Boolean> cir) {
		if (CameraController.get().blocksInteraction()) cir.setReturnValue(false);
	}

	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void ooga$hideHand(CallbackInfo ci) {
		if (!FirstPersonRenderer.shouldRenderHand()) ci.cancel();
	}

	@ModifyArg(method = "renderItemInHand", index = 4, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V"))
	private int ooga$relightHand(int packedLight) {
		return FirstPersonRenderer.handLight(packedLight);
	}
}
