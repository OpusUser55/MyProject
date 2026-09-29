package dev.ooga.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public class ModeSetting extends Setting<String> {
	private final List<String> modes;

	public ModeSetting(String name, String description, String defaultValue, String... modes) {
		super(name, description, defaultValue);
		this.modes = List.of(modes);
	}

	public List<String> getModes() {
		return modes;
	}

	public boolean is(String mode) {
		return value.equalsIgnoreCase(mode);
	}

	public void cycle(boolean forward) {
		int index = modes.indexOf(value);
		int next = Math.floorMod(index + (forward ? 1 : -1), modes.size());
		set(modes.get(next));
	}

	@Override
	protected String sanitize(String value) {
		for (String mode : modes) {
			if (mode.equalsIgnoreCase(value)) return mode;
		}
		return getDefault();
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) set(element.getAsString());
	}
}
