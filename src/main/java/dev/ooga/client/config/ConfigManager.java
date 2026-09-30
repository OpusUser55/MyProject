package dev.ooga.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.client.ClientSettings;
import dev.ooga.client.module.setting.Setting;
import dev.ooga.client.ui.clickgui.PanelLayout;
import dev.ooga.client.ui.hud.HudManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Persists module state, keybinds, settings and HUD layout to {@code config/ooga/config.json}.
 * Writes are debounced: any change marks the config dirty and it is flushed shortly after,
 * plus once more when the game closes.
 */
public final class ConfigManager {
	private static final ConfigManager INSTANCE = new ConfigManager();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final long SAVE_DELAY_MS = 1500;

	private final Path file = FabricLoader.getInstance().getConfigDir().resolve("ooga").resolve("config.json");
	private long dirtySince = -1;
	private boolean loading;

	private ConfigManager() {
	}

	public static ConfigManager get() {
		return INSTANCE;
	}

	public void markDirty() {
		if (loading) return;
		if (dirtySince < 0) dirtySince = System.currentTimeMillis();
	}

	public void tick() {
		if (dirtySince >= 0 && System.currentTimeMillis() - dirtySince >= SAVE_DELAY_MS) save();
	}

	public void load() {
		if (!Files.exists(file)) return;
		loading = true;
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			apply(root, true);
			// Version 1 configs stored the old gold default; move them to the new blue default.
			if (!root.has("version") || root.get("version").getAsInt() < 2) {
				ClientSettings settings = ModuleManager.get().get(ClientSettings.class);
				if (settings.accent.is("Gold")) settings.accent.set("Ocean");
			}
		} catch (IOException | RuntimeException e) {
			ModuleManager.LOGGER.error("Failed to load Ooga config; using defaults", e);
		} finally {
			loading = false;
			dirtySince = -1;
		}
	}

	/**
	 * Applies a saved state. With {@code layout}, HUD positions and menu panels are restored too
	 * (the main config); named configs leave the layout where it is.
	 */
	public void apply(JsonObject root, boolean layout) {
		JsonObject modules = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
		for (Module module : ModuleManager.get().getModules()) {
			if (!modules.has(module.getName())) continue;
			JsonObject data = modules.getAsJsonObject(module.getName());
			if (data.has("key")) module.setKey(data.get("key").getAsInt());
			if (data.has("settings")) {
				JsonObject settings = data.getAsJsonObject("settings");
				for (Setting<?> setting : module.getSettings()) {
					JsonElement value = settings.get(setting.getName());
					if (value != null) setting.fromJson(value);
				}
			}
			if (module.persistsEnabledState() && data.has("enabled")) {
				module.setEnabled(data.get("enabled").getAsBoolean(), false);
			}
		}
		if (layout && root.has("hud")) HudManager.get().fromJson(root.getAsJsonObject("hud"));
		if (layout && root.has("clickgui")) PanelLayout.fromJson(root.getAsJsonObject("clickgui"));
	}

	/** The whole current state as JSON: modules, keys, settings, HUD and menu layout. */
	public JsonObject snapshot() {
		JsonObject root = new JsonObject();
		root.addProperty("version", 2);
		JsonObject modules = new JsonObject();
		for (Module module : ModuleManager.get().getModules()) {
			JsonObject data = new JsonObject();
			data.addProperty("enabled", module.isEnabled());
			data.addProperty("key", module.getKey());
			JsonObject settings = new JsonObject();
			for (Setting<?> setting : module.getSettings()) settings.add(setting.getName(), setting.toJson());
			data.add("settings", settings);
			modules.add(module.getName(), data);
		}
		root.add("modules", modules);
		root.add("hud", HudManager.get().toJson());
		root.add("clickgui", PanelLayout.toJson());
		return root;
	}

	public void save() {
		dirtySince = -1;
		JsonObject root = snapshot();

		try {
			Files.createDirectories(file.getParent());
			// Write to a temp file first so a crash mid-write can't corrupt the real config.
			Path temp = file.resolveSibling("config.json.tmp");
			Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
			try {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			ModuleManager.LOGGER.error("Failed to save Ooga config", e);
		}
	}
}
