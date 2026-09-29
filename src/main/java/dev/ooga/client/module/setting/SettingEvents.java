package dev.ooga.client.module.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Lets the config system observe setting changes without settings knowing about it. */
public final class SettingEvents {
	private static final List<Consumer<Setting<?>>> LISTENERS = new ArrayList<>();

	private SettingEvents() {
	}

	public static void listen(Consumer<Setting<?>> listener) {
		LISTENERS.add(listener);
	}

	static void changed(Setting<?> setting) {
		for (Consumer<Setting<?>> listener : LISTENERS) listener.accept(setting);
	}
}
