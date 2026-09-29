package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.ModuleListHud;

public class ModuleListModule extends Module {
	public final ModeSetting sort = add(new ModeSetting("Sort", "Order of enabled modules.", "Length", "Length", "Alphabetical"));
	public final NumberSetting scale = add(new NumberSetting("Scale", "List size.", 1.0, 0.5, 1.5, 0.05, "x"));
	public final NumberSetting opacity = add(new NumberSetting("Background", "Opacity of each row's backing.", 0.75, 0.0, 1.0, 0.05));
	public final BooleanSetting accentBar = add(new BooleanSetting("Accent Bar", "Gold edge on the screen side of each row.", true));
	public final BooleanSetting suffixes = add(new BooleanSetting("Suffixes", "Show module modes next to names.", true));
	public final ModeSetting suffixColor = add(new ModeSetting("Suffix Color", "Colour of the suffix text.", "Accent", "Accent", "Muted")
			.visibleWhen(suffixes::get));
	public final ModeSetting textCase = add(new ModeSetting("Text Case", "How module names are written.", "Normal", "Normal", "lowercase", "UPPERCASE"));
	public final NumberSetting spacing = add(new NumberSetting("Row Spacing", "Extra space between rows.", 0, 0, 4, 0.5, "px"));

	public ModuleListModule() {
		super("Module List", "Lists your enabled modules on screen.", Category.HUD);
		hideFromList();
		enableByDefault();
		HudManager.get().register(new ModuleListHud(this));
	}
}
