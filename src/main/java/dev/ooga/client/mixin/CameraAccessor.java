package dev.ooga.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
	@Invoker("setPosition")
	void ooga$setPosition(Vec3 position);

	@Invoker("setRotation")
	void ooga$setRotation(float yRot, float xRot);

	@Accessor("detached")
	void ooga$setDetached(boolean detached);
}
