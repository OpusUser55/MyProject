package dev.ooga.client.mixin;

import dev.ooga.client.module.impl.render.NoRenderModule;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
	/** No Render: skip the carved-pumpkin blur (the only overlay texture with "pumpkin" in its path). */
	@Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true, require = 0)
	private void ooga$noPumpkin(GuiGraphics graphics, Identifier texture, float alpha, CallbackInfo ci) {
		if (NoRenderModule.pumpkin() && texture.getPath().contains("pumpkin")) ci.cancel();
	}
}
