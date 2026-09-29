package dev.ooga.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.ooga.client.render.Projector;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the frame's view and projection so the HUD can place labels over world positions. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
	private void ooga$captureMatrices(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker, boolean renderBlockOutline,
			Camera camera, Matrix4f view, Matrix4f projection, Matrix4f cullingProjection, GpuBufferSlice fog,
			Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
		Projector.capture(view, projection, camera.position());
	}
}
