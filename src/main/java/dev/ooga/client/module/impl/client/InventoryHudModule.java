package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.InventoryHud;

public class InventoryHudModule extends Module {
	public final BooleanSetting title = add(new BooleanSetting("Title", "Show the \"Inventory\" header.", true));
	public final BooleanSetting hotbar = add(new BooleanSetting("Hotbar", "Include the hotbar as a fourth row.", false));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public InventoryHudModule() {
		super("Inventory HUD", "Your inventory on screen, without opening it.", Category.HUD);
		hideFromList();
		HudManager.get().register(new InventoryHud(this));
	}
}
