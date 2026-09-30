package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import net.minecraft.world.phys.HitResult;

/** Holds the attack button for you while you look at a block, so you can mine hands-free. */
public class AutoMineModule extends Module {
	private boolean holding;

	public AutoMineModule() {
		super("Auto Mine", "Keeps mining whatever block you look at.", Category.MISC);
	}

	@Override
	public void onTick() {
		boolean onBlock = mc.player != null && mc.screen == null && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
		if (onBlock) {
			mc.options.keyAttack.setDown(true);
			holding = true;
		} else {
			release();
		}
	}

	@Override
	protected void onDisable() {
		release();
	}

	private void release() {
		if (!holding) return;
		holding = false;
		mc.options.keyAttack.setDown(false);
	}
}
