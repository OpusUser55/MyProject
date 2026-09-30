package dev.ooga.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
	@Accessor("rightClickDelay")
	int ooga$getRightClickDelay();

	@Accessor("rightClickDelay")
	void ooga$setRightClickDelay(int delay);
}
