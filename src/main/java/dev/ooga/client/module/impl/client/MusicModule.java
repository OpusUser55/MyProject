package dev.ooga.client.module.impl.client;

import dev.ooga.client.media.MediaManager;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.MusicHud;

public class MusicModule extends Module {
	public final NumberSetting scale = add(new NumberSetting("Scale", "Widget size.", 1.0, 0.5, 2.0, 0.05, "x"));
	public final BooleanSetting hideIdle = add(new BooleanSetting("Hide When Idle", "Only show while something is playing.", true));
	public final BooleanSetting controls = add(new BooleanSetting("Controls", "Previous, play/pause and next buttons (click them with chat open).", true));
	public final BooleanSetting marquee = add(new BooleanSetting("Scroll Titles", "Scroll titles that don't fit.", true));

	private final MusicHud hud = new MusicHud(this);

	public MusicModule() {
		super("Music", "Now playing from Spotify, your browser or any system media player.", Category.HUD);
		hideFromList();
		HudManager.get().register(hud);
	}

	@Override
	protected void onEnable() {
		MediaManager.get().setRunning(true);
	}

	@Override
	protected void onDisable() {
		MediaManager.get().setRunning(false);
	}

	public MusicHud hud() {
		return hud;
	}
}
