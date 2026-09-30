package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.RadarHud;

public class RadarModule extends Module {
	public final NumberSetting range = add(new NumberSetting("Range", "Blocks from the centre to the edge.", 64, 16, 256, 8, "m"));
	public final BooleanSetting players = add(new BooleanSetting("Players", "Show other players.", true));
	public final BooleanSetting hostiles = add(new BooleanSetting("Hostiles", "Show hostile mobs.", false));
	public final BooleanSetting finds = add(new BooleanSetting("Finds", "Show spawners, stashes, tunnels and sus chunks.", true));
	public final BooleanSetting distances = add(new BooleanSetting("Distance Tags", "Label each player with how far away they are.", true));
	public final BooleanSetting names = add(new BooleanSetting("Names", "Label each player with their name instead of distance.", false));
	public final ModeSetting dotColor = add(new ModeSetting("Dot Color", "Colour of player dots.", "Pink", "Pink", "Accent", "Red", "White"));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Radar size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public RadarModule() {
		super("Radar", "A rotating minimap of players and finds around you.", Category.HUD);
		hideFromList();
		HudManager.get().register(new RadarHud(this));
	}
}
