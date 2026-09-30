package dev.ooga.client.module.impl.movement;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Tells the server you've landed while you fall, so fall damage never builds up. */
public class NoFallModule extends Module {
	public NoFallModule() {
		super("No Fall", "Cancels fall damage.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.getConnection() == null) return;
		if (mc.player.fallDistance > 2.5 && !mc.player.isFallFlying() && !mc.player.getAbilities().flying) {
			mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
		}
	}
}
