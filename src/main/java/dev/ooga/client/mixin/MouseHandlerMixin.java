package dev.ooga.client.mixin;

import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.render.FreecamModule;
import dev.ooga.client.module.impl.render.ZoomModule;
import dev.ooga.client.util.ClickTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void ooga$scroll(long window, double scrollX, double scrollY, CallbackInfo ci) {
		ModuleManager modules = ModuleManager.get();
		if (modules.get(ZoomModule.class).onScroll(scrollY) || modules.get(FreecamModule.class).onScroll(scrollY)) {
			ci.cancel();
		}
	}

	@Inject(method = "onButton", at = @At("HEAD"))
	private void ooga$trackClicks(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		if (action == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null) ClickTracker.onPress(info.button());
	}
}
