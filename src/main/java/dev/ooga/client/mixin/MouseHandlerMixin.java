package dev.ooga.client.mixin;

import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.render.FreecamModule;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void ooga$freecamScroll(long window, double scrollX, double scrollY, CallbackInfo ci) {
		if (ModuleManager.get().get(FreecamModule.class).onScroll(scrollY)) ci.cancel();
	}
}
