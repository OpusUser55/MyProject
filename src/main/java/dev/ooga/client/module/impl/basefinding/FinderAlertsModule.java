package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;

/** Shared alert options for every base finder. Read through {@link #instance()}. */
public class FinderAlertsModule extends Module {
	private static FinderAlertsModule instance;

	public final BooleanSetting sound = add(new BooleanSetting("Sound", "Play a ping when a finder turns something up.", true));
	public final NumberSetting volume = add(new NumberSetting("Volume", "Ping volume.", 0.6, 0.1, 1.0, 0.05)
			.visibleWhen(sound::get));
	public final NumberSetting pitch = add(new NumberSetting("Pitch", "Ping pitch.", 1.4, 0.5, 2.0, 0.05)
			.visibleWhen(sound::get));
	public final BooleanSetting logToFile = add(new BooleanSetting("Log To File", "Append every find to config/ooga/finds.log with server, dimension and time.", true));

	public FinderAlertsModule() {
		super("Finder Alerts", "Sound and logging for spawners, stashes, tunnels and sus chunks.", Category.BASEFINDING);
		settingsOnly();
		instance = this;
	}

	public static FinderAlertsModule instance() {
		return instance;
	}
}
