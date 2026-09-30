package dev.ooga.client.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ooga.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Waypoints, grouped per server (or singleplayer world) and saved to
 * {@code config/ooga/waypoints.json} on every change, since they're rare and precious.
 */
public final class WaypointStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ooga").resolve("waypoints.json");
	private static final Map<String, List<Waypoint>> BY_WORLD = new LinkedHashMap<>();
	private static boolean loaded;

	private WaypointStore() {
	}

	/** Identifies the current server or singleplayer world; null when not in one. */
	public static String worldKey() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		if (mc.getCurrentServer() != null) return "server:" + mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
		if (mc.getSingleplayerServer() != null) return "local:" + mc.getSingleplayerServer().getWorldData().getLevelName();
		return "unknown";
	}

	public static String currentDimension() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return "overworld";
		if (mc.level.dimension() == Level.NETHER) return "nether";
		if (mc.level.dimension() == Level.END) return "end";
		return "overworld";
	}

	/** Waypoints for the current world, all dimensions. */
	public static List<Waypoint> current() {
		load();
		String key = worldKey();
		if (key == null) return List.of();
		return Collections.unmodifiableList(BY_WORLD.getOrDefault(key, List.of()));
	}

	public static Waypoint find(String name) {
		for (Waypoint w : current()) if (w.name().equalsIgnoreCase(name)) return w;
		return null;
	}

	/** Adds or replaces (by name) a waypoint in the current world. */
	public static boolean add(Waypoint waypoint) {
		load();
		String key = worldKey();
		if (key == null) return false;
		List<Waypoint> list = BY_WORLD.computeIfAbsent(key, k -> new ArrayList<>());
		list.removeIf(w -> w.name().equalsIgnoreCase(waypoint.name()));
		list.add(waypoint);
		save();
		return true;
	}

	public static boolean remove(String name) {
		load();
		String key = worldKey();
		List<Waypoint> list = key == null ? null : BY_WORLD.get(key);
		if (list == null || !list.removeIf(w -> w.name().equalsIgnoreCase(name))) return false;
		save();
		return true;
	}

	public static int clear() {
		load();
		String key = worldKey();
		List<Waypoint> list = key == null ? null : BY_WORLD.remove(key);
		if (list == null) return 0;
		save();
		return list.size();
	}

	private static void load() {
		if (loaded) return;
		loaded = true;
		if (!Files.exists(FILE)) return;
		try {
			JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
			for (Map.Entry<String, JsonElement> world : root.entrySet()) {
				List<Waypoint> list = new ArrayList<>();
				for (JsonElement e : world.getValue().getAsJsonArray()) {
					JsonObject o = e.getAsJsonObject();
					list.add(new Waypoint(o.get("name").getAsString(),
							new BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt()),
							o.has("dimension") ? o.get("dimension").getAsString() : "overworld"));
				}
				BY_WORLD.put(world.getKey(), list);
			}
		} catch (IOException | RuntimeException e) {
			ModuleManager.LOGGER.error("Failed to load waypoints", e);
		}
	}

	private static void save() {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, List<Waypoint>> world : BY_WORLD.entrySet()) {
			JsonArray array = new JsonArray();
			for (Waypoint w : world.getValue()) {
				JsonObject o = new JsonObject();
				o.addProperty("name", w.name());
				o.addProperty("x", w.pos().getX());
				o.addProperty("y", w.pos().getY());
				o.addProperty("z", w.pos().getZ());
				o.addProperty("dimension", w.dimension());
				array.add(o);
			}
			root.add(world.getKey(), array);
		}
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException e) {
			ModuleManager.LOGGER.error("Failed to save waypoints", e);
		}
	}
}
