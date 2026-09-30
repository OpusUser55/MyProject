package dev.ooga.client.mixin;

import dev.ooga.client.module.impl.render.NoRenderModule;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
	private void ooga$noWeather(MultiBufferSource buffers, Vec3 camera, WeatherRenderState state, CallbackInfo ci) {
		if (NoRenderModule.weather()) ci.cancel();
	}
}
