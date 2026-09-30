package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.client.gui.screens.DeathScreen;

/** Clicks "Respawn" for you. Death Coords still records where you died. */
public class AutoRespawnModule extends Module {
	public final NumberSetting delay = add(new NumberSetting("Delay", "Ticks to wait on the death screen first (20 = 1s).", 10, 0, 100, 1));

	private int waited;

	public AutoRespawnModule() {
		super("Auto Respawn", "Respawns automatically when you die.", Category.MISC);
	}

	@Override
	public void onTick() {
		if (!(mc.screen instanceof DeathScreen) || mc.player == null) {
			waited = 0;
			return;
		}
		if (waited++ < delay.getInt()) return;
		waited = 0;
		mc.player.respawn();
		mc.setScreen(null);
	}
}
