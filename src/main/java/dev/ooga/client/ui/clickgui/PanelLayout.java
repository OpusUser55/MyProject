package dev.ooga.client.ui.clickgui;

import com.google.gson.JsonObject;
import dev.ooga.client.config.ConfigManager;

import java.util.LinkedHashMap;
import java.util.Map;

/** Remembers where each ClickGUI panel sits and whether it's collapsed, across sessions. */
public final class PanelLayout {
	public static final class Entry {
		public float x = Float.NaN;
		public float y = Float.NaN;
		public boolean collapsed;

		public boolean placed() {
			return !Float.isNaN(x) && !Float.isNaN(y);
		}
	}

	private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

	private PanelLayout() {
	}

	public static Entry get(String id) {
		return ENTRIES.computeIfAbsent(id, k -> new Entry());
	}

	public static void changed() {
		ConfigManager.get().markDirty();
	}

	public static void reset() {
		ENTRIES.clear();
		changed();
	}

	public static JsonObject toJson() {
		JsonObject json = new JsonObject();
		ENTRIES.forEach((id, e) -> {
			if (!e.placed()) return;
			JsonObject o = new JsonObject();
			o.addProperty("x", e.x);
			o.addProperty("y", e.y);
			o.addProperty("collapsed", e.collapsed);
			json.add(id, o);
		});
		return json;
	}

	public static void fromJson(JsonObject json) {
		for (String id : json.keySet()) {
			JsonObject o = json.getAsJsonObject(id);
			Entry e = get(id);
			if (o.has("x")) e.x = o.get("x").getAsFloat();
			if (o.has("y")) e.y = o.get("y").getAsFloat();
			if (o.has("collapsed")) e.collapsed = o.get("collapsed").getAsBoolean();
		}
	}
}
