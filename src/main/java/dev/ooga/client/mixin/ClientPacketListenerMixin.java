package dev.ooga.client.mixin;

import dev.ooga.client.module.impl.combat.TotemPopsModule;
import dev.ooga.client.module.impl.render.LogoutSpotsModule;
import dev.ooga.client.module.impl.combat.VelocityModule;
import dev.ooga.client.world.BlockUpdates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
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

	/** Velocity: scales knockback sent to us. Only acts on the client thread, where the packet is applied. */
	@Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true, require = 0)
	private void ooga$velocity(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (!mc.isSameThread() || mc.player == null || packet.getId() != mc.player.getId() || !VelocityModule.active()) return;
		mc.player.lerpMotion(VelocityModule.scale(packet.getMovement()));
		ci.cancel();
	}

	/** Velocity: takes back the part of explosion knockback we don't want. */
	@Inject(method = "handleExplosion", at = @At("TAIL"), require = 0)
	private void ooga$explosionVelocity(ClientboundExplodePacket packet, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !VelocityModule.active()) return;
		packet.playerKnockback().ifPresent(knock -> mc.player.addDeltaMovement(VelocityModule.scale(knock).subtract(knock)));
	}

	/** Totem Pops: event 35 is the totem-of-undying animation. */
	@Inject(method = "handleEntityEvent", at = @At("TAIL"), require = 0)
	private void ooga$totemPop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (packet.getEventId() != 35 || mc.level == null) return;
		net.minecraft.world.entity.Entity entity = packet.getEntity(mc.level);
		if (entity != null) TotemPopsModule.onPop(entity);
	}

	/** Logout Spots: see who is leaving before their entity disappears. */
	@Inject(method = "handlePlayerInfoRemove", at = @At("HEAD"), require = 0)
	private void ooga$logout(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
		if (Minecraft.getInstance().isSameThread()) LogoutSpotsModule.onRemove(packet.profileIds());
	}

	@Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"), require = 0)
	private void ooga$sectionUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
		packet.runUpdates(BlockUpdates::fire);
	}
}
