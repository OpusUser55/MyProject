package dev.ooga.client.mixin;

import dev.ooga.client.world.BlockUpdates;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forwards block changes to {@link BlockUpdates}. Handlers first run on the network thread,
 * where {@code ensureRunningOnSameThread} throws to reschedule them, so TAIL is only ever
 * reached on the client thread, after the level has applied the change.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleBlockUpdate", at = @At("TAIL"), require = 0)
	private void ooga$blockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
		BlockUpdates.fire(packet.getPos(), packet.getBlockState());
	}

	@Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"), require = 0)
	private void ooga$sectionUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
		packet.runUpdates(BlockUpdates::fire);
	}
}
