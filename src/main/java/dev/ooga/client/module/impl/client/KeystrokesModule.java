package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.KeystrokesHud;

public class KeystrokesModule extends Module {
	public final BooleanSetting mouse = add(new BooleanSetting("Mouse Buttons", "Show left and right click with CPS.", true));
	public final BooleanSetting space = add(new BooleanSetting("Space Bar", "Show the jump key.", true));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Keystrokes size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public KeystrokesModule() {
		super("Keystrokes", "Shows movement keys and clicks as you press them.", Category.HUD);
		hideFromList();
		HudManager.get().register(new KeystrokesHud(this));
	}
}
