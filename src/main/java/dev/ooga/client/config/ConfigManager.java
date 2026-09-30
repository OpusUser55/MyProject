package dev.ooga.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
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
import java.util.ArrayList;
import java.util.List;

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
		try {
			apply(read(file));
		} catch (IOException | RuntimeException e) {
			ModuleManager.LOGGER.error("Failed to load Ooga config; using defaults", e);
		}
	}

	public void save() {
		dirtySince = -1;
		try {
			write(file, buildRoot());
		} catch (IOException e) {
			ModuleManager.LOGGER.error("Failed to save Ooga config", e);
		}
	}

	// ------------------------------------------------------------------ profiles

	/** Profile names are kept to letters, digits, dashes and underscores so they're safe file names. */
	public static String cleanProfileName(String name) {
		String clean = name.replaceAll("[^A-Za-z0-9_-]", "");
		return clean.length() > 32 ? clean.substring(0, 32) : clean;
	}

	private Path profilesDir() {
		return file.resolveSibling("profiles");
	}

	/** Saves the current modules, binds, settings and HUD layout as a named profile. */
	public boolean saveProfile(String name) {
		String clean = cleanProfileName(name);
		if (clean.isEmpty()) return false;
		try {
			write(profilesDir().resolve(clean + ".json"), buildRoot());
			return true;
		} catch (IOException e) {
			ModuleManager.LOGGER.error("Failed to save profile {}", clean, e);
			return false;
		}
	}

	/** Applies a saved profile on top of the current state, then saves it as the active config. */
	public boolean loadProfile(String name) {
		Path path = profilesDir().resolve(cleanProfileName(name) + ".json");
		if (cleanProfileName(name).isEmpty() || !Files.exists(path)) return false;
		try {
			apply(read(path));
			save();
			return true;
		} catch (IOException | RuntimeException e) {
			ModuleManager.LOGGER.error("Failed to load profile {}", name, e);
			return false;
		}
	}

	public List<String> profiles() {
		List<String> names = new ArrayList<>();
		if (!Files.isDirectory(profilesDir())) return names;
		try (var stream = Files.list(profilesDir())) {
			stream.map(p -> p.getFileName().toString())
					.filter(n -> n.endsWith(".json"))
					.map(n -> n.substring(0, n.length() - 5))
					.sorted()
					.forEach(names::add);
		} catch (IOException e) {
			ModuleManager.LOGGER.error("Failed to list profiles", e);
		}
		return names;
	}

	public boolean deleteProfile(String name) {
		try {
			return !cleanProfileName(name).isEmpty() && Files.deleteIfExists(profilesDir().resolve(cleanProfileName(name) + ".json"));
		} catch (IOException e) {
			return false;
		}
	}

	// ------------------------------------------------------------------ (de)serialisation

	private static JsonObject read(Path path) throws IOException {
		return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private void apply(JsonObject root) {
		loading = true;
		try {
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
			if (root.has("hud")) HudManager.get().fromJson(root.getAsJsonObject("hud"));
			if (root.has("clickgui")) PanelLayout.fromJson(root.getAsJsonObject("clickgui"));
		} finally {
			loading = false;
			dirtySince = -1;
		}
	}

	private JsonObject buildRoot() {
		JsonObject root = new JsonObject();
		root.addProperty("version", 1);
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

	private static void write(Path target, JsonObject root) throws IOException {
		Files.createDirectories(target.getParent());
		// Write to a temp file first so a crash mid-write can't corrupt the real file.
		Path temp = target.resolveSibling(target.getFileName() + ".tmp");
		Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
		try {
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
