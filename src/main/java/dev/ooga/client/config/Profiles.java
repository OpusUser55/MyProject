package dev.ooga.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ooga.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Named configs in {@code config/ooga/configs/<name>.json}: a snapshot of every module's
 * on/off state, keybind and settings. Loading one leaves your HUD and menu layout alone.
 */
public final class Profiles {
	public record Entry(String name, long modified) {
	}

	public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("ooga").resolve("configs");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private Profiles() {
	}

	/** Letters, digits, spaces, dashes and underscores only; at most 24 characters. */
	public static String clean(String name) {
		String cleaned = name.replaceAll("[^A-Za-z0-9 _-]", "").trim();
		return cleaned.length() > 24 ? cleaned.substring(0, 24) : cleaned;
	}

	private static Path file(String name) {
		return DIR.resolve(clean(name) + ".json");
	}

	public static List<Entry> list() {
		List<Entry> result = new ArrayList<>();
		if (!Files.isDirectory(DIR)) return result;
		try (Stream<Path> files = Files.list(DIR)) {
			files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
				String file = p.getFileName().toString();
				long modified = 0;
				try {
					FileTime time = Files.getLastModifiedTime(p);
					modified = time.toMillis();
				} catch (IOException ignored) {
					// Unknown time sorts last.
				}
				result.add(new Entry(file.substring(0, file.length() - 5), modified));
			});
		} catch (IOException e) {
			ModuleManager.LOGGER.warn("Couldn't list {}", DIR, e);
		}
		result.sort((a, b) -> Long.compare(b.modified(), a.modified()));
		return result;
	}

	public static boolean save(String name) {
		if (clean(name).isEmpty()) return false;
		try {
			Files.createDirectories(DIR);
			JsonObject snapshot = ConfigManager.get().snapshot();
			snapshot.remove("hud");
			snapshot.remove("clickgui");
			Files.writeString(file(name), GSON.toJson(snapshot), StandardCharsets.UTF_8);
			return true;
		} catch (IOException e) {
			ModuleManager.LOGGER.warn("Couldn't save config {}", name, e);
			return false;
		}
	}

	public static boolean load(String name) {
		Path path = file(name);
		if (!Files.exists(path)) return false;
		try {
			JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
			ConfigManager.get().apply(root, false);
			ConfigManager.get().markDirty();
			return true;
		} catch (IOException | RuntimeException e) {
			ModuleManager.LOGGER.warn("Couldn't load config {}", name, e);
			return false;
		}
	}

	public static boolean delete(String name) {
		try {
			return Files.deleteIfExists(file(name));
		} catch (IOException e) {
			return false;
		}
	}

	public static boolean exists(String name) {
		return Files.exists(file(name));
	}
}
