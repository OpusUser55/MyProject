package dev.ooga.client.command;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.waypoint.Waypoint;
import dev.ooga.client.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Client-side chat commands, typed with a {@code .} prefix (e.g. {@code .t freecam}). They're
 * handled locally and never reach the server. Start a message with {@code ..} to send a
 * literal dot.
 */
public final class CommandManager {
	public static final String PREFIX = ".";

	@FunctionalInterface
	private interface Handler {
		void run(String[] args);
	}

	private record Command(String usage, String description, Handler handler) {
	}

	private static final Map<String, Command> COMMANDS = new LinkedHashMap<>();
	private static final Map<String, String> ALIASES = Map.of("t", "toggle", "b", "bind", "wp", "waypoint", "h", "help");

	private CommandManager() {
	}

	static {
		COMMANDS.put("help", new Command("help", "List commands.", args -> {
			for (Map.Entry<String, Command> e : COMMANDS.entrySet()) {
				ChatUtil.info(PREFIX + e.getValue().usage() + " — " + e.getValue().description());
			}
		}));
		COMMANDS.put("toggle", new Command("toggle <module>", "Turn a module on or off (alias .t).", args -> {
			Module module = module(args, 0);
			if (module == null) return;
			if (module.isSettingsOnly()) {
				ChatUtil.info(module.getName() + " has no on/off state.");
				return;
			}
			module.toggle();
			ChatUtil.info(module.getName() + (module.isEnabled() ? " enabled" : " disabled"));
		}));
		COMMANDS.put("bind", new Command("bind <module> <key|none>", "Set a module's keybind (alias .b).", args -> {
			if (args.length < 2) {
				ChatUtil.info("Usage: " + PREFIX + "bind <module> <key|none>");
				return;
			}
			Module module = moduleByName(String.join(" ", Arrays.copyOf(args, args.length - 1)));
			if (module == null) return;
			String keyName = args[args.length - 1].toLowerCase(Locale.ROOT);
			if (keyName.equals("none") || keyName.equals("unbind")) {
				module.setKey(-1);
				ChatUtil.info(module.getName() + " unbound");
				return;
			}
			try {
				int key = InputConstants.getKey("key.keyboard." + keyName).getValue();
				module.setKey(key);
				ChatUtil.info(module.getName() + " bound to " + keyName.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				ChatUtil.info("Unknown key '" + keyName + "'. Try names like r, f6, left.alt, right.shift.");
			}
		}));
		COMMANDS.put("waypoint", new Command("wp <add|remove|list|clear> [name] [x y z]", "Manage waypoints for this server.", CommandManager::waypoint));
		COMMANDS.put("coords", new Command("coords", "Copy your coordinates to the clipboard.", args -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null) return;
			BlockPos p = mc.player.blockPosition();
			String text = p.getX() + " " + p.getY() + " " + p.getZ();
			mc.keyboardHandler.setClipboard(text);
			ChatUtil.info("Copied " + text);
		}));
		COMMANDS.put("modules", new Command("modules", "List every module and whether it's on.", args -> {
			StringJoiner on = new StringJoiner(", ");
			StringJoiner off = new StringJoiner(", ");
			for (Module m : ModuleManager.get().getModules()) {
				if (m.isSettingsOnly()) continue;
				(m.isEnabled() ? on : off).add(m.getName());
			}
			ChatUtil.info("On: " + (on.length() == 0 ? "none" : on));
			ChatUtil.info("Off: " + (off.length() == 0 ? "none" : off));
		}));
	}

	/**
	 * @param message the raw chat line
	 * @return true if it was a client command (handled here, must not be sent)
	 */
	public static boolean handle(String message) {
		if (!message.startsWith(PREFIX) || message.startsWith(PREFIX + PREFIX)) return false;
		String[] parts = message.substring(PREFIX.length()).trim().split("\\s+");
		if (parts.length == 0 || parts[0].isEmpty()) return false;
		String name = parts[0].toLowerCase(Locale.ROOT);
		name = ALIASES.getOrDefault(name, name);
		Command command = COMMANDS.get(name);
		// Messages like "..." or ".5 blocks" aren't commands; let them through as chat.
		if (command == null) {
			if (!Character.isLetter(parts[0].charAt(0))) return false;
			ChatUtil.info("Unknown command. Type " + PREFIX + "help");
			return true;
		}
		try {
			command.handler().run(Arrays.copyOfRange(parts, 1, parts.length));
		} catch (RuntimeException e) {
			ChatUtil.info("That didn't work: " + e.getMessage());
		}
		return true;
	}

	/** A ".." message: strip one dot and send the rest as normal chat. */
	public static String unescape(String message) {
		return message.startsWith(PREFIX + PREFIX) ? message.substring(PREFIX.length()) : message;
	}

	private static Module module(String[] args, int from) {
		if (args.length <= from) {
			ChatUtil.info("Which module?");
			return null;
		}
		return moduleByName(String.join(" ", Arrays.copyOfRange(args, from, args.length)));
	}

	/** Matches ignoring case, spaces and dashes, so "freecam", "Free-Cam" and "free cam" all work. */
	private static Module moduleByName(String name) {
		String wanted = normalize(name);
		for (Module m : ModuleManager.get().getModules()) {
			if (normalize(m.getName()).equals(wanted)) return m;
		}
		ChatUtil.info("No module called '" + name + "'. Type " + PREFIX + "modules");
		return null;
	}

	private static String normalize(String s) {
		return s.toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "").replace("_", "");
	}

	private static void waypoint(String[] args) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
		switch (sub) {
			case "add", "set" -> {
				if (args.length < 2) {
					ChatUtil.info("Usage: " + PREFIX + "wp add <name> [x y z]");
					return;
				}
				BlockPos pos = mc.player.blockPosition();
				String name = args[1];
				if (args.length >= 5) {
					pos = new BlockPos(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
				}
				WaypointStore.add(new Waypoint(name, pos, WaypointStore.currentDimension()));
				ChatUtil.info("Waypoint " + name + " set at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
			}
			case "remove", "del", "delete" -> {
				if (args.length < 2) {
					ChatUtil.info("Usage: " + PREFIX + "wp remove <name>");
					return;
				}
				ChatUtil.info(WaypointStore.remove(args[1]) ? "Removed " + args[1] : "No waypoint called " + args[1]);
			}
			case "clear" -> ChatUtil.info("Removed " + WaypointStore.clear() + " waypoints on this server.");
			case "list" -> {
				List<Waypoint> all = WaypointStore.current();
				if (all.isEmpty()) {
					ChatUtil.info("No waypoints here yet. " + PREFIX + "wp add <name>");
					return;
				}
				for (Waypoint w : all) {
					BlockPos p = w.pos();
					ChatUtil.info(w.name() + ": " + p.getX() + ", " + p.getY() + ", " + p.getZ() + " (" + w.dimension() + ")");
				}
			}
			default -> ChatUtil.info("Usage: " + PREFIX + "wp <add|remove|list|clear>");
		}
	}
}
