package dev.ooga.client.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.ooga.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Saved waypoints, per server and dimension, in {@code config/ooga/waypoints.json}. The list
 * is small and edited rarely, so it's rewritten whole on every change.
 */
public final class Waypoints {
	public static final class Waypoint {
		public String name;
		public int x, y, z;
		public String dimension;
		public String server;
		public int color;

		public BlockPos pos() {
			return new BlockPos(x, y, z);
		}
	}

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ooga").resolve("waypoints.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int[] PALETTE = {0xFFF2C14E, 0xFF4FA3F7, 0xFF4ADE9A, 0xFFF472B6, 0xFFA78BFA, 0xFFF08A3C, 0xFF49D3E8};
	private static List<Waypoint> all;

	private Waypoints() {
	}

	private static List<Waypoint> all() {
		if (all != null) return all;
		all = new ArrayList<>();
		try {
			if (Files.exists(FILE)) {
				List<Waypoint> loaded = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), new TypeToken<List<Waypoint>>() {}.getType());
				if (loaded != null) all.addAll(loaded);
			}
		} catch (IOException | RuntimeException e) {
			ModuleManager.LOGGER.warn("Couldn't read {}", FILE, e);
		}
		return all;
	}

	private static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(all()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			ModuleManager.LOGGER.warn("Couldn't write {}", FILE, e);
		}
	}

	public static String server() {
		ServerData data = Minecraft.getInstance().getCurrentServer();
		return data != null ? data.ip.toLowerCase(Locale.ROOT) : "singleplayer";
	}

	public static String dimension() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? "" : mc.level.dimension().identifier().toString();
	}

	/** Waypoints for the current server and dimension. */
	public static List<Waypoint> here() {
		List<Waypoint> result = new ArrayList<>();
		String server = server(), dimension = dimension();
		for (Waypoint w : all()) if (w.server.equals(server) && w.dimension.equals(dimension)) result.add(w);
		return result;
	}

	/** Adds (or moves, if the name exists here) a waypoint. Returns it. */
	public static Waypoint add(String name, BlockPos pos) {
		Waypoint existing = find(name);
		Waypoint w = existing != null ? existing : new Waypoint();
		w.name = name;
		w.x = pos.getX();
		w.y = pos.getY();
		w.z = pos.getZ();
		w.dimension = dimension();
		w.server = server();
		if (existing == null) {
			w.color = PALETTE[all().size() % PALETTE.length];
			all().add(w);
		}
		save();
		return w;
	}

	public static Waypoint find(String name) {
		for (Waypoint w : here()) if (w.name.equalsIgnoreCase(name)) return w;
		return null;
	}

	public static boolean remove(String name) {
		Waypoint w = find(name);
		if (w == null) return false;
		all().remove(w);
		save();
		return true;
	}

	/** Removes every waypoint for this server and dimension. Returns how many. */
	public static int clearHere() {
		List<Waypoint> here = here();
		all().removeAll(here);
		save();
		return here.size();
	}

	/** Next free "Waypoint N" name. */
	public static String nextName(String base) {
		for (int i = 1; ; i++) {
			String name = base + " " + i;
			if (find(name) == null) return name;
		}
	}
}
