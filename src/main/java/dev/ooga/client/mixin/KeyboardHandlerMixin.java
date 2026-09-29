package dev.ooga.client.mixin;

import dev.ooga.client.module.ModuleManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	/** Module keybinds fire on press, only in-world with no screen open. */
	@Inject(method = "keyPress", at = @At("HEAD"))
	private void ooga$moduleKeybinds(long window, int action, KeyEvent event, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (action != GLFW.GLFW_PRESS || mc.screen != null || event.key() == GLFW.GLFW_KEY_UNKNOWN) return;
		if (window != mc.getWindow().handle()) return;
		ModuleManager.get().onKeyPressed(event.key());
	}
}
