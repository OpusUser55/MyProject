package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.client.gui.screens.DeathScreen;

/** Respawns for you after dying (your death point is saved by Waypoints). */
public class AutoRespawnModule extends Module {
	public final NumberSetting delay = add(new NumberSetting("Delay", "Seconds to wait on the death screen.", 0.5, 0, 5, 0.25, "s"));

	private long diedAt = -1;

	public AutoRespawnModule() {
		super("Auto Respawn", "Skips the death screen.", Category.MISC);
	}

	@Override
	public void onTick() {
		if (!(mc.screen instanceof DeathScreen) || mc.player == null) {
			diedAt = -1;
			return;
		}
		if (diedAt < 0) diedAt = System.currentTimeMillis();
		if (System.currentTimeMillis() - diedAt >= delay.get() * 1000) {
			mc.player.respawn();
			mc.setScreen(null);
			diedAt = -1;
		}
	}
}
