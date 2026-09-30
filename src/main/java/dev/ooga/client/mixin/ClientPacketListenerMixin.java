package dev.ooga.client.mixin;

import dev.ooga.client.world.BlockUpdates;
import dev.ooga.client.world.ServerStats;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block changes the server sends after a chunk has loaded. RETURN is only reached on the client
 * thread: the first call on the network thread bails out by throwing to reschedule itself.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleBlockUpdate", at = @At("RETURN"))
	private void ooga$blockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
		BlockUpdates.fire(packet.getPos(), packet.getBlockState());
	}

	@Inject(method = "handleChunkBlocksUpdate", at = @At("RETURN"))
	private void ooga$sectionUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
		packet.runUpdates(BlockUpdates::fire);
	}

	/** Optional: if this ever fails to apply, only the TPS readout goes missing. */
	@Inject(method = "handleSetTime", at = @At("RETURN"), require = 0)
	private void ooga$timeUpdate(ClientboundSetTimePacket packet, CallbackInfo ci) {
		ServerStats.onTimeUpdate();
	}
}
