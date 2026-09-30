package dev.ooga.client.module.impl.movement;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.SafetyModule;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Tells the server you've landed while you fall, so fall damage never builds up. */
public class NoFallModule extends Module {
	private boolean sentThisFall;

	public NoFallModule() {
		super("No Fall", "Cancels fall damage.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.getConnection() == null) return;
		if (mc.player.onGround()) {
			sentThisFall = false;
			return;
		}
		if (mc.player.fallDistance <= 2.5 || mc.player.isFallFlying() || mc.player.getAbilities().flying) return;
		if (SafetyModule.safe()) {
			// One packet per fall, right before landing, instead of every tick in the air.
			if (sentThisFall || !aboutToLand()) return;
			sentThisFall = true;
		}
		mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
	}

	private boolean aboutToLand() {
		double fall = -mc.player.getDeltaMovement().y;
		var below = mc.player.getBoundingBox().expandTowards(0, -Math.max(1.5, fall * 2), 0);
		return !mc.level.noCollision(mc.player, below);
	}

	@Override
	public boolean isBlatant() {
		return true;
	}
}
