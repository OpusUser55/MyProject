package dev.ooga.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Free text, edited in place in the ClickGUI. */
public class StringSetting extends Setting<String> {
	public static final int MAX_LENGTH = 48;

	public StringSetting(String name, String description, String defaultValue) {
		super(name, description, defaultValue);
	}

	@Override
	protected String sanitize(String value) {
		// Strip control characters; a pasted newline would break the single-line field.
		StringBuilder clean = new StringBuilder();
		value.codePoints().filter(c -> !Character.isISOControl(c)).forEach(clean::appendCodePoint);
		return clean.length() > MAX_LENGTH ? clean.substring(0, MAX_LENGTH) : clean.toString();
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
