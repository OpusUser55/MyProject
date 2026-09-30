package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.ArmorHud;
import dev.ooga.client.ui.hud.HudManager;

public class ArmorHudModule extends Module {
	public final BooleanSetting hands = add(new BooleanSetting("Hands", "Also show your main and off hand.", true));
	public final BooleanSetting percent = add(new BooleanSetting("Percentage", "Durability left under each item.", true));
	public final BooleanSetting vertical = add(new BooleanSetting("Vertical", "Stack items top to bottom instead of left to right.", false));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public ArmorHudModule() {
		super("Armor HUD", "Your armor and held items with durability.", Category.HUD);
		hideFromList();
		HudManager.get().register(new ArmorHud(this));
	}
}
