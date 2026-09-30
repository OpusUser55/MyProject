package dev.ooga.client.mixin;

import dev.ooga.client.world.BlockUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	/** Both single and multi-block update packets end up here, already on the client thread. */
	@Inject(method = "setServerVerifiedBlockState", at = @At("TAIL"))
	private void ooga$blockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
		BlockUpdates.fire(pos, state);
	}
}
