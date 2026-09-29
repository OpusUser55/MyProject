package dev.ooga.client.mixin;

import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.render.SlowMineModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Slow Mine: stretches the local player's arm swing without touching how fast blocks break. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
	private void ooga$slowSwing(CallbackInfoReturnable<Integer> cir) {
		if (!ooga$isLocalPlayer()) return;
		int duration = ModuleManager.get().get(SlowMineModule.class).swingDuration(cir.getReturnValueI());
		if (duration != cir.getReturnValueI()) cir.setReturnValue(duration);
	}

	/** Only the animation is held back: {@code LocalPlayer.swing} sends its swing packet either way. */
	@Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void ooga$fullSwing(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
		if (!ooga$isLocalPlayer()) return;
		LivingEntity self = (LivingEntity) (Object) this;
		int duration = ((LivingEntityAccessor) self).ooga$swingDuration();
		if (ModuleManager.get().get(SlowMineModule.class).holdSwing(self.swingTime, self.swinging, duration)) ci.cancel();
	}

	@Unique
	private boolean ooga$isLocalPlayer() {
		return (Object) this == Minecraft.getInstance().player;
	}
}
