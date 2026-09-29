package dev.ooga.client.mixin;

import dev.ooga.client.camera.FirstPersonRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
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
