package dev.ooga.client.mixin;

import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.render.EspModule;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void ooga$espColor(CallbackInfoReturnable<Integer> cir) {
		int color = ModuleManager.get().get(EspModule.class).outlineColor((Entity) (Object) this);
		if (color != -1) cir.setReturnValue(color);
	}
}
