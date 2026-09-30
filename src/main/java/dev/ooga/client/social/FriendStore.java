package dev.ooga.client.social;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.ooga.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Player names you've marked as friends, saved to {@code config/ooga/friends.json}. Case-insensitive. */
public final class FriendStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ooga").resolve("friends.json");
	private static final Set<String> FRIENDS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	private static boolean loaded;

	private FriendStore() {
	}

	public static boolean isFriend(String name) {
		load();
		return name != null && FRIENDS.contains(name);
	}

	public static boolean add(String name) {
		load();
		boolean added = FRIENDS.add(name);
		if (added) save();
		return added;
	}

	public static boolean remove(String name) {
		load();
		boolean removed = FRIENDS.remove(name);
		if (removed) save();
		return removed;
	}

	public static List<String> all() {
		load();
		return Collections.unmodifiableList(new ArrayList<>(FRIENDS));
	}

	private static void load() {
		if (loaded) return;
		loaded = true;
		if (!Files.exists(FILE)) return;
		try {
			for (JsonElement e : JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonArray()) {
				FRIENDS.add(e.getAsString());
			}
		} catch (IOException | RuntimeException e) {
			ModuleManager.LOGGER.error("Failed to load friends", e);
		}
	}

	private static void save() {
		JsonArray array = new JsonArray();
		for (String name : FRIENDS) array.add(name);
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(array), StandardCharsets.UTF_8);
		} catch (IOException e) {
			ModuleManager.LOGGER.error("Failed to save friends", e);
		}
	}
}
