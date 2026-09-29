package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.WatermarkHud;

public class WatermarkModule extends Module {
	public final ModeSetting style = add(new ModeSetting("Style", "How much of the brand to show.", "Wordmark", "Mark", "Wordmark", "Full"));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Watermark size.", 1.0, 0.5, 2.0, 0.05, "x"));
	public final NumberSetting opacity = add(new NumberSetting("Opacity", "Watermark opacity.", 1.0, 0.2, 1.0, 0.05));
	public final NumberSetting glow = add(new NumberSetting("Glow Intensity", "Glow around the Ooga mark. Respects the global Glow toggle.", 0.8, 0.0, 2.0, 0.05));
	public final BooleanSetting fps = add(new BooleanSetting("Show FPS", "Append the current frame rate.", true));
	public final BooleanSetting time = add(new BooleanSetting("Show Time", "Append your local time.", false));
	public final ModeSetting clock = add(new ModeSetting("Clock", "Time format.", "24h", "24h", "12h")
			.visibleWhen(time::get));
	public final BooleanSetting server = add(new BooleanSetting("Show Server", "Append the server address (or Singleplayer).", false));

	public WatermarkModule() {
		super("Watermark", "The Ooga mark in the corner of your screen.", Category.HUD);
		hideFromList();
		enableByDefault();
		HudManager.get().register(new WatermarkHud(this));
	}
}
