package dev.ooga.client.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.waypoint.WaypointStore;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What was in each container the last time you closed it, per server, saved to
 * {@code config/ooga/containers.json}. Items are stored by ID with a display name so searches
 * match either.
 */
public final class ContainerMemory {
	/** One remembered container. {@code items} maps item ID to count; {@code names} maps item ID to display name. */
	public record Entry(String dimension, BlockPos pos, Map<String, Integer> items, Map<String, String> names, long seenAt) {
	}

	private static final Gson GSON = new GsonBuilder().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ooga").resolve("containers.json");
	/** World key → position key → entry. */
	private static final Map<String, Map<String, Entry>> WORLDS = new LinkedHashMap<>();
	private static boolean loaded;
	private static boolean dirty;
	private static long dirtySince;

	private ContainerMemory() {
	}

	private static String key(String dimension, BlockPos pos) {
		return dimension + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
	}

	public static void remember(BlockPos pos, Map<String, Integer> items, Map<String, String> names) {
		load();
		String world = WaypointStore.worldKey();
		if (world == null) return;
		String dim = WaypointStore.currentDimension();
		Map<String, Entry> entries = WORLDS.computeIfAbsent(world, k -> new LinkedHashMap<>());
		if (items.isEmpty()) entries.remove(key(dim, pos));
		else entries.put(key(dim, pos), new Entry(dim, pos.immutable(), items, names, System.currentTimeMillis()));
		markDirty();
	}

	/** Forget a container that was broken. */
	public static void forget(BlockPos pos) {
		load();
		String world = WaypointStore.worldKey();
		Map<String, Entry> entries = world == null ? null : WORLDS.get(world);
		if (entries != null && entries.remove(key(WaypointStore.currentDimension(), pos)) != null) markDirty();
	}

	public static boolean knows(BlockPos pos) {
		load();
		String world = WaypointStore.worldKey();
		Map<String, Entry> entries = world == null ? null : WORLDS.get(world);
		return entries != null && entries.containsKey(key(WaypointStore.currentDimension(), pos));
	}

	/** Containers on this server whose items match {@code query} by ID or name, with the matching count. */
	public static List<Map.Entry<Entry, Integer>> search(String query) {
		load();
		String world = WaypointStore.worldKey();
		Map<String, Entry> entries = world == null ? null : WORLDS.get(world);
		if (entries == null) return List.of();
		String q = query.toLowerCase(Locale.ROOT).replace(' ', '_');
		String qSpaced = query.toLowerCase(Locale.ROOT);
		List<Map.Entry<Entry, Integer>> hits = new ArrayList<>();
		for (Entry e : entries.values()) {
			int count = 0;
			for (Map.Entry<String, Integer> item : e.items().entrySet()) {
				String name = e.names().getOrDefault(item.getKey(), "").toLowerCase(Locale.ROOT);
				if (item.getKey().contains(q) || name.contains(qSpaced)) count += item.getValue();
			}
			if (count > 0) hits.add(Map.entry(e, count));
		}
		return hits;
	}

	public static int size() {
		load();
		String world = WaypointStore.worldKey();
		Map<String, Entry> entries = world == null ? null : WORLDS.get(world);
		return entries == null ? 0 : entries.size();
	}

	public static int clearWorld() {
		load();
		String world = WaypointStore.worldKey();
		Map<String, Entry> entries = world == null ? null : WORLDS.remove(world);
		if (entries == null) return 0;
		markDirty();
		return entries.size();
	}

	private static void markDirty() {
		if (!dirty) dirtySince = System.currentTimeMillis();
		dirty = true;
	}

	/** Called every tick; writes a few seconds after the last change so chest-hopping doesn't hammer the disk. */
	public static void tick() {
		if (dirty && System.currentTimeMillis() - dirtySince > 3000) save();
	}

	private static void load() {
		if (loaded) return;
		loaded = true;
		if (!Files.exists(FILE)) return;
		try {
			JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
			for (Map.Entry<String, JsonElement> world : root.entrySet()) {
				Map<String, Entry> entries = new LinkedHashMap<>();
				for (JsonElement el : world.getValue().getAsJsonArray()) {
					JsonObject o = el.getAsJsonObject();
					BlockPos pos = new BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt());
					String dim = o.get("dim").getAsString();
					Map<String, Integer> items = new HashMap<>();
					Map<String, String> names = new HashMap<>();
					for (Map.Entry<String, JsonElement> item : o.getAsJsonObject("items").entrySet()) {
						JsonObject i = item.getValue().getAsJsonObject();
						items.put(item.getKey(), i.get("count").getAsInt());
						if (i.has("name")) names.put(item.getKey(), i.get("name").getAsString());
					}
					entries.put(key(dim, pos), new Entry(dim, pos, items, names, o.has("seen") ? o.get("seen").getAsLong() : 0));
				}
				WORLDS.put(world.getKey(), entries);
			}
		} catch (IOException | RuntimeException e) {
			ModuleManager.LOGGER.error("Failed to load container memory", e);
		}
	}

	public static void save() {
		if (!loaded) return;
		dirty = false;
		JsonObject root = new JsonObject();
		for (Map.Entry<String, Map<String, Entry>> world : WORLDS.entrySet()) {
			com.google.gson.JsonArray array = new com.google.gson.JsonArray();
			for (Entry e : world.getValue().values()) {
				JsonObject o = new JsonObject();
				o.addProperty("dim", e.dimension());
				o.addProperty("x", e.pos().getX());
				o.addProperty("y", e.pos().getY());
				o.addProperty("z", e.pos().getZ());
				o.addProperty("seen", e.seenAt());
				JsonObject items = new JsonObject();
				for (Map.Entry<String, Integer> item : e.items().entrySet()) {
					JsonObject i = new JsonObject();
					i.addProperty("count", item.getValue());
					String name = e.names().get(item.getKey());
					if (name != null) i.addProperty("name", name);
					items.add(item.getKey(), i);
				}
				o.add("items", items);
				array.add(o);
			}
			root.add(world.getKey(), array);
		}
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException e) {
			ModuleManager.LOGGER.error("Failed to save container memory", e);
		}
	}

	/** Entries in the current world and dimension, for highlighting. */
	public static List<Entry> hereAndNow() {
		load();
		String world = WaypointStore.worldKey();
		Map<String, Entry> entries = world == null ? null : WORLDS.get(world);
		if (entries == null) return List.of();
		String dim = WaypointStore.currentDimension();
		List<Entry> out = new ArrayList<>();
		for (Entry e : entries.values()) if (e.dimension().equals(dim)) out.add(e);
		return Collections.unmodifiableList(out);
	}
}
