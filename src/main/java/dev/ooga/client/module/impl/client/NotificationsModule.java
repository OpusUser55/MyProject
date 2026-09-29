package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;

public class NotificationsModule extends Module {
	private static NotificationsModule instance;

	public final ModeSetting position = add(new ModeSetting("Position", "Screen corner for notifications.", "Bottom Right",
			"Bottom Right", "Top Right", "Bottom Left", "Top Left"));
	public final NumberSetting duration = add(new NumberSetting("Duration", "How long each notification stays.", 2.2, 1.0, 6.0, 0.1, "s"));
	public final NumberSetting maxVisible = add(new NumberSetting("Max Visible", "Most notifications shown at once.", 4, 1, 8, 1));
	public final BooleanSetting toggles = add(new BooleanSetting("Module Toggles", "Notify when modules are enabled or disabled.", true));

	public NotificationsModule() {
		super("Notifications", "Toasts for module toggles and client events.", Category.HUD);
		hideFromList();
		instance = this;
		enableByDefault();
	}

	public static NotificationsModule instance() {
		return instance;
	}
}
