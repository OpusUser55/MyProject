package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.FindsHud;
import dev.ooga.client.ui.hud.HudManager;

public class FindsHudModule extends Module {
	public final NumberSetting count = add(new NumberSetting("Entries", "How many recent finds to list.", 5, 1, 12, 1));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Panel size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public FindsHudModule() {
		super("Finds", "Recent spawners, tunnels and sus chunks with distance and direction.", Category.HUD);
		hideFromList();
		HudManager.get().register(new FindsHud(this));
	}
}
