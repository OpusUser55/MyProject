package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;

/** A notification when it starts or stops raining or thundering. */
public class WeatherNotifierModule extends Module {
	private Boolean raining;
	private Boolean thundering;

	public WeatherNotifierModule() {
		super("Weather Notifier", "Tells you when rain or thunder starts and stops.", Category.MISC);
	}

	@Override
	public void onTick() {
		if (mc.level == null) {
			raining = thundering = null;
			return;
		}
		boolean rain = mc.level.isRaining(), thunder = mc.level.isThundering();
		if (raining != null && rain != raining) NotificationManager.get().push("Weather", rain ? "It started raining" : "The rain stopped", Notification.Kind.INFO);
		if (thundering != null && thunder != thundering) NotificationManager.get().push("Weather", thunder ? "Thunderstorm started" : "Thunderstorm over", Notification.Kind.INFO);
		raining = rain;
		thundering = thunder;
	}
}
