package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.RadarHud;

public class RadarModule extends Module {
	public final NumberSetting range = add(new NumberSetting("Range", "Blocks from the centre to the edge.", 64, 16, 256, 8, "m"));
	public final BooleanSetting players = add(new BooleanSetting("Players", "Show other players.", true));
	public final BooleanSetting hostiles = add(new BooleanSetting("Hostiles", "Show hostile mobs.", false));
	public final BooleanSetting finds = add(new BooleanSetting("Finds", "Show spawners, stashes, tunnels and sus chunks.", true));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Radar size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public RadarModule() {
		super("Radar", "A rotating minimap of players and finds around you.", Category.HUD);
		hideFromList();
		HudManager.get().register(new RadarHud(this));
	}
}
